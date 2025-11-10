package compiler.middle.ast;

import compiler.middle.*;
import compiler.middle.ASTNodeBase;
import compiler.middle.NodeVisitor;

public class AssignmentNode extends ASTNodeBase {
    private final String varName;
    private final ASTNodeBase value;

    public AssignmentNode(String varName, ASTNodeBase value) {
        this.varName = varName;
        this.value = value;
    }

    public String getVarName() { return varName; }
    public ASTNodeBase getValue() { return value; }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
