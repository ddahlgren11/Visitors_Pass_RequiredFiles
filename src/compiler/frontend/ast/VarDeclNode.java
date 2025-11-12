package compiler.frontend.ast;

public class VarDeclNode extends StatementNode {
    private final String type;
    private final String name;
    private final ExpressionNode init;

    public VarDeclNode(String type, String name, ExpressionNode init) {
        this.type = type;
        this.name = name;
        this.init = init;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public ExpressionNode getInit() { return init; }

    @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
}
