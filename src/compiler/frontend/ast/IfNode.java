package compiler.frontend.ast;

public class IfNode extends StatementNode {
    private final ExpressionNode cond;
    private final BlockNode thenBlock;
    private final BlockNode elseBlock;

    public IfNode(ExpressionNode cond, BlockNode thenBlock, BlockNode elseBlock) {
        this.cond = cond; this.thenBlock = thenBlock; this.elseBlock = elseBlock;
    }

    public ExpressionNode getCond() { return cond; }
    public BlockNode getThenBlock() { return thenBlock; }
    public BlockNode getElseBlock() { return elseBlock; }

    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visit(this); }
}
