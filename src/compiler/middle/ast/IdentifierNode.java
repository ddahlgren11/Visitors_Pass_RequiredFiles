package compiler.middle.ast;

import compiler.middle.ASTNodeBase;
import compiler.middle.NodeVisitor;

public class IdentifierNode extends ASTNodeBase {
    private final String name;
    public IdentifierNode(String name) { this.name = name; }
    public String getName() { return name; }
    @Override public void accept(NodeVisitor visitor) { /* no-op for builder */ }
}
