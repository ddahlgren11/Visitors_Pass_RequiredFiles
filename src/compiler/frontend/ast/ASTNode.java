package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;
import compiler.infra.SourceLocation;

/**
 * Base node for the frontend AST used by visitor-based passes.
 * AST nodes are data holders; visitors implement behavior.
 */
public abstract class ASTNode {
    private SourceLocation sourceLocation;

    /** Accept a visitor and return a visitor-defined result. */
    public abstract <T> T accept(ASTVisitor<T> visitor);

    public abstract ASTTestTree toASTTestTree();

    public void setSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }
}
