package compiler.middle.tac;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.frontend.ast.*;

public class TACConversionPass implements CompilerPass, ASTVisitor<String> {

    private final List<TACInstruction> instructions = new ArrayList<>();
    private int tempCount = 0;
    private int labelCount = 0;
    private String currentClass;

    // Map<ClassName, Map<MethodName, Signature>>
    private Map<String, Map<String, String>> methodSignatures = new HashMap<>();

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
            buildSignatureMap(root);
            root.accept(this);
            context.setTacInstructions(instructions);
        }
    }

    private void buildSignatureMap(ASTNode root) {
        if (root instanceof BlockNode) {
            for (ASTNode stmt : ((BlockNode)root).getStatements()) {
                if (stmt instanceof ClassDeclNode) {
                    ClassDeclNode c = (ClassDeclNode) stmt;
                    Map<String, String> methods = new HashMap<>();
                    for (FunctionDeclNode m : c.methods) {
                         StringBuilder sig = new StringBuilder("(");
                         for (VarDeclNode p : m.getParams()) {
                             sig.append(getDescriptor(p.type));
                         }
                         sig.append(")").append(getDescriptor(m.returnType));

                         // Constructor return type fix
                         if (m.returnType.equals(c.className)) {
                             int idx = sig.lastIndexOf(")");
                             if (idx != -1) {
                                 sig.replace(idx + 1, sig.length(), "V");
                             }
                         }

                         methods.put(m.name, sig.toString());
                    }
                    methodSignatures.put(c.className, methods);
                }
            }
        }
    }

    public List<TACInstruction> getInstructions() {
        return instructions;
    }

    private void emit(OpCode op, String target, String arg1, String arg2) {
        instructions.add(new TACInstruction(op, target, arg1, arg2));
    }

    @Override
    public String visitLiteralNode(LiteralNode node) {
        String temp = newTemp();
        emit(OpCode.LOAD_CONST, temp, node.value.toString(), null);
        return temp;
    }

    @Override
    public String visitIdentifierNode(IdentifierNode node) {
        if ("this".equals(node.name)) {
            return "this";
        }
        String temp = newTemp();
        emit(OpCode.LOAD_VAR, temp, node.name, null);
        return temp;
    }

    @Override
    public String visitBinaryOpNode(BinaryOpNode node) {
        String left = node.left.accept(this);
        String right = node.right.accept(this);

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
        return temp;
    }

    @Override
    public String visitVarDeclNode(VarDeclNode node) {
        if (node.initializer != null) {
            String value = node.initializer.accept(this);
            emit(OpCode.STORE_VAR, node.name, value, null);
        }
        return null;
    }

    @Override
    public String visitAssignmentNode(AssignmentNode node) {
        String value = node.expression.accept(this);

        if (node.target instanceof IdentifierNode) {
            String targetName = ((IdentifierNode)node.target).name;
            emit(OpCode.STORE_VAR, targetName, value, null);
            return value;
        } else if (node.target instanceof MemberAccessNode) {
            MemberAccessNode man = (MemberAccessNode) node.target;
            String obj = man.object.accept(this);
            String fieldName = man.memberName;
            if (man.object.type != null) {
                fieldName = man.object.type + ":" + fieldName;
            }
            emit(OpCode.PUT_FIELD, obj, fieldName, value);
            return value;
        } else {
             throw new RuntimeException("Unsupported assignment target");
        }
    }

    @Override
    public String visitFunctionDeclNode(FunctionDeclNode node) {
        StringBuilder sig = new StringBuilder("(");
        for (VarDeclNode param : node.getParams()) {
            sig.append(getDescriptor(param.type));
        }
        sig.append(")").append(getDescriptor(node.returnType));

        emit(OpCode.FUNC_ENTRY, node.name, String.valueOf(node.getParams().size()), sig.toString());

        for (VarDeclNode param : node.getParams()) {
             emit(OpCode.PARAM_DECL, param.name, getDescriptor(param.type), null);
        }

        emit(OpCode.LABEL, node.name, null, null);

        node.body.accept(this);

        if (node.returnType.equals("void")) {
             emit(OpCode.RETURN, null, null, null);
        }
        emit(OpCode.FUNC_EXIT, node.name, null, null);

        return null;
    }

    @Override
    public String visitBinaryExprNode(BinaryExprNode node) {
        String left = node.getLeft().accept(this);
        String right = node.getRight().accept(this);

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
        return temp;
    }

    @Override
    public String visitBlockNode(BlockNode node) {
        for (ASTNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
        return null;
    }

    @Override
    public String visitReturnNode(ReturnNode node) {
        String val = null;
        if (node.getExpr() != null) {
            val = node.getExpr().accept(this);
        }
        emit(OpCode.RETURN, val, null, null);
        return null;
    }

    @Override
    public String visitIfNode(IfNode node) {
        String elseLabel = newLabel();
        String endLabel = newLabel();

        String cond = node.getCond().accept(this);

        emit(OpCode.IFZ, cond, elseLabel, null);

        node.getThenBlock().accept(this);
        emit(OpCode.GOTO, endLabel, null, null);

        emit(OpCode.LABEL, elseLabel, null, null);
        if (node.getElseBlock() != null) {
            node.getElseBlock().accept(this);
        }

        emit(OpCode.LABEL, endLabel, null, null);
        return null;
    }

    @Override
    public String visitWhileNode(WhileNode node) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        emit(OpCode.LABEL, startLabel, null, null);

        String cond = node.getCond().accept(this);

        emit(OpCode.IFZ, cond, endLabel, null);

        node.getBody().accept(this);
        emit(OpCode.GOTO, startLabel, null, null);

        emit(OpCode.LABEL, endLabel, null, null);
        return null;
    }

    @Override
    public String visitForNode(ForNode node) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        if (node.getInit() != null) {
            node.getInit().accept(this);
        }

        emit(OpCode.LABEL, startLabel, null, null);

        if (node.getCond() != null) {
            String cond = node.getCond().accept(this);
            emit(OpCode.IFZ, cond, endLabel, null);
        }

        node.getBody().accept(this);

        if (node.getUpdate() != null) {
            node.getUpdate().accept(this);
        }

        emit(OpCode.GOTO, startLabel, null, null);
        emit(OpCode.LABEL, endLabel, null, null);
        return null;
    }

    @Override
    public String visitUnaryOpNode(UnaryOpNode node) {
        String val = node.expr.accept(this);
        String temp = newTemp();

        OpCode op = switch(node.op) {
            case "!" -> OpCode.NOT;
            case "-" -> OpCode.NEG;
            default -> throw new RuntimeException("Unary op not fully supported in TAC yet: " + node.op);
        };

        emit(op, temp, val, null);
        return temp;
    }

    @Override
    public String visitEmptyNode(EmptyNode emptyNode) {
        return null;
    }

    @Override
    public String visitClassDeclNode(ClassDeclNode node) {
        String prevClass = currentClass;
        currentClass = node.className;

        for (VarDeclNode field : node.fields) {
            emit(OpCode.FIELD_DECL, null, node.className, field.name);
        }

        for (FunctionDeclNode method : node.methods) {
            String mangledName = node.className + "." + method.name;
            int paramCount = 1 + method.getParams().size(); // +1 for this

            StringBuilder sig = new StringBuilder("(");
            for (VarDeclNode param : method.getParams()) {
                sig.append(getDescriptor(param.type));
            }
            sig.append(")").append(getDescriptor(method.returnType));

            if (method.returnType.equals(node.className)) {
                 int closeParen = sig.lastIndexOf(")");
                 sig.replace(closeParen + 1, sig.length(), "V");
            }

            emit(OpCode.FUNC_ENTRY, mangledName, String.valueOf(paramCount), sig.toString());

            emit(OpCode.PARAM_DECL, "this", "L" + node.className + ";", null);

            for (VarDeclNode param : method.getParams()) {
                 emit(OpCode.PARAM_DECL, param.name, getDescriptor(param.type), null);
            }

            emit(OpCode.LABEL, mangledName, null, null);

            method.body.accept(this);

            if (method.returnType.equals("void")) {
                 emit(OpCode.RETURN, null, null, null);
            }
            emit(OpCode.FUNC_EXIT, mangledName, null, null);
        }

        currentClass = prevClass;
        return null;
    }

    @Override
    public String visitNewExprNode(NewExprNode node) {
        String temp = newTemp();
        emit(OpCode.NEW_ALLOC, temp, node.className, null);

        List<String> argTemps = new ArrayList<>();
        for (ASTNode arg : node.args) {
            argTemps.add(arg.accept(this));
        }

        for (String arg : argTemps) {
            emit(OpCode.PARAM, arg, null, null);
        }

        String signature = "()V";
        if (methodSignatures.containsKey(node.className)) {
            Map<String, String> methods = methodSignatures.get(node.className);
            if (methods != null && methods.containsKey(node.className)) {
                signature = methods.get(node.className);
            }
        }

        emit(OpCode.NEW_CONSTRUCT, temp, node.className, signature);
        return temp;
    }

    @Override
    public String visitMethodCallNode(MethodCallNode node) {
        String obj = null;
        String className = null;

        if (node.object != null) {
            obj = node.object.accept(this);
            className = node.object.type;
        } else {
            obj = "this";
            className = currentClass;
        }

        String methodName = node.methodName;
        String signature = "()I";

        if (className != null && !className.equals("int") && !className.equals("boolean") && !className.equals("string")) {
             methodName = className + "." + node.methodName;
             if (methodSignatures.containsKey(className)) {
                 Map<String, String> methods = methodSignatures.get(className);
                 if (methods != null && methods.containsKey(node.methodName)) {
                     signature = methods.get(node.methodName);
                 }
             }
        }

        emit(OpCode.PARAM, obj, null, null);

        List<String> argTemps = new ArrayList<>();
        for (ASTNode arg : node.args) {
            argTemps.add(arg.accept(this));
        }

        for (String arg : argTemps) {
            emit(OpCode.PARAM, arg, null, null);
        }

        String temp = newTemp();
        emit(OpCode.CALL_VIRTUAL, temp, obj, methodName + ":" + signature);
        return temp;
    }

    @Override
    public String visitMemberAccessNode(MemberAccessNode node) {
        String obj = node.object.accept(this);
        String temp = newTemp();
        String fieldName = node.memberName;
        if (node.object.type != null) {
            fieldName = node.object.type + ":" + fieldName;
        }
        emit(OpCode.GET_FIELD, temp, obj, fieldName);
        return temp;
    }
}
