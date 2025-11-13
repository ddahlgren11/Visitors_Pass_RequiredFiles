package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class IfNode extends StatementNode {
    public final ExpressionNode cond;
    public final ASTNode thenBlock;
    public final ASTNode elseBlock;

    public IfNode(ExpressionNode cond, ASTNode thenBlock, ASTNode elseBlock) {
        this.cond = cond; this.thenBlock = thenBlock; this.elseBlock = elseBlock;
    }

    public ExpressionNode getCond() { return cond; }
    public ASTNode getThenBlock() { return thenBlock; }
    public ASTNode getElseBlock() { return elseBlock; }

    @Override public void accept(ASTVisitor visitor) { visitor.visitIfNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("IF");
        root.addChild(cond.toASTTestTree());
        root.addChild(thenBlock.toASTTestTree());
        if (elseBlock != null) {
            root.addChild(elseBlock.toASTTestTree());
        }
        return root;
    }
}
