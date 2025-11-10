package compiler.frontend;

public class BinaryOpNode extends ASTNodeBase {
    private final String op;
    private final ASTNodeBase left, right;

    public BinaryOpNode(String op, ASTNodeBase left, ASTNodeBase right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree(op);
        root.addChild(left.toASTTestTree());
        root.addChild(right.toASTTestTree());
        return root;
    }
}
