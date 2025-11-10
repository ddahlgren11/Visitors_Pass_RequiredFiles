
import compiler.infra.*;
import compiler.middle.*;

/**
 * Type checking pass that performs basic semantic validation.
 */
public class TypeCheckingPass implements CompilerPass {
    @Override
    public String name() { return "TypeCheckingPass"; }

    @Override
    public void execute(CompilerContext context) throws Exception {
        Diagnostics diag = context.getDiagnostics();
        SymbolTable table = context.getSymbolTable();
        if (table == null || context.getAst() == null) return;

        Object ast = context.getAst();
        if (ast instanceof ASTNodeBase) {
            visit((ASTNodeBase) ast, table, diag);
        }
    }

    private void visit(ASTNodeBase node, SymbolTable table, Diagnostics diag) {
        if (node == null) return;

        checkNode(node, table, diag);

        // Visit children
        for (java.lang.reflect.Method m : node.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            try {
                if (ASTNodeBase.class.isAssignableFrom(m.getReturnType())) {
                    Object child = m.invoke(node);
                    if (child instanceof ASTNodeBase) visit((ASTNodeBase) child, table, diag);
                } else if (java.util.List.class.isAssignableFrom(m.getReturnType())) {
                    Object result = m.invoke(node);
                    if (result instanceof java.util.List<?>) {
                        for (Object item : (java.util.List<?>) result) {
                            if (item instanceof ASTNodeBase) visit((ASTNodeBase) item, table, diag);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void checkNode(ASTNodeBase node, SymbolTable table, Diagnostics diag) {
        String cname = node.getClass().getSimpleName();
        
        if ("IdentifierNode".equals(cname)) {
            String name = getNodeProperty(node, "getName");
            if (name != null && !table.lookup(name).isPresent())
                diag.addError("Undeclared variable used: " + name);
        } else if ("AssignmentNode".equals(cname)) {
            String targetName = getNodeProperty(node, "getVarName");
            if (targetName != null) {
                if (!table.lookup(targetName).isPresent()) {
                    diag.addError("Undeclared variable used: " + targetName);
                } else {
                    java.util.Optional<Symbol> sym = table.lookup(targetName);
                    if (sym.isPresent()) {
                        Object decl = sym.get().declaration();
                        if (decl instanceof ASTNodeBase) {
                            String type = getNodeProperty((ASTNodeBase)decl, "getType");
                            if ("boolean".equals(type)) {
                                Object value = getNodePropertyObject(node, "getValue");
                                if (value != null) {
                                    String simpleName = value.getClass().getSimpleName();
                                    if (simpleName.contains("BinaryOp") || simpleName.contains("BinaryExpr")) {
                                        diag.addError("Type mismatch: assigning numeric expression to boolean");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if ("VarDeclNode".equals(cname)) {
            String type = getNodeProperty(node, "getType");
            if ("boolean".equals(type)) {
                Object init = getNodePropertyObject(node, "getInit");
                if (init != null) {
                    String simpleName = init.getClass().getSimpleName();
                    if (simpleName.contains("BinaryOp") || simpleName.contains("BinaryExpr"))
                        diag.addError("Type mismatch: assigning numeric expression to boolean");
                }
            }
        }
    }

    private String getNodeProperty(ASTNodeBase node, String methodName) {
        try {
            java.lang.reflect.Method m = node.getClass().getMethod(methodName);
            Object result = m.invoke(node);
            return result instanceof String ? (String)result : null;
        } catch (Exception ignored) { return null; }
    }

    private Object getNodePropertyObject(ASTNodeBase node, String methodName) {
        try {
            java.lang.reflect.Method m = node.getClass().getMethod(methodName);
            return m.invoke(node);
        } catch (Exception ignored) { return null; }
    }
}
