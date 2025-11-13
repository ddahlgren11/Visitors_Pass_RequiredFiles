package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class ReturnNode extends StatementNode {
    public final ExpressionNode expr;
    public ReturnNode(ExpressionNode expr) { this.expr = expr; }
    public ExpressionNode getExpr() { return expr; }
    @Override public void accept(ASTVisitor visitor) { visitor.visitReturnNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("RETURN");
        if (expr != null) {
            root.addChild(expr.toASTTestTree());
        }
        return root;
    }
}
