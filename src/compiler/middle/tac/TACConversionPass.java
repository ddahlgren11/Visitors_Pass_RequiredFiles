package compiler.middle.tac;

import java.util.ArrayList;
import java.util.List;

import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.frontend.ast.*;

public class TACConversionPass implements CompilerPass, ASTVisitor {

    private final List<TACInstruction> instructions = new ArrayList<>();
    private int tempCount = 0;
    private int labelCount = 0;
    private String lastResult;
    private String currentClass;

    private String newTemp() {
        return "t" + (tempCount++);
    }

    private String newLabel() {
        return "L" + (labelCount++);
    }

    @Override
    public String name() { return "TACConversionPass"; }

    private String getDescriptor(String type) {
        if (type.equals("int")) return "I";
        if (type.equals("boolean")) return "Z";
        if (type.equals("void")) return "V";
        if (type.equals("String")) return "Ljava/lang/String;";
        return "L" + type + ";";
    }

    @Override
    public void execute(CompilerContext context) {
        ASTNode root = context.getAst();
        if (root != null) {
            root.accept(this);
            context.setTacInstructions(instructions);
        }
    }

    public List<TACInstruction> getInstructions() {
        return instructions;
    }

    private void emit(OpCode op, String target, String arg1, String arg2) {
        instructions.add(new TACInstruction(op, target, arg1, arg2));
    }

    @Override
    public void visitLiteralNode(LiteralNode node) {
        String temp = newTemp();
        emit(OpCode.LOAD_CONST, temp, node.value.toString(), null);
        lastResult = temp;
    }

    @Override
    public void visitIdentifierNode(IdentifierNode node) {
        // Identifier as an expression evaluates to its value.
        // We load it into a temp if it's a local var usage, or return the name if it's for assignment target?
        // Actually, for consistency, let's load it. But wait, visitAssignmentNode needs the name.
        // If we visitIdentifierNode, we assume it's an R-value.
        // For L-values, the parent (AssignmentNode) should handle it differently.

        // Check if it's 'this'
        if ("this".equals(node.name)) {
            lastResult = "this";
            return;
        }

        String temp = newTemp();
        emit(OpCode.LOAD_VAR, temp, node.name, null);
        lastResult = temp;
    }

    @Override
    public void visitBinaryOpNode(BinaryOpNode node) {
        node.left.accept(this);
        String left = lastResult;
        node.right.accept(this);
        String right = lastResult;

        String temp = newTemp();
        OpCode op = switch(node.op) {
            case "+" -> OpCode.ADD;
            case "-" -> OpCode.SUB;
            case "*" -> OpCode.MUL;
            case "/" -> OpCode.DIV;
            case "==" -> OpCode.EQ;
            case "!=" -> OpCode.NEQ;
            case "<" -> OpCode.LT;
            case "<=" -> OpCode.LE;
            case ">" -> OpCode.GT;
            case ">=" -> OpCode.GE;
            case "&&" -> OpCode.AND;
            case "||" -> OpCode.OR;
            default -> throw new RuntimeException("Unknown operator: " + node.op);
        };

        emit(op, temp, left, right);
        lastResult = temp;
    }

    @Override
    public void visitVarDeclNode(VarDeclNode node) {
        if (node.initializer != null) {
            node.initializer.accept(this);
            String value = lastResult;
            emit(OpCode.STORE_VAR, node.name, value, null);
        }
    }

    @Override
    public void visitAssignmentNode(AssignmentNode node) {
        node.expression.accept(this);
        String value = lastResult;

        // Handle target.
        // If target is Identifier, simple STORE_VAR.
        // If target is MemberAccess, PUT_FIELD.

        if (node.target instanceof IdentifierNode) {
            String targetName = ((IdentifierNode)node.target).name;
            emit(OpCode.STORE_VAR, targetName, value, null);
            lastResult = targetName;
        } else if (node.target instanceof MemberAccessNode) {
            MemberAccessNode man = (MemberAccessNode) node.target;
            man.object.accept(this);
            String obj = lastResult;
            String fieldName = man.memberName;
            if (man.object.type != null) {
                fieldName = man.object.type + ":" + fieldName;
            }
            emit(OpCode.PUT_FIELD, obj, fieldName, value);
            lastResult = value; // Assignment result is the value?
        } else {
             throw new RuntimeException("Unsupported assignment target");
        }
    }

