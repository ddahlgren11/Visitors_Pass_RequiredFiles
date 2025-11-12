package compiler.frontend.visitor;

import compiler.frontend.ast.*;

/**
 * Example visitor that would build a symbol table. This is a stub showing
 * how a pass should implement behavior separate from the AST data classes.
 */
public class SymbolTableBuilderVisitor implements ASTVisitor {

    @Override
    public void visit(BinaryExprNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
    }

    @Override
    public void visit(AssignmentNode node) {
        node.getTarget().accept(this);
        node.getValue().accept(this);
    }

    @Override
    public void visit(VarDeclNode node) {
        if (node.getInit() != null) node.getInit().accept(this);
        // would declare symbol here
    }

    @Override
    public void visit(LiteralNode node) { }

    @Override
    public void visit(IdentifierNode node) { }

    @Override
    public void visit(BlockNode node) {
        for (compiler.frontend.ASTNode s : node.getStatements()) {
            s.accept(this);
        }
    }

    @Override
    public void visit(FunctionDeclNode node) {
        // declare function, then visit body
        if (node.getBody() != null) node.getBody().accept(this);
    }

    @Override
    public void visit(ReturnNode node) {
        if (node.getExpr() != null) node.getExpr().accept(this);
    }

    @Override
    public void visit(IfNode node) {
        node.getCond().accept(this);
        if (node.getThenBlock() != null) node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) node.getElseBlock().accept(this);
    }

    @Override
    public void visit(ForNode node) {
        if (node.getInit() != null) node.getInit().accept(this);
        if (node.getCond() != null) node.getCond().accept(this);
        if (node.getUpdate() != null) node.getUpdate().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);
    }

    @Override
    public void visit(WhileNode node) {
        if (node.getCond() != null) node.getCond().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);
    }
}
