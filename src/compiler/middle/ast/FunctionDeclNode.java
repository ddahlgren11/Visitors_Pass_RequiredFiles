package compiler.middle.ast;

import compiler.middle.ASTNodeBase;
import compiler.middle.NodeVisitor;

import java.util.List;

public class FunctionDeclNode extends ASTNodeBase {
    private final String functionName;
    private final List<String> parameters;
    private final ASTNodeBase body;

    public FunctionDeclNode(String functionName, List<String> parameters, ASTNodeBase body) {
        this.functionName = functionName;
        this.parameters = parameters;
        this.body = body;
    }

    public String getFunctionName() { return functionName; }
    public List<String> getParameters() { return parameters; }
    public ASTNodeBase getBody() { return body; }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
