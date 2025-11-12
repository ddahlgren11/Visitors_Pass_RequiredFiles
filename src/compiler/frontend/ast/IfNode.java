package compiler.frontend.ast;

public class IfNode extends StatementNode {
    public final ExpressionNode cond;
    public final BlockNode thenBlock;
    public final BlockNode elseBlock;

    public IfNode(ExpressionNode cond, BlockNode thenBlock, BlockNode elseBlock) {
        this.cond = cond; this.thenBlock = thenBlock; this.elseBlock = elseBlock;
    }

    public ExpressionNode getCond() { return cond; }
    public BlockNode getThenBlock() { return thenBlock; }
    public BlockNode getElseBlock() { return elseBlock; }

    @Override public void accept(ASTVisitor visitor) { visitor.visitIfNode(this); }
}
