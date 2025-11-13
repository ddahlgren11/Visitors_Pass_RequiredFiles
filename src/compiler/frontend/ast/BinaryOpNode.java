package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class BinaryOpNode extends ExpressionNode {
    public final String op;
    public final ExpressionNode left, right;

    public BinaryOpNode(String op, ExpressionNode left, ExpressionNode right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visitBinaryOpNode(this);
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree(op);
        root.addChild(left.toASTTestTree());
        root.addChild(right.toASTTestTree());
        return root;
    }
}