    @Override
    public void visitFunctionDeclNode(FunctionDeclNode node) {
        StringBuilder sig = new StringBuilder("(");
        for (VarDeclNode param : node.getParams()) {
            sig.append(getDescriptor(param.type));
        }
        sig.append(")").append(getDescriptor(node.returnType));

        emit(OpCode.FUNC_ENTRY, node.name, String.valueOf(node.getParams().size()), sig.toString());

        // Emit params declarations
        for (VarDeclNode param : node.getParams()) {
             emit(OpCode.PARAM_DECL, param.name, getDescriptor(param.type), null);
        }

        emit(OpCode.LABEL, node.name, null, null);

        node.body.accept(this);

        // Implicit return if missing (for void)
        if (node.returnType.equals("void")) {
             emit(OpCode.RETURN, null, null, null);
        }
        emit(OpCode.FUNC_EXIT, node.name, null, null);

        lastResult = null;
    }

    @Override
    public void visitBinaryExprNode(BinaryExprNode node) {
         // Duplicate of BinaryOpNode logic if used
        node.getLeft().accept(this);
        String left = lastResult;
        node.getRight().accept(this);
        String right = lastResult;

        String temp = newTemp();
        OpCode op = switch(node.getOp()) {
            case "+" -> OpCode.ADD;
            case "-" -> OpCode.SUB;
            case "*" -> OpCode.MUL;
            case "/" -> OpCode.DIV;
             case "==" -> OpCode.EQ;
            case "!=" -> OpCode.NEQ;
            case "<" -> OpCode.LT;
            case "<=" -> OpCode.LE;
            case ">" -> OpCode.GT;
            case ">=" -> OpCode.GE;
            case "&&" -> OpCode.AND;
            case "||" -> OpCode.OR;
            default -> throw new RuntimeException("Unknown operator: " + node.getOp());
        };

        emit(op, temp, left, right);
        lastResult = temp;
    }

    @Override
    public void visitBlockNode(BlockNode node) {
        for (ASTNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
    }

    @Override
    public void visitReturnNode(ReturnNode node) {
        String val = null;
        if (node.getExpr() != null) {
            node.getExpr().accept(this);
            val = lastResult;
        }
        emit(OpCode.RETURN, val, null, null);
    }

    @Override
    public void visitIfNode(IfNode node) {
        String elseLabel = newLabel();
        String endLabel = newLabel();

        node.getCond().accept(this);
        String cond = lastResult;

        emit(OpCode.IFZ, cond, elseLabel, null); // Jump to else if false (zero)

        node.getThenBlock().accept(this);
        emit(OpCode.GOTO, endLabel, null, null);

        emit(OpCode.LABEL, elseLabel, null, null);
        if (node.getElseBlock() != null) {
            node.getElseBlock().accept(this);
        }

        emit(OpCode.LABEL, endLabel, null, null);
    }

    @Override
    public void visitWhileNode(WhileNode node) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        emit(OpCode.LABEL, startLabel, null, null);

        node.getCond().accept(this);
        String cond = lastResult;

        emit(OpCode.IFZ, cond, endLabel, null);

        node.getBody().accept(this);
        emit(OpCode.GOTO, startLabel, null, null);

        emit(OpCode.LABEL, endLabel, null, null);
    }

    @Override
    public void visitForNode(ForNode node) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        if (node.getInit() != null) {
            node.getInit().accept(this);
        }

        emit(OpCode.LABEL, startLabel, null, null);

        if (node.getCond() != null) {
            node.getCond().accept(this);
            String cond = lastResult;
            emit(OpCode.IFZ, cond, endLabel, null);
        }

        node.getBody().accept(this);

        if (node.getUpdate() != null) {
            node.getUpdate().accept(this);
        }

