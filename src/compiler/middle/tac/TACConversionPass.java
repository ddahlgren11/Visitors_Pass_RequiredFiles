package compiler.middle.tac;

import java.util.ArrayList;
import java.util.List;

import compiler.frontend.ast.ASTNode;
import compiler.frontend.ast.AssignmentNode;
import compiler.frontend.ast.ASTVisitor;
import compiler.frontend.ast.BinaryOpNode;
import compiler.frontend.ast.BinaryExprNode;
import compiler.frontend.ast.BlockNode;
import compiler.frontend.ast.ForNode;
import compiler.frontend.ast.FunctionDeclNode;
import compiler.frontend.ast.IdentifierNode;
import compiler.frontend.ast.IfNode;
import compiler.frontend.ast.LiteralNode;
import compiler.frontend.ast.ReturnNode;
import compiler.frontend.ast.VarDeclNode;
import compiler.frontend.ast.WhileNode;
import compiler.frontend.ast.ClassDeclNode;
import compiler.frontend.ast.NewExprNode;
import compiler.frontend.ast.MethodCallNode;
import compiler.frontend.ast.MemberAccessNode;
import compiler.frontend.ast.UnaryOpNode;
import compiler.frontend.ast.EmptyNode;
import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;

public class TACConversionPass implements ASTVisitor, CompilerPass {

    private List<TACInstruction> mainInstructions = new ArrayList<>();
    private List<TACInstruction> functionInstructions = new ArrayList<>();
    private List<TACInstruction> currentInstructions;

    private int tempCount = 0;
    private int labelCount = 0;
    private String lastResult;

    public TACConversionPass() {
        this.currentInstructions = mainInstructions;
    }

    @Override
    public String name() {
        return "IR Generation (TAC)";
    }

    @Override
    public void execute(CompilerContext context) throws Exception {
        ASTNode ast = context.getAst();
        if (ast != null) {
            ast.accept(this);
        }

        // Combine instructions: main first, then functions (or vice-versa, main usually at end or start)
        // We'll put main first.
        List<TACInstruction> allInstructions = new ArrayList<>(mainInstructions);
        // Add a return to main if not present, though usually implicit or handled by exit
        // allInstructions.add(new TACInstruction(OpCode.RETURN, null, null, null));
        allInstructions.addAll(functionInstructions);

        context.setTACInstructions(allInstructions);
    }

    private String newTemp() {
        return "t" + (tempCount++);
    }

    private String newLabel() {
        return "L" + (labelCount++);
    }

    private void emit(OpCode op, String arg1, String arg2, String arg3) {
        currentInstructions.add(new TACInstruction(op, arg1, arg2, arg3));
    }

    @Override
    public void visitLiteralNode(LiteralNode node) {
        String temp = newTemp();
        emit(OpCode.LOAD_CONST, temp, node.value.toString(), null);
        lastResult = temp;
    }

    @Override
    public void visitIdentifierNode(IdentifierNode node) {
        lastResult = node.name; // In TAC, variables are used directly by name
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
            case "%" -> OpCode.MOD;
            case "&&" -> OpCode.AND;
            case "||" -> OpCode.OR;
            case "<" -> OpCode.LT;
            case ">" -> OpCode.GT;
            case "==" -> OpCode.EQ;
            case "!=" -> OpCode.NEQ;
            // Add more as needed
            default -> throw new RuntimeException("Unknown operator: " + node.op);
        };

