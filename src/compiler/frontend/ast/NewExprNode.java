package compiler.frontend.ast;
import compiler.frontend.ASTTestTree;

public class NewExprNode extends ExpressionNode {
    public String className;
    public java.util.List<ExpressionNode> args;

    public NewExprNode(String className, java.util.List<ExpressionNode> args) {
        this.className = className;
        this.args = args;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visitNewExprNode(this);
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree tree = new ASTTestTree("New: " + className);
        for (ExpressionNode arg : args) {
            tree.addChild(arg.toASTTestTree());
        }
        return tree;
    }
}
