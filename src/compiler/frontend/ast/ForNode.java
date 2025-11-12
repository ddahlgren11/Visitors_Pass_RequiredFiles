package compiler.frontend.ast;

public class ForNode extends StatementNode {
    private final StatementNode init;
    private final ExpressionNode cond;
    private final StatementNode update;
    private final BlockNode body;

    public ForNode(StatementNode init, ExpressionNode cond, StatementNode update, BlockNode body) {
        this.init = init; this.cond = cond; this.update = update; this.body = body;
    }

    public StatementNode getInit() { return init; }
    public ExpressionNode getCond() { return cond; }
    public StatementNode getUpdate() { return update; }
    public BlockNode getBody() { return body; }

    @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
}
