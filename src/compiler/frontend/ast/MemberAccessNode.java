package compiler.frontend.ast;
import compiler.frontend.ASTTestTree;

public class MemberAccessNode extends ExpressionNode {
    public ExpressionNode object;
    public String memberName;

    public MemberAccessNode(ExpressionNode object, String memberName) {
        this.object = object;
        this.memberName = memberName;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visitMemberAccessNode(this);
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree tree = new ASTTestTree("MemberAccess: " + memberName);
        tree.addChild(object.toASTTestTree());
        return tree;
    }
}
