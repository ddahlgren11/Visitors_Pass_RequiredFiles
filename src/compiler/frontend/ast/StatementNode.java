package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public abstract class StatementNode extends ASTNode {
    public abstract ASTTestTree toASTTestTree();
}
