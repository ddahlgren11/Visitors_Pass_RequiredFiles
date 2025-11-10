package compiler.frontend.ast;

public class BinaryExprNode extends ExpressionNode {
    private final String op;
    private final ExpressionNode left;
    private final ExpressionNode right;

    public BinaryExprNode(String op, ExpressionNode left, ExpressionNode right) {
        this.op = op; this.left = left; this.right = right;
    }

    public String getOp() { return op; }
    public ExpressionNode getLeft() { return left; }
    public ExpressionNode getRight() { return right; }

    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visit(this); }
}
