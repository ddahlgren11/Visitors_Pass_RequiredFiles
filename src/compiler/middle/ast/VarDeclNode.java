package compiler.middle.ast;

import compiler.middle.ASTNodeBase;
import compiler.middle.NodeVisitor;

import java.util.Objects;

public class VarDeclNode extends ASTNodeBase {
    private final String varName;
    private final ASTNodeBase value;

    public VarDeclNode(String varName, ASTNodeBase value) {
        this.varName = Objects.requireNonNull(varName);
        this.value = value;
    }

    public String getVarName() { return varName; }
    public ASTNodeBase getValue() { return value; }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
