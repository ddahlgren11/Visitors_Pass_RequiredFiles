package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class BinaryExprNode extends ExpressionNode {
    public final String op;
    public final ExpressionNode left;
    public final ExpressionNode right;

    public BinaryExprNode(String op, ExpressionNode left, ExpressionNode right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public String getOp() {
        return op;
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public ExpressionNode getRight() {
        return right;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBinaryExprNode(this);
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree(op);
        root.addChild(left.toASTTestTree());
        root.addChild(right.toASTTestTree());
        return root;
    }
}
