package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class IdentifierNode extends ExpressionNode {
    public final String name;
    public IdentifierNode(String name) { this.name = name; }
    public String getName() { return name; }
    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visitIdentifierNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        return new ASTTestTree("ID(" + name + ")");
    }
}
