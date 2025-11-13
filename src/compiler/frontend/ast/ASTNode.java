package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;
import compiler.middle.tac.TACConversionPass;

/**
 * Base node for the frontend AST used by visitor-based passes.
 * AST nodes are data holders; visitors implement behavior.
 */
public abstract class ASTNode {
    /** Accept a visitor and return a visitor-defined result. */
    public abstract void accept(ASTVisitor visitor);

    public abstract ASTTestTree toASTTestTree();
}
