package compiler.frontend.ast;

import compiler.frontend.ASTTestTree;

public abstract class ExpressionNode extends ASTNode {
    public String type; // The resolved type of this expression
    public abstract ASTTestTree toASTTestTree();
}
