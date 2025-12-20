package compiler.frontend.visitor;

import compiler.frontend.ast.*;
import compiler.infra.Diagnostics;
import compiler.middle.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TypeCheckingVisitor implements ASTVisitor<Void> {
    private final SymbolTable table;
    private final Diagnostics diag;

    public TypeCheckingVisitor(SymbolTable table, Diagnostics diag) {
        this.table = table;
        this.diag = diag;
    }

    private void setType(ExpressionNode node, String type) {
        node.type = type;
    }

    private String getType(ExpressionNode node) {
        return node.type;
    }

    private boolean isCompatible(String expected, String actual) {
        if (expected.equals(actual)) return true;
        if ("String".equals(expected) && "string".equals(actual)) return true; // loose matching
        if ("String".equals(actual) && "string".equals(expected)) return true;
        // Basic compatibility: int can be assigned to int, etc.
        // Special case: null can be assigned to any object (non-primitive).
        if ("null".equals(actual) && !isPrimitive(expected)) return true;
        return false;
    }

    private boolean isPrimitive(String type) {
        return "int".equals(type) || "boolean".equals(type) || "void".equals(type);
    }

    private boolean isNumeric(String type) {
        return "int".equals(type);
    }

    private boolean isBoolean(String type) {
        return "boolean".equals(type);
    }

    // --- Visitor Methods ---

    @Override
    public Void visitBlockNode(BlockNode node) {
        table.enterScope();

        // Pre-pass: Register all classes and functions in the current scope
        for (ASTNode stmt : node.getStatements()) {
            if (stmt instanceof ClassDeclNode) {
                ClassDeclNode cdn = (ClassDeclNode) stmt;
                Symbol sym = new Symbol(cdn.className, Kind.TYPE, cdn);
                table.declare(sym);
            } else if (stmt instanceof FunctionDeclNode) {
                FunctionDeclNode fdn = (FunctionDeclNode) stmt;
                Symbol sym = new Symbol(fdn.name, Kind.FUNCTION, fdn);
                table.declare(sym);
            }
        }

        for (ASTNode statement : node.getStatements()) {
            statement.accept(this);
        }

        table.exitScope();
        return null;
    }

    @Override
    public Void visitClassDeclNode(ClassDeclNode node) {
        String prevClass = currentClassName;
        currentClassName = node.className;

        table.enterScope();

        // 1. Declare fields
        for (VarDeclNode field : node.fields) {
            Symbol sym = new Symbol(field.name, Kind.VARIABLE, field);
            if (!table.declare(sym)) {
                diag.addError("Duplicate field: " + field.name);
            }
        }

        // 2. Declare methods
        for (FunctionDeclNode method : node.methods) {
            Symbol sym = new Symbol(method.name, Kind.FUNCTION, method);
            if (!table.declare(sym)) {
                diag.addError("Duplicate method: " + method.name);
            }
        }

        // 3. Visit fields (initializers)
        for (VarDeclNode field : node.fields) {
            field.accept(this);
        }

        // 4. Visit methods (bodies)
        for (FunctionDeclNode method : node.methods) {
            method.accept(this);
        }

        table.exitScope();
        currentClassName = prevClass;
        return null;
    }

    @Override
    public Void visitFunctionDeclNode(FunctionDeclNode node) {
        String prevRet = currentMethodReturnType;
        currentMethodReturnType = node.returnType;

        Optional<Symbol> existing = table.lookupLocal(node.name);
        if (existing.isPresent() && existing.get().declaration() == node) {
            // Already declared
        } else {
             Symbol funcSym = new Symbol(node.name, Kind.FUNCTION, node);
             table.declare(funcSym);
        }

        table.enterScope();

        // Declare parameters
        for (VarDeclNode param : node.getParams()) {
            Symbol sym = new Symbol(param.name, Kind.PARAMETER, param);
            if (!table.declare(sym)) {
                diag.addError("Duplicate parameter: " + param.name);
            }
        }

        // Visit body
        node.getBody().accept(this);

        // Check return type enforcement
        if (!"void".equals(node.returnType)) {
            if (!checkReturn(node.getBody())) {
                diag.addError("Missing return statement in function: " + node.name);
            }
        }

        table.exitScope();
        currentMethodReturnType = prevRet;
        return null;
    }

    // Helper to check if a block returns
    private boolean checkReturn(ASTNode node) {
        if (node instanceof ReturnNode) return true;
        if (node instanceof BlockNode) {
            BlockNode bn = (BlockNode) node;
            for (ASTNode s : bn.getStatements()) {
                if (checkReturn(s)) return true;
            }
            return false;
        }
        if (node instanceof IfNode) {
            IfNode ifn = (IfNode) node;
            boolean thenReturns = checkReturn(ifn.getThenBlock());
            boolean elseReturns = ifn.getElseBlock() != null && checkReturn(ifn.getElseBlock());
            return thenReturns && elseReturns;
        }
        return false;
    }

    @Override
    public Void visitVarDeclNode(VarDeclNode node) {
        Optional<Symbol> existing = table.lookupLocal(node.name);
        if (existing.isPresent() && existing.get().declaration() == node) {
            // Already declared
        } else {
            Symbol sym = new Symbol(node.name, Kind.VARIABLE, node);
            if (!table.declare(sym)) {
                diag.addError("Duplicate variable: " + node.name);
            }
        }

        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
            String initType = getType(node.getInitializer());
            if (initType != null && !isCompatible(node.type, initType)) {
                diag.addError("Type mismatch in initialization of " + node.name + ": expected " + node.type + ", got " + initType);
            }
        }
        return null;
    }

    @Override
    public Void visitAssignmentNode(AssignmentNode node) {
        node.getTarget().accept(this);
        node.getExpression().accept(this);

        String targetType = getType(node.getTarget());
        String exprType = getType(node.getExpression());

        if (targetType != null && exprType != null) {
            if (!isCompatible(targetType, exprType)) {
                diag.addError("Type mismatch in assignment: expected " + targetType + ", got " + exprType);
            }
        }
        return null;
    }

    @Override
    public Void visitBinaryExprNode(BinaryExprNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);

        String leftType = getType(node.getLeft());
        String rightType = getType(node.getRight());

        if (leftType == null || rightType == null) return null;

        String op = node.getOp();
        checkBinaryOp(node, op, leftType, rightType);
        return null;
    }

    @Override
    public Void visitBinaryOpNode(BinaryOpNode node) {
        node.left.accept(this);
        node.right.accept(this);

        String leftType = getType(node.left);
        String rightType = getType(node.right);

        if (leftType == null || rightType == null) return null;

        checkBinaryOp(node, node.op, leftType, rightType);
        return null;
    }

    private void checkBinaryOp(ExpressionNode node, String op, String leftType, String rightType) {
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
             if (isNumeric(leftType) && isNumeric(rightType)) {
                    setType(node, "int");
                } else if (op.equals("+") && (leftType.equals("String") || rightType.equals("String"))) {
                    setType(node, "String"); // String concatenation
                } else {
                    diag.addError("Operator " + op + " requires numeric operands.");
                    setType(node, "int"); // Fallback
                }
        } else if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
             if (isNumeric(leftType) && isNumeric(rightType)) {
                    setType(node, "boolean");
                } else {
                    diag.addError("Operator " + op + " requires numeric operands.");
                    setType(node, "boolean");
                }
        } else if (op.equals("&&") || op.equals("||")) {
             if (isBoolean(leftType) && isBoolean(rightType)) {
                    setType(node, "boolean");
                } else {
                    diag.addError("Operator " + op + " requires boolean operands.");
                    setType(node, "boolean");
                }
        } else if (op.equals("==") || op.equals("!=")) {
             if (isCompatible(leftType, rightType) || isCompatible(rightType, leftType)) {
                    setType(node, "boolean");
                } else {
                    diag.addError("Operator " + op + " requires compatible operands.");
                    setType(node, "boolean");
                }
        } else {
            setType(node, "unknown");
        }
    }

    @Override
    public Void visitUnaryOpNode(UnaryOpNode node) {
        node.expr.accept(this);
        String type = getType(node.expr);
        if (type == null) return null;

        if (node.op.equals("!")) {
            if (isBoolean(type)) setType(node, "boolean");
            else diag.addError("Operator ! requires boolean operand.");
        } else if (node.op.equals("-") || node.op.equals("+") || node.op.contains("++") || node.op.contains("--")) {
            if (isNumeric(type)) setType(node, "int");
            else diag.addError("Operator " + node.op + " requires numeric operand.");
        }
        return null;
    }

    @Override
    public Void visitIdentifierNode(IdentifierNode node) {
        if ("this".equals(node.name)) {
            if (currentClassName != null) {
                setType(node, currentClassName);
            } else {
                diag.addError("'this' used outside of class context.");
                setType(node, "unknown");
            }
            return null;
        }

        Optional<Symbol> sym = table.lookup(node.name);
        if (sym.isPresent()) {
            Symbol s = sym.get();
            if (s.declaration() instanceof VarDeclNode) {
                setType(node, ((VarDeclNode) s.declaration()).type);
            } else if (s.declaration() instanceof FunctionDeclNode) {
                 setType(node, "unknown");
            } else {
                setType(node, "unknown");
            }
        } else {
            diag.addError("Undefined identifier: " + node.name);
            setType(node, "unknown");
        }
        return null;
    }

    @Override
    public Void visitLiteralNode(LiteralNode node) {
        if (node.value.matches("-?\\d+")) {
            setType(node, "int");
        } else if (node.value.equals("true") || node.value.equals("false")) {
            setType(node, "boolean");
        } else if (node.value.equals("null")) {
            setType(node, "null");
        } else {
            setType(node, "String");
        }
        return null;
    }

    @Override
    public Void visitMethodCallNode(MethodCallNode node) {
        String className = null;

        // 1. Resolve Object
        if (node.object != null) {
            node.object.accept(this);
            String objType = getType(node.object);
            if (objType == null || isPrimitive(objType)) {
                diag.addError("Cannot call method on primitive or null type: " + objType);
                return null;
            }
            className = objType;
        } else {
            // Implicit 'this' or local function
            if (currentClassName != null) {
                Optional<Symbol> classSym = table.lookup(currentClassName);
                if (classSym.isPresent()) {
                    ClassDeclNode classDecl = (ClassDeclNode) classSym.get().declaration();
                    FunctionDeclNode method = findMethod(classDecl, node.methodName);
                    if (method != null) {
                         checkMethodCall(node, method);
                         return null;
                    }
                }
            }

            // Try global function
            Optional<Symbol> sym = table.lookup(node.methodName);
            if (sym.isPresent() && sym.get().kind() == Kind.FUNCTION) {
                checkMethodCall(node, (FunctionDeclNode) sym.get().declaration());
                return null;
            }

            diag.addError("Method not found: " + node.methodName);
            return null;
        }

        // 2. Lookup Class
        if (className != null) {
            Optional<Symbol> classSym = table.lookup(className);
            if (!classSym.isPresent() || classSym.get().kind() != Kind.TYPE) {
                diag.addError("Undefined class: " + className);
                return null;
            }
            ClassDeclNode classDecl = (ClassDeclNode) classSym.get().declaration();

            // 3. Lookup Method in Class
            FunctionDeclNode method = findMethod(classDecl, node.methodName);
            if (method == null) {
                diag.addError("Method " + node.methodName + " not found in class " + className);
                return null;
            }

            checkMethodCall(node, method);
        }
        return null;
    }

    private void checkMethodCall(MethodCallNode node, FunctionDeclNode method) {
        if (node.args.size() != method.params.size()) {
            diag.addError("Method " + method.name + " expects " + method.params.size() + " arguments, got " + node.args.size());
            return;
        }

        for (int i = 0; i < node.args.size(); i++) {
            ExpressionNode arg = node.args.get(i);
            arg.accept(this);
            String argType = getType(arg);
            String paramType = method.params.get(i).type;

            if (!isCompatible(paramType, argType)) {
                diag.addError("Argument " + (i+1) + " type mismatch: expected " + paramType + ", got " + argType);
            }
        }

        setType(node, method.returnType);
    }

    private FunctionDeclNode findMethod(ClassDeclNode classDecl, String methodName) {
        for (FunctionDeclNode m : classDecl.methods) {
            if (m.name.equals(methodName)) return m;
        }
        return null;
    }

    @Override
    public Void visitNewExprNode(NewExprNode node) {
        Optional<Symbol> sym = table.lookup(node.className);
        if (!sym.isPresent() || sym.get().kind() != Kind.TYPE) {
            diag.addError("Undefined class: " + node.className);
            setType(node, "unknown");
            return null;
        }

        ClassDeclNode classDecl = (ClassDeclNode) sym.get().declaration();
        FunctionDeclNode constructor = findMethod(classDecl, node.className);

        if (constructor != null) {
             if (node.args.size() != constructor.params.size()) {
                diag.addError("Constructor " + node.className + " expects " + constructor.params.size() + " arguments, got " + node.args.size());
            } else {
                 for (int i = 0; i < node.args.size(); i++) {
                    ExpressionNode arg = node.args.get(i);
                    arg.accept(this);
                    String argType = getType(arg);
                    String paramType = constructor.params.get(i).type;
                    if (!isCompatible(paramType, argType)) {
                         diag.addError("Constructor Argument " + (i+1) + " type mismatch.");
                    }
                }
            }
        } else {
            if (node.args.size() > 0) {
                 diag.addError("No matching constructor for " + node.className);
            }
        }

        setType(node, node.className);
        return null;
    }

    @Override
    public Void visitMemberAccessNode(MemberAccessNode node) {
        node.object.accept(this);
        String objType = getType(node.object);

        if (objType == null || isPrimitive(objType)) {
            diag.addError("Cannot access member of non-object type: " + objType);
            return null;
        }

        Optional<Symbol> classSym = table.lookup(objType);
        if (!classSym.isPresent()) {
             diag.addError("Class not found: " + objType);
             return null;
        }

        ClassDeclNode classDecl = (ClassDeclNode) classSym.get().declaration();
        for (VarDeclNode field : classDecl.fields) {
            if (field.name.equals(node.memberName)) {
                setType(node, field.type);
                return null;
            }
        }

        diag.addError("Field " + node.memberName + " not found in class " + objType);
        setType(node, "unknown");
        return null;
    }

    @Override
    public Void visitReturnNode(ReturnNode node) {
        if (currentMethodReturnType != null) {
            if (node.getExpr() != null) {
                node.getExpr().accept(this);
                String actual = getType(node.getExpr());
                if (!isCompatible(currentMethodReturnType, actual)) {
                    diag.addError("Return type mismatch: expected " + currentMethodReturnType + ", got " + actual);
                }
            } else {
                if (!"void".equals(currentMethodReturnType)) {
                    diag.addError("Missing return value for non-void function.");
                }
            }
        }
        return null;
    }

    private String currentClassName = null;
    private String currentMethodReturnType = null;

    @Override public Void visitIfNode(IfNode node) {
        node.getCond().accept(this);
        if (!isBoolean(getType(node.getCond()))) diag.addError("If condition must be boolean");
        node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) node.getElseBlock().accept(this);
        return null;
    }

    @Override public Void visitWhileNode(WhileNode node) {
        node.getCond().accept(this);
        if (!isBoolean(getType(node.getCond()))) diag.addError("While condition must be boolean");
        node.getBody().accept(this);
        return null;
    }

    @Override public Void visitForNode(ForNode node) {
        table.enterScope();
        if (node.getInit() != null) node.getInit().accept(this);
        if (node.getCond() != null) {
            node.getCond().accept(this);
            if (!isBoolean(getType(node.getCond()))) diag.addError("For condition must be boolean");
        }
        if (node.getUpdate() != null) node.getUpdate().accept(this);
        node.getBody().accept(this);
        table.exitScope();
        return null;
    }

    @Override public Void visitEmptyNode(EmptyNode node) { return null; }

}
