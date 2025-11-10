package compiler.frontend;

public class AssignmentNode extends ASTNodeBase {
    private final ASTNodeBase var;
    private final ASTNodeBase value;

    public AssignmentNode(ASTNodeBase var, ASTNodeBase value) {
        this.var = var;
        this.value = value;
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("=");
        root.addChild(var.toASTTestTree());
        root.addChild(value.toASTTestTree());
        return root;
    }
}
