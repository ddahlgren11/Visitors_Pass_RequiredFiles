package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public class AssignmentNode extends StatementNode {
    public final ExpressionNode target;
    public final ExpressionNode expression;

    public AssignmentNode(ExpressionNode target, ExpressionNode expression) {
        this.target = target;
        this.expression = expression;
    }

    public ExpressionNode getTarget() { return target; }
    public ExpressionNode getExpression() { return expression; }

    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visitAssignmentNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("ASSIGN");
        root.addChild(target.toASTTestTree());
        root.addChild(expression.toASTTestTree());
        return root;
    }
}
