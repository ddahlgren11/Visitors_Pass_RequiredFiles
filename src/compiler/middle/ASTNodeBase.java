package compiler.middle;
/**
 * Minimal base class for all Abstract Syntax Tree nodes.
 * Contains the accept method required by the Visitor pattern.
 */
public abstract class ASTNodeBase {
    /**
     * Accepts a NodeVisitor, implementing the Visitor pattern.
     * @param visitor The visitor to accept.
     */
    public abstract void accept(NodeVisitor visitor);
}
