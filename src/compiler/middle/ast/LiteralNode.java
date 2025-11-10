package compiler.middle.ast;

import compiler.middle.ASTNodeBase;
import compiler.middle.NodeVisitor;

public class LiteralNode extends ASTNodeBase {
    private final Object value;
    public LiteralNode(Object value) { this.value = value; }
    public Object getValue() { return value; }
    @Override public void accept(NodeVisitor visitor) { /* no-op for builder */ }
}
