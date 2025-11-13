package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public abstract class ExpressionNode extends ASTNode {
    public abstract ASTTestTree toASTTestTree();
}