        emit(op, temp, left, right);
        lastResult = temp;
    }

    @Override
    public void visitBinaryExprNode(BinaryExprNode node) {
        // Fallback if BinaryExprNode is used
         node.left.accept(this);
        String left = lastResult;
        node.right.accept(this);
        String right = lastResult;

        String temp = newTemp();
        OpCode op = switch(node.op) {
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
        } else {
            // Optional: initialize to 0 or null
             emit(OpCode.LOAD_CONST, node.name, "0", null);
        }
        lastResult = node.name;
    }

    @Override
    public void visitAssignmentNode(AssignmentNode node) {
        node.expression.accept(this);
        String value = lastResult;
        String targetName = ((IdentifierNode)node.target).name;
        emit(OpCode.STORE_VAR, targetName, value, null);
        lastResult = targetName;
    }

    @Override
    public void visitFunctionDeclNode(FunctionDeclNode node) {
        List<TACInstruction> previousInstructions = currentInstructions;
        currentInstructions = new ArrayList<>();

        emit(OpCode.FUNC_START, node.name, null, null);

        node.body.accept(this);

        emit(OpCode.FUNC_END, node.name, null, null);

        functionInstructions.addAll(currentInstructions);
        currentInstructions = previousInstructions;
        lastResult = null;
    }

    @Override
    public void visitBlockNode(BlockNode node) {
        for (ASTNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
        lastResult = null;
    }

    @Override
    public void visitReturnNode(ReturnNode node) {
        String val = null;
        if (node.expr != null) {
            node.expr.accept(this);
            val = lastResult;
        }
        emit(OpCode.RETURN, val, null, null);
        lastResult = null;
    }

    @Override
    public void visitIfNode(IfNode node) {
        String labelFalse = newLabel();
        String labelEnd = newLabel();

        node.cond.accept(this);
        String cond = lastResult;

        emit(OpCode.IFZ, cond, labelFalse, null);

        node.thenBlock.accept(this);
        emit(OpCode.GOTO, labelEnd, null, null);

        emit(OpCode.LABEL, labelFalse, null, null);
        if (node.elseBlock != null) {
            node.elseBlock.accept(this);
        }

        emit(OpCode.LABEL, labelEnd, null, null);
        lastResult = null;
    }

    @Override
    public void visitWhileNode(WhileNode node) {
        String labelStart = newLabel();
        String labelEnd = newLabel();

        emit(OpCode.LABEL, labelStart, null, null);

        node.cond.accept(this);
        String cond = lastResult;

        emit(OpCode.IFZ, cond, labelEnd, null);

        node.body.accept(this);

        emit(OpCode.GOTO, labelStart, null, null);
        emit(OpCode.LABEL, labelEnd, null, null);

        lastResult = null;
    }

    @Override
    public void visitForNode(ForNode node) {
        if (node.init != null) node.init.accept(this);

        String labelStart = newLabel();
        String labelEnd = newLabel();

        emit(OpCode.LABEL, labelStart, null, null);

        if (node.cond != null) {
            node.cond.accept(this);
            String cond = lastResult;
            emit(OpCode.IFZ, cond, labelEnd, null);
        }

        node.body.accept(this);

        if (node.update != null) node.update.accept(this);

        emit(OpCode.GOTO, labelStart, null, null);
        emit(OpCode.LABEL, labelEnd, null, null);

        lastResult = null;
    }

    @Override
    public void visitUnaryOpNode(UnaryOpNode node) {
        node.expr.accept(this);
        String val = lastResult;
        String temp = newTemp();

        // Handle unary minus or not
        OpCode op = switch(node.op) {
            case "-" -> OpCode.SUB; // We might do 0 - val
            case "!" -> OpCode.NOT;
            default -> null; // Unknown
        };

        if (op == OpCode.SUB) {
            // emulate -x as 0 - x
             emit(OpCode.LOAD_CONST, temp, "0", null);
             String temp2 = newTemp();
             emit(OpCode.SUB, temp2, temp, val);
             lastResult = temp2;
        } else if (op == OpCode.NOT) {
             emit(OpCode.NOT, temp, val, null);
             lastResult = temp;
        } else {
             // Just pass through if unknown or handled elsewhere
             lastResult = val;
        }
    }

    @Override
    public void visitEmptyNode(EmptyNode emptyNode) {
        // Do nothing
    }

    @Override
    public void visitClassDeclNode(ClassDeclNode node) {
        // Not implemented for this simple TAC
    }

    @Override
    public void visitNewExprNode(NewExprNode node) {
        // Not fully supported, but can emit a call
        String temp = newTemp();
        emit(OpCode.CALL, temp, "new " + node.className, null);
        lastResult = temp;
    }

    @Override
    public void visitMethodCallNode(MethodCallNode node) {
        for (compiler.frontend.ast.ExpressionNode arg : node.args) {
            arg.accept(this);
            emit(OpCode.PARAM, lastResult, null, null);
        }
        String temp = newTemp();
        emit(OpCode.CALL, temp, node.methodName, null);
        lastResult = temp;
    }

    @Override
    public void visitMemberAccessNode(MemberAccessNode node) {
        // Simplified member access
        node.object.accept(this);
        String obj = lastResult;
        // We might just return obj.member as a string name for now
        lastResult = obj + "." + node.memberName;
    }
}
