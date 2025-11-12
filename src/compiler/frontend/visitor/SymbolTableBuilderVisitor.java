package compiler.frontend.visitor;

import compiler.frontend.ast.*;

/**
 * Example visitor that would build a symbol table. This is a stub showing
 * how a pass should implement behavior separate from the AST data classes.
 */
public class SymbolTableBuilderVisitor implements ASTVisitor {

    @Override
    public void visitBinaryExprNode(BinaryExprNode node) {
        node.left.accept(this);
        node.right.accept(this);
    }

    @Override
    public void visitAssignmentNode(AssignmentNode node) {
        node.target.accept(this);
        node.expression.accept(this);
    }

    @Override
    public void visitVarDeclNode(VarDeclNode node) {
        if (node.initializer != null) node.initializer.accept(this);
        // would declare symbol here
    }

    @Override
    public void visitLiteralNode(LiteralNode node) { }

    @Override
    public void visitIdentifierNode(IdentifierNode node) { }

    @Override
    public void visitBlockNode(BlockNode node) {
        for (ASTNode s : node.statements) {
            s.accept(this);
        }
    }

    @Override
    public void visitFunctionDeclNode(FunctionDeclNode node) {
        // declare function, then visit body
        if (node.body != null) node.body.accept(this);
    }

    @Override
    public void visitReturnNode(ReturnNode node) {
        if (node.expr != null) node.expr.accept(this);
    }

    @Override
    public void visitIfNode(IfNode node) {
        node.cond.accept(this);
        if (node.thenBlock != null) node.thenBlock.accept(this);
        if (node.elseBlock != null) node.elseBlock.accept(this);
    }

    @Override
    public void visitForNode(ForNode node) {
        if (node.init != null) node.init.accept(this);
        if (node.cond != null) node.cond.accept(this);
        if (node.update != null) node.update.accept(this);
        if (node.body != null) node.body.accept(this);
    }

    @Override
    public void visitWhileNode(WhileNode node) {
        if (node.cond != null) node.cond.accept(this);
        if (node.body != null) node.body.accept(this);
    }

    @Override
    public void visitBinaryOpNode(BinaryOpNode node) {
        // TODO Auto-generated method stub
    }
}
