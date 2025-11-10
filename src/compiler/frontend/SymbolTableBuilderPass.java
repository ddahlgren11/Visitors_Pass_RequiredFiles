package compiler.frontend;

import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.infra.Diagnostics;
import compiler.infra.SourceLocation;
import compiler.middle.SymbolTable;
import compiler.middle.SymbolTableImpl;
import compiler.middle.Symbol;
import compiler.middle.Kind;
import compiler.middle.ScopeInfo;

/**
 * Symbol table builder pass for the legacy frontend AST.
 * This implementation is defensive and reflection-based to avoid compile-time
 * dependencies on many legacy node classes that may or may not be present.
 * It populates a SymbolTable and stores it in the CompilerContext for later passes.
 */
public class SymbolTableBuilderPass implements CompilerPass {
    @Override
    public String name() { return "SymbolTableBuilderPass"; }

    @Override
    public void execute(CompilerContext context) throws Exception {
        Diagnostics diag = context.getDiagnostics();
        diag.log("=== Starting " + name() + " ===");

        // Create and attach a fresh symbol table to the context.
        SymbolTable table = new SymbolTableImpl();
        table.enterScope(); // global scope
        context.setSymbolTable(table);
        diag.log("Created symbol table with global scope.");

        // Get AST produced by the front-end; if missing, nothing to do.
        Object ast = context.getAst();
        if (ast == null) {
            diag.log("No AST found — symbol table will be empty.");
            return;
        }

        // If AST uses the legacy frontend nodes (they extend ASTNodeBase), walk reflectively.
        if (ast instanceof ASTNodeBase) {
            diag.log("Building symbol table from legacy frontend AST...");
            walkReflective((ASTNodeBase) ast, table, diag);
            diag.log("Symbol table complete: " + table.getScopeInfo().size() + " scope(s).");
            return;
        }

        // Newer AST shape (visitor-style) lives under package compiler.frontend.ast
        Package p = ast.getClass().getPackage();
        if (p != null && p.getName().startsWith("compiler.frontend.ast")) {
            diag.log("Building symbol table from visitor-style frontend.ast AST...");
            walkReflectiveAst(ast, table, diag);
            diag.log("Symbol table complete: " + table.getScopeInfo().size() + " scope(s).");
            return;
        }

        diag.log("Unknown AST type — skipping symbol table construction.");
    }

