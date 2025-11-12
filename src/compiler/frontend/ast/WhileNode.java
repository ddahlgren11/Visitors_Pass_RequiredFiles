package compiler.frontend.ast;

public class WhileNode extends StatementNode {
    private final ExpressionNode cond;
    private final BlockNode body;

    public WhileNode(ExpressionNode cond, BlockNode body) {
        this.cond = cond; this.body = body;
    }

    public ExpressionNode getCond() { return cond; }
    public BlockNode getBody() { return body; }

    @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
}
