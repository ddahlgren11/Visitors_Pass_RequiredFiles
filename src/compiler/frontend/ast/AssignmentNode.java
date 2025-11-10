package compiler.frontend.ast;

public class AssignmentNode extends StatementNode {
    private final ExpressionNode target;
    private final ExpressionNode value;

    public AssignmentNode(ExpressionNode target, ExpressionNode value) {
        this.target = target;
        this.value = value;
    }

    public ExpressionNode getTarget() { return target; }
    public ExpressionNode getValue() { return value; }

    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visit(this); }
}
