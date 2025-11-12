package compiler.frontend.ast;

public class WhileNode extends StatementNode {
    public final ExpressionNode cond;
    public final BlockNode body;

    public WhileNode(ExpressionNode cond, BlockNode body) {
        this.cond = cond; this.body = body;
    }

    public ExpressionNode getCond() { return cond; }
    public BlockNode getBody() { return body; }

    @Override public void accept(ASTVisitor visitor) { visitor.visitWhileNode(this); }
}