    /**
     * Reflection-based traversal for legacy frontend AST nodes.
     */
    private void walkReflective(ASTNodeBase node, SymbolTable table, Diagnostics diag) {
        if (node == null) return;

        Class<?> cls = node.getClass();
        String cname = cls.getSimpleName();

        try {
            if ("VarDeclNode".equals(cname)) {
                // try to read name via getName()
                String name = invokeStringGetter(node, "getName");
                if (name != null) {
                    Symbol sym = new Symbol(name, Kind.VARIABLE, node);
                    boolean ok = table.declare(sym);
                    if (!ok) diag.addError("Duplicate declaration: " + name);
                }
                // attempt to walk initializer if present (getInit or getValue)
                Object init = tryGetFieldOrMethod(node, "getInit", "init", "getValue", "value");
                if (init instanceof ASTNodeBase) walkReflective((ASTNodeBase) init, table, diag);
                return;
            }

            if ("AssignmentNode".equals(cname)) {
                Object left = tryGetFieldOrMethod(node, "getVar", "var", "getTarget", "target");
                Object right = tryGetFieldOrMethod(node, "getValue", "value", "getExpr", "expr");

                if (left != null) {
                    if (left.getClass().getSimpleName().equals("VarDeclNode")) {
                        // declare variable
                        String name = invokeStringGetter(left, "getName");
                        if (name != null) {
                            Symbol sym = new Symbol(name, Kind.VARIABLE, (ASTNodeBase) left);
                            if (!table.declare(sym)) diag.addError("Duplicate declaration: " + name);
                        }
                    } else {
                        // left may be identifier
                        String id = extractIdentifierName(left);
                        if (id != null && !table.lookup(id).isPresent()) {
                            diag.addError("Use of undeclared variable: " + id);
                        }
                    }
                }
                if (right instanceof ASTNodeBase) walkReflective((ASTNodeBase) right, table, diag);
                return;
            }

            if ("BinaryOpNode".equals(cname) || "BinaryExprNode".equals(cname)) {
                Object l = tryGetFieldOrMethod(node, "getLeft", "left");
                Object r = tryGetFieldOrMethod(node, "getRight", "right");
                if (l instanceof ASTNodeBase) walkReflective((ASTNodeBase) l, table, diag);
                if (r instanceof ASTNodeBase) walkReflective((ASTNodeBase) r, table, diag);
                return;
            }

            if ("IdentifierNode".equals(cname)) {
                String id = extractIdentifierName(node);
                if (id != null && !table.lookup(id).isPresent()) {
                    // Leave undeclared uses to be reported more strictly by type checking
                }
                return;
            }

            if ("LiteralNode".equals(cname)) return;

            // Generic handling for BlockNode / FunctionDeclNode / IfNode etc.:
            // Try to find child ASTNodeBase-returning methods and descend.
            for (java.lang.reflect.Method m : cls.getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!ASTNodeBase.class.isAssignableFrom(m.getReturnType())) continue;
                try {
                    Object child = m.invoke(node);
                    if (child instanceof ASTNodeBase) walkReflective((ASTNodeBase) child, table, diag);
                } catch (Exception ignored) {}
            }

            // Also try to find collections of ASTNodeBase (e.g., getStatements)
            try {
                java.lang.reflect.Method ms[] = cls.getMethods();
                for (java.lang.reflect.Method m : ms) {
                    if (m.getParameterCount() != 0) continue;
                    if (!java.util.List.class.isAssignableFrom(m.getReturnType())) continue;
                    try {
                        Object maybeList = m.invoke(node);
                        if (maybeList instanceof java.util.List) {
                            for (Object child : (java.util.List<?>) maybeList) {
                                if (child instanceof ASTNodeBase) walkReflective((ASTNodeBase) child, table, diag);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            // Reflection errors are non-fatal for the pedagogical pass — record and continue.
            diag.addError("SymbolTableBuilder reflection error: " + e.getMessage());
        }
    }

    private String invokeStringGetter(Object obj, String methodName) {
        try {
            java.lang.reflect.Method m = obj.getClass().getMethod(methodName);
            Object res = m.invoke(obj);
            if (res instanceof String) return (String) res;
        } catch (Exception ignored) {}
        return null;
    }

    private Object tryGetFieldOrMethod(Object obj, String... names) {
        Class<?> cls = obj.getClass();
        for (String n : names) {
            try {
                try {
                    java.lang.reflect.Method m = cls.getMethod(n);
                    return m.invoke(obj);
                } catch (NoSuchMethodException ignored) {
                    // try field
                    try {
                        java.lang.reflect.Field f = cls.getDeclaredField(n);
                        f.setAccessible(true);
                        return f.get(obj);
                    } catch (NoSuchFieldException | IllegalAccessException ignored2) {
                        // continue
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractIdentifierName(Object idNode) {
        String name = invokeStringGetter(idNode, "getName");
        if (name != null) return name;
        // try common field names
        Object v = tryGetFieldOrMethod(idNode, "name", "varName", "identifier");
        if (v instanceof String) return (String) v;
        return null;
    }

    /**
     * Reflection-based traversal for the newer frontend.ast nodes (visitor-style).
     * Uses class simple names (VarDeclNode, AssignmentNode, IdentifierNode, LiteralNode, etc.).
     */
    private void walkReflectiveAst(Object node, SymbolTable table, Diagnostics diag) {
        if (node == null) return;
        Class<?> cls = node.getClass();
        String cname = cls.getSimpleName();
        try {
            if ("VarDeclNode".equals(cname)) {
                String name = invokeStringGetter(node, "getName");
                if (name != null) {
                    Symbol sym = new Symbol(name, Kind.VARIABLE, node);
                    boolean ok = table.declare(sym);
                    if (!ok) diag.addError("Duplicate declaration: " + name);
                }
                Object init = tryGetFieldOrMethod(node, "getInit", "getValue", "getExpr");
                if (init != null) walkReflectiveAst(init, table, diag);
                return;
            }

            if ("AssignmentNode".equals(cname)) {
                Object left = tryGetFieldOrMethod(node, "getVar", "getTarget", "getLeft");
                Object right = tryGetFieldOrMethod(node, "getValue", "getExpr", "getRight");

                if (left != null) {
                    String id = invokeStringGetter(left, "getName");
                    if (id != null && !table.lookup(id).isPresent()) {
                        diag.addError("Use of undeclared variable: " + id);
                    }
                }
                if (right != null) walkReflectiveAst(right, table, diag);
                return;
            }

            if ("IdentifierNode".equals(cname)) {
                String id = invokeStringGetter(node, "getName");
                if (id != null && !table.lookup(id).isPresent()) {
                    // leave to type checking for stricter reporting but note it here
                }
                return;
            }

            if ("LiteralNode".equals(cname)) return;

            // descend into ASTNode children and lists
            for (java.lang.reflect.Method m : cls.getMethods()) {
                if (m.getParameterCount() != 0) continue;
                try {
                    Object child = m.invoke(node);
                    if (child == null) continue;
                    if (child.getClass().getPackage() != null && child.getClass().getPackage().getName().startsWith("compiler.frontend.ast")) {
                        walkReflectiveAst(child, table, diag);
                    } else if (child instanceof java.util.List) {
                        for (Object c : (java.util.List<?>) child) {
                            if (c != null && c.getClass().getPackage() != null && c.getClass().getPackage().getName().startsWith("compiler.frontend.ast")) {
                                walkReflectiveAst(c, table, diag);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            diag.addError("SymbolTableBuilder reflection (ast) error: " + e.getMessage());
        }
    }
}
