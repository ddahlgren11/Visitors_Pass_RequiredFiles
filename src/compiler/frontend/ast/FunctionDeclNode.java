package compiler.frontend.ast;

import java.util.List;

public class FunctionDeclNode extends ASTNode {
    public final String returnType;
    public final String name;
    public final List<VarDeclNode> params;
    public final BlockNode body;

    public FunctionDeclNode(String returnType, String name, List<VarDeclNode> params, BlockNode body) {
        this.returnType = returnType; this.name = name; this.params = params; this.body = body;
    }

    public String getReturnType() { return returnType; }
    public String getName() { return name; }
    public List<VarDeclNode> getParams() { return params; }
    public BlockNode getBody() { return body; }

    @Override public void accept(ASTVisitor visitor) { visitor.visitFunctionDeclNode(this); }
}
