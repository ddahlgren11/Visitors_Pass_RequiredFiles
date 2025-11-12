package compiler.frontend.ast;
import compiler.frontend.ASTNode;
import java.util.List;

public class FunctionDeclNode extends ASTNode {
    private final String returnType;
    private final String name;
    private final List<VarDeclNode> params;
    private final BlockNode body;

    public FunctionDeclNode(String returnType, String name, List<VarDeclNode> params, BlockNode body) {
        this.returnType = returnType; this.name = name; this.params = params; this.body = body;
    }

    public String getReturnType() { return returnType; }
    public String getName() { return name; }
    public List<VarDeclNode> getParams() { return params; }
    public BlockNode getBody() { return body; }

    @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
}
