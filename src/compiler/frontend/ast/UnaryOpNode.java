package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class UnaryOpNode extends ExpressionNode {
    public final String op;
    public final ExpressionNode expr;

    public UnaryOpNode(String op, ExpressionNode expr) {
        this.op = op;
        this.expr = expr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitUnaryOpNode(this);
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree(op);
        root.addChild(expr.toASTTestTree());
        return root;
    }
}
