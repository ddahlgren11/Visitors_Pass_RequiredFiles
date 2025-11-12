package compiler.frontend.ast;

public class BinaryOpNode extends ExpressionNode {
    public final String op;
    public final ExpressionNode left, right;

    public BinaryOpNode(String op, ExpressionNode left, ExpressionNode right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visitBinaryOpNode(this);
    }
}
