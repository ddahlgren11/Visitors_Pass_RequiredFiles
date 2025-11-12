package compiler.frontend.ast;

public class ForNode extends StatementNode {
    public final StatementNode init;
    public final ExpressionNode cond;
    public final StatementNode update;
    public final BlockNode body;

    public ForNode(StatementNode init, ExpressionNode cond, StatementNode update, BlockNode body) {
        this.init = init; this.cond = cond; this.update = update; this.body = body;
    }

    public StatementNode getInit() { return init; }
    public ExpressionNode getCond() { return cond; }
    public StatementNode getUpdate() { return update; }
    public BlockNode getBody() { return body; }

    @Override public void accept(ASTVisitor visitor) { visitor.visitForNode(this); }
}
