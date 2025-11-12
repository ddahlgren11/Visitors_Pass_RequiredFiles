package compiler.frontend.ast;

public class AssignmentNode extends StatementNode {
    public final ExpressionNode target;
    public final ExpressionNode expression;

    public AssignmentNode(ExpressionNode target, ExpressionNode expression) {
        this.target = target;
        this.expression = expression;
    }

    public ExpressionNode getTarget() { return target; }
    public ExpressionNode getExpression() { return expression; }

    @Override public void accept(ASTVisitor visitor) { visitor.visitAssignmentNode(this); }
}
