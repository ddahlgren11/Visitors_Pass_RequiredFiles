package compiler.frontend.ast;

public class LiteralNode extends ExpressionNode {
    public final String value;
    public LiteralNode(String value) { this.value = value; }
    public String getValue() { return value; }
    @Override public void accept(ASTVisitor visitor) { visitor.visitLiteralNode(this); }
}
