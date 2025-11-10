package compiler.frontend.ast;

public class LiteralNode extends ExpressionNode {
    private final String value;
    public LiteralNode(String value) { this.value = value; }
    public String getValue() { return value; }
    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visit(this); }
}
