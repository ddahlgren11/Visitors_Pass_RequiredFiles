package compiler.middle;

import compiler.frontend.ast.*;
import compiler.middle.tac.OpCode;
import compiler.middle.tac.TACInstruction;

import java.util.ArrayList;
import java.util.List;

public class TACVisitor implements ASTVisitor {
    private final List<TACInstruction> tac = new ArrayList<>();
    private int tempCounter = 0;
    private int labelCounter = 0;

    public List<TACInstruction> getTac() {
        return tac;
    }

    private String newTemp() {
        return "t" + tempCounter++;
    }

    private String newLabel() {
        return "L" + labelCounter++;
    }

    @Override
    public void visit(BinaryExprNode node) {
        node.getLeft().accept(this);
        String leftTemp = ((ExpressionNode) node.getLeft()).result;

        node.getRight().accept(this);
        String rightTemp = ((ExpressionNode) node.getRight()).result;

        String dest = newTemp();
        OpCode opCode = getOpCode(node.getOp());
        tac.add(new TACInstruction(opCode, dest, leftTemp, rightTemp));
        node.result = dest;
    }

    @Override
    public void visit(AssignmentNode node) {
        node.getValue().accept(this);
        String exprResult = ((ExpressionNode) node.getValue()).result;
        String varName = ((IdentifierNode) node.getTarget()).getName();
        tac.add(new TACInstruction(OpCode.STORE_VAR, varName, exprResult, null));
    }

    @Override
    public void visit(VarDeclNode node) {
        if (node.getInit() != null) {
            node.getInit().accept(this);
            String initResult = ((ExpressionNode) node.getInit()).result;
            tac.add(new TACInstruction(OpCode.STORE_VAR, node.getName(), initResult, null));
        }
    }

    @Override
    public void visit(LiteralNode node) {
        String dest = newTemp();
        tac.add(new TACInstruction(OpCode.LOAD_CONST, dest, node.getValue(), null));
        node.result = dest;
    }

    @Override
    public void visit(IdentifierNode node) {
        String dest = newTemp();
        tac.add(new TACInstruction(OpCode.LOAD_VAR, dest, node.getName(), null));
        node.result = dest;
    }

    @Override
    public void visit(IfNode node) {
        String elseLabel = newLabel();
        String endLabel = newLabel();

        node.getCond().accept(this);
        String condResult = ((ExpressionNode) node.getCond()).result;

        tac.add(new TACInstruction(OpCode.JUMP_IF_ZERO, elseLabel, condResult, null));

        node.getThenBlock().accept(this);
        tac.add(new TACInstruction(OpCode.JUMP, endLabel, null, null));

        tac.add(new TACInstruction(OpCode.LABEL, elseLabel, null, null));
        if (node.getElseBlock() != null) {
            node.getElseBlock().accept(this);
        }

        tac.add(new TACInstruction(OpCode.LABEL, endLabel, null, null));
    }

    // Helper to map binary operators to OpCodes
    private OpCode getOpCode(String op) {
        switch (op) {
            case "+": return OpCode.ADD;
            case "-": return OpCode.SUB;
            case "*": return OpCode.MUL;
            case "/": return OpCode.DIV;
            default: throw new IllegalArgumentException("Unsupported operator: " + op);
        }
    }

    // Unimplemented methods for other node types
    @Override public void visit(BlockNode node) { for (compiler.frontend.ASTNode s : node.getStatements()) s.accept(this); }

    @Override
    public void visit(FunctionDeclNode node) {
        tac.add(new TACInstruction(OpCode.LABEL, node.getName(), null, null));
        for (VarDeclNode param : node.getParams()) {
            tac.add(new TACInstruction(OpCode.PARAM, param.getName(), null, null));
        }
        node.getBody().accept(this);
    }
    @Override
    public void visit(ReturnNode node) {
        if (node.getExpr() != null) {
            node.getExpr().accept(this);
            String returnVal = ((ExpressionNode) node.getExpr()).result;
            tac.add(new TACInstruction(OpCode.RETURN, returnVal, null, null));
        } else {
            tac.add(new TACInstruction(OpCode.RETURN, null, null, null));
        }
    }
    @Override
    public void visit(ForNode node) {
        String startLabel = newLabel();
        String endLabel = newLabel();
        String updateLabel = newLabel();

        if (node.getInit() != null) {
            node.getInit().accept(this);
        }

        tac.add(new TACInstruction(OpCode.LABEL, startLabel, null, null));

        if (node.getCond() != null) {
            node.getCond().accept(this);
            String condResult = ((ExpressionNode) node.getCond()).result;
            tac.add(new TACInstruction(OpCode.JUMP_IF_ZERO, endLabel, condResult, null));
        }

        node.getBody().accept(this);

        tac.add(new TACInstruction(OpCode.LABEL, updateLabel, null, null));
        if (node.getUpdate() != null) {
            node.getUpdate().accept(this);
        }
        tac.add(new TACInstruction(OpCode.JUMP, startLabel, null, null));

        tac.add(new TACInstruction(OpCode.LABEL, endLabel, null, null));
    }
    @Override
    public void visit(WhileNode node) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        tac.add(new TACInstruction(OpCode.LABEL, startLabel, null, null));

        node.getCond().accept(this);
        String condResult = ((ExpressionNode) node.getCond()).result;

        tac.add(new TACInstruction(OpCode.JUMP_IF_ZERO, endLabel, condResult, null));

        node.getBody().accept(this);

        tac.add(new TACInstruction(OpCode.JUMP, startLabel, null, null));
        tac.add(new TACInstruction(OpCode.LABEL, endLabel, null, null));
    }
}
