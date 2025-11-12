package compiler.frontend;

import compiler.frontend.ast.ASTVisitor;

/**
 * Base node for the frontend AST used by visitor-based passes.
 * AST nodes are data holders; visitors implement behavior.
 */
public abstract class ASTNode extends ASTNodeBase {
    /** Accept a visitor and return a visitor-defined result. */
    public abstract void accept(ASTVisitor visitor);
}
