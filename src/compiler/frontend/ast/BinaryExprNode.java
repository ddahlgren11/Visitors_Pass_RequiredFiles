package compiler.frontend.ast;

public class BinaryExprNode extends ExpressionNode {
    public final String op;
    public final ExpressionNode left;
    public final ExpressionNode right;

    public BinaryExprNode(String op, ExpressionNode left, ExpressionNode right) {
        this.op = op; this.left = left; this.right = right;
    }

    public String getOp() { return op; }
    public ExpressionNode getLeft() { return left; }
    public ExpressionNode getRight() { return right; }

    @Override public void accept(ASTVisitor visitor) { visitor.visitBinaryExprNode(this); }
}
