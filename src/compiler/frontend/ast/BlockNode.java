package compiler.frontend.ast;

import java.util.List;

public class BlockNode extends StatementNode {
    private final List<ASTNode> statements;

    public BlockNode(List<ASTNode> statements) { this.statements = statements; }

    public List<ASTNode> getStatements() { return statements; }

    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visit(this); }
}
