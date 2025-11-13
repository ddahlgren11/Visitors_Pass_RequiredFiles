package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class ForNode extends StatementNode {
    public final ASTNode init;
    public final ExpressionNode cond;
    public final ASTNode update;
    public final ASTNode body;

    public ForNode(ASTNode init, ExpressionNode cond, ASTNode update, ASTNode body) {
        this.init = init; this.cond = cond; this.update = update; this.body = body;
    }

    public ASTNode getInit() { return init; }
    public ExpressionNode getCond() { return cond; }
    public ASTNode getUpdate() { return update; }
    public ASTNode getBody() { return body; }

    @Override public void accept(ASTVisitor visitor) { visitor.visitForNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("FOR");
        if (init != null) root.addChild(init.toASTTestTree());
        if (cond != null) root.addChild(cond.toASTTestTree());
        if (update != null) root.addChild(update.toASTTestTree());
        root.addChild(body.toASTTestTree());
        return root;
    }
}