        emit(OpCode.GOTO, startLabel, null, null);
        emit(OpCode.LABEL, endLabel, null, null);
    }

    @Override
    public void visitUnaryOpNode(UnaryOpNode node) {
        node.expr.accept(this);
        String val = lastResult;
        String temp = newTemp();

        OpCode op = switch(node.op) {
            case "!" -> OpCode.NOT;
            case "-" -> OpCode.NEG;
            default -> throw new RuntimeException("Unary op not fully supported in TAC yet: " + node.op);
        };

        emit(op, temp, val, null);
        lastResult = temp;
    }

    @Override
    public void visitEmptyNode(EmptyNode emptyNode) {
        // No-op
    }

    @Override
    public void visitClassDeclNode(ClassDeclNode node) {
        String prevClass = currentClass;
        currentClass = node.className;

        for (VarDeclNode field : node.fields) {
            emit(OpCode.FIELD_DECL, null, node.className, field.name);
        }

        for (FunctionDeclNode method : node.methods) {
            String mangledName = node.className + "." + method.name;
            // Add params to size, include implicit 'this' (1 + params)
            int paramCount = 1 + method.getParams().size();

            StringBuilder sig = new StringBuilder("(");
            for (VarDeclNode param : method.getParams()) {
                sig.append(getDescriptor(param.type));
            }
            sig.append(")").append(getDescriptor(method.returnType));

            // If constructor, return type is V in bytecode
            if (method.name.equals(node.className)) {
                 int closeParen = sig.lastIndexOf(")");
                 sig.replace(closeParen + 1, sig.length(), "V");
            }

            emit(OpCode.FUNC_ENTRY, mangledName, String.valueOf(paramCount), sig.toString());

            // Param declarations
            // For instance methods, do we emit 'this'?
            // IdentifierNode visitor uses "this".
            // If we emit PARAM_DECL "this", we map it to 0.
            emit(OpCode.PARAM_DECL, "this", "L" + node.className + ";", null);

            for (VarDeclNode param : method.getParams()) {
                 emit(OpCode.PARAM_DECL, param.name, getDescriptor(param.type), null);
            }

            emit(OpCode.LABEL, mangledName, null, null);

            method.body.accept(this);

            if (method.returnType.equals("void") || method.name.equals(node.className)) {
                 emit(OpCode.RETURN, null, null, null);
            }
            emit(OpCode.FUNC_EXIT, mangledName, null, null);
        }

        currentClass = prevClass;
    }

    @Override
    public void visitNewExprNode(NewExprNode node) {
        // args...
        List<String> argTemps = new ArrayList<>();
        for (ASTNode arg : node.args) {
            arg.accept(this);
            argTemps.add(lastResult);
        }

        // Push params
        for (String arg : argTemps) {
            emit(OpCode.PARAM, arg, null, null);
        }

        String temp = newTemp();
        String signature = node.descriptor != null ? node.descriptor : "()V";
        emit(OpCode.NEW, temp, node.className, node.args.size() + ":" + signature);
        lastResult = temp;
    }

    @Override
    public void visitMethodCallNode(MethodCallNode node) {
        String obj = null;
        String className = null;

        if (node.object != null) {
            node.object.accept(this);
            obj = lastResult;
            className = node.object.type; // Type set by TypeCheckingVisitor
        } else {
            // Implicit this?
            obj = "this";
            className = currentClass;
        }

        // Mangle method name if class is known
        String methodName = node.methodName;
        if (className != null && !className.equals("int") && !className.equals("boolean") && !className.equals("string")) {
             methodName = className + "." + methodName;
        }

        // Emit PARAM for object (receiver)
        emit(OpCode.PARAM, obj, null, null);

        List<String> argTemps = new ArrayList<>();
        for (ASTNode arg : node.args) {
            arg.accept(this);
            argTemps.add(lastResult);
        }

        for (String arg : argTemps) {
            emit(OpCode.PARAM, arg, null, null);
        }

        String temp = newTemp();
        // Encode param count and signature
        int totalArgs = 1 + node.args.size();
        String signature = node.descriptor != null ? node.descriptor : "()V";
        emit(OpCode.CALL_VIRTUAL, temp, obj, methodName + ":" + totalArgs + ":" + signature);
        lastResult = temp;
    }

    @Override
    public void visitMemberAccessNode(MemberAccessNode node) {
        node.object.accept(this);
        String obj = lastResult;
        String temp = newTemp();
        String fieldName = node.memberName;
        if (node.object.type != null) {
            fieldName = node.object.type + ":" + fieldName;
        }
        emit(OpCode.GET_FIELD, temp, obj, fieldName);
        lastResult = temp;
    }
}
