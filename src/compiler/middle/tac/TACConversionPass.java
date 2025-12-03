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

public class TACConversionPass implements ASTVisitor {

    private final List<TACInstruction> instructions = new ArrayList<>();
    private int tempCount = 0;
    private String lastResult;

    private String newTemp() {
        return "t" + (tempCount++);
    }

    public List<TACInstruction> getInstructions() {
        return instructions;
    }

    @Override
    public void visitLiteralNode(LiteralNode node) {
        String temp = newTemp();
        instructions.add(new TACInstruction(OpCode.LOAD_CONST, temp, node.value.toString(), null));
        lastResult = temp;
    }

    @Override
    public void visitIdentifierNode(IdentifierNode node) {
        lastResult = node.name; // no TAC
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
            default -> throw new RuntimeException("Unknown operator");
        };

        instructions.add(new TACInstruction(op, temp, left, right));
        lastResult = temp;
    }

    @Override
    public void visitVarDeclNode(VarDeclNode node) {
        node.initializer.accept(this);
        String value = lastResult;
        instructions.add(new TACInstruction(OpCode.STORE_VAR, node.name, value, null));
        lastResult = node.name;
    }

    @Override
    public void visitAssignmentNode(AssignmentNode node) {
        node.expression.accept(this);
        String value = lastResult;
        String targetName = ((IdentifierNode)node.target).name;
        instructions.add(new TACInstruction(OpCode.STORE_VAR, targetName, value, null));
        lastResult = targetName;
    }

    @Override
    public void visitFunctionDeclNode(FunctionDeclNode node) {
        for (ASTNode stmt : node.body.getStatements()) {
            stmt.accept(this);
        }
        lastResult = null;
    }

    @Override
    public void visitBinaryExprNode(BinaryExprNode node) {
        // TODO Auto-generated method stub
        lastResult = null;
    }

    @Override
    public void visitBlockNode(BlockNode node) {
        // TODO Auto-generated method stub
        lastResult = null;
    }

    @Override
    public void visitReturnNode(ReturnNode node) {
        // TODO Auto-generated method stub
        lastResult = null;
    }

    @Override
    public void visitIfNode(IfNode node) {
        // TODO Auto-generated method stub
        lastResult = null;
    }

    @Override
    public void visitForNode(ForNode node) {
        // TODO Auto-generated method stub
        lastResult = null;
    }

    @Override
    public void visitWhileNode(WhileNode node) {
        // TODO Auto-generated method stub
        lastResult = null;
    }

    @Override
    public void visitUnaryOpNode(compiler.frontend.ast.UnaryOpNode node) {
        // TODO Auto-generated method stub
        lastResult = null;
    }
}
