package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

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

    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visitFunctionDeclNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("FUNC " + returnType + " " + name);
        ASTTestTree paramsRoot = new ASTTestTree("PARAMS");
        for (VarDeclNode param : params) {
            paramsRoot.addChild(param.toASTTestTree());
        }
        root.addChild(paramsRoot);
        root.addChild(body.toASTTestTree());
        return root;
    }
}
