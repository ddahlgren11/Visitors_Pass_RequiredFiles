package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class VarDeclNode extends StatementNode {
    public final String type;
    public final String name;
    public final ExpressionNode initializer;

    public VarDeclNode(String type, String name, ExpressionNode initializer) {
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public ExpressionNode getInitializer() { return initializer; }

    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visitVarDeclNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("VAR_DECL " + type + " " + name);
        if (initializer != null) {
            root.addChild(initializer.toASTTestTree());
        }
        return root;
    }
}
