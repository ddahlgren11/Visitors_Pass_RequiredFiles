package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class EmptyNode extends ASTNode {
    public EmptyNode() {}

    @Override
    public String toString() {
        return "<Empty>";
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visitEmptyNode(this);
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree tree = new ASTTestTree("Empty");
        return tree;
    }
}