package compiler.frontend.ast;
import compiler.frontend.ASTNode;
import java.util.List;

public class BlockNode extends StatementNode {
    private final List<ASTNode> statements;

    public BlockNode(List<ASTNode> statements) { this.statements = statements; }

    public List<ASTNode> getStatements() { return statements; }

    @Override public void accept(ASTVisitor visitor) { visitor.visit(this); }
}
