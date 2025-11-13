package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class WhileNode extends StatementNode {
    public final ExpressionNode cond;
    public final ASTNode body;

    public WhileNode(ExpressionNode cond, ASTNode body) {
        this.cond = cond; this.body = body;
    }

    public ExpressionNode getCond() { return cond; }
    public ASTNode getBody() { return body; }

    @Override public void accept(ASTVisitor visitor) { visitor.visitWhileNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("WHILE");
        root.addChild(cond.toASTTestTree());
        root.addChild(body.toASTTestTree());
        return root;
    }
}
