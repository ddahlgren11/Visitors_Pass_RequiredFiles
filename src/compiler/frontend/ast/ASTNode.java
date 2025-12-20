package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

/**
 * Base node for the frontend AST used by visitor-based passes.
 * AST nodes are data holders; visitors implement behavior.
 */
public abstract class ASTNode {
    /** Accept a visitor and return a visitor-defined result. */
    public abstract <T> T accept(ASTVisitor<T> visitor);

    public abstract ASTTestTree toASTTestTree();
}
