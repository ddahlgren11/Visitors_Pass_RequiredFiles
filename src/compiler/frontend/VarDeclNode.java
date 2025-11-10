package compiler.frontend;

public class VarDeclNode extends ASTNodeBase {
    private final String type, name;
    private final ASTNodeBase init;

    public VarDeclNode(String type, String name, ASTNodeBase init) {
        this.type = type;
        this.name = name;
        this.init = init;
    }

    public String getType() { return type; }
    public String getName() { return name; }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("decl");
        root.addChild(new ASTTestTree(type));
        root.addChild(new ASTTestTree(name));
        if (init != null) {
            root.addChild(init.toASTTestTree());
        }
        return root;
    }
}
