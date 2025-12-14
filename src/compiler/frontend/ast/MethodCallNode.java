package compiler.frontend.ast;

import java.util.List;
import compiler.frontend.ASTTestTree;

public class MethodCallNode extends ExpressionNode {
    public ExpressionNode object; // null for local function call
    public String methodName;
    public List<ExpressionNode> args;

    public MethodCallNode(ExpressionNode object, String methodName, List<ExpressionNode> args) {
        this.object = object;
        this.methodName = methodName;
        this.args = args;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visitMethodCallNode(this);
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree tree = new ASTTestTree("MethodCall: " + methodName);
        if (object != null) {
            tree.addChild(object.toASTTestTree());
        }
        for (ExpressionNode arg : args) {
            tree.addChild(arg.toASTTestTree());
        }
        return tree;
    }
}
