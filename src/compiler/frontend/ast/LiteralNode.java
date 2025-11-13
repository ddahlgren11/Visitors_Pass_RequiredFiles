package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class LiteralNode extends ExpressionNode {
    public final String value;
    public LiteralNode(String value) { this.value = value; }
    public String getValue() { return value; }
    @Override public void accept(ASTVisitor visitor) { visitor.visitLiteralNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        return new ASTTestTree("LIT(" + value + ")");
    }
}
