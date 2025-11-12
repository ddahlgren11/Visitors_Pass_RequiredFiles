package compiler.frontend.ast;

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

    @Override public void accept(ASTVisitor visitor) { visitor.visitVarDeclNode(this); }
}