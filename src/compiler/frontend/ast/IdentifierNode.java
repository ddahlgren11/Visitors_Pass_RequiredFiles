package compiler.frontend.ast;

public class IdentifierNode extends ExpressionNode {
    private final String name;
    public IdentifierNode(String name) { this.name = name; }
    public String getName() { return name; }
    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visit(this); }
}
// Legacy IdentifierNode removed