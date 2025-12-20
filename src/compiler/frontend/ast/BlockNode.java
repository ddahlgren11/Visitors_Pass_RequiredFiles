package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

import java.util.List;

public class BlockNode extends StatementNode {
    public final List<ASTNode> statements;

    public BlockNode(List<ASTNode> statements) { this.statements = statements; }

    public List<ASTNode> getStatements() { return statements; }

    @Override public <T> T accept(ASTVisitor<T> visitor) { return visitor.visitBlockNode(this); }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree root = new ASTTestTree("BLOCK");
        for (ASTNode statement : statements) {
            root.addChild(statement.toASTTestTree());
        }
        return root;
    }
}
