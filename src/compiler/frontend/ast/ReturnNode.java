package compiler.frontend.ast;

public class ReturnNode extends StatementNode {
    private final ExpressionNode expr;
    public ReturnNode(ExpressionNode expr) { this.expr = expr; }
    public ExpressionNode getExpr() { return expr; }
    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visit(this); }
}