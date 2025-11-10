package compiler.frontend.visitor;

import compiler.frontend.ast.*;

/**
 * Example visitor that would build a symbol table. This is a stub showing
 * how a pass should implement behavior separate from the AST data classes.
 */
public class SymbolTableBuilderVisitor implements ASTVisitor<Void> {

    @Override
    public Void visit(BinaryExprNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        node.getTarget().accept(this);
        node.getValue().accept(this);
        return null;
    }

    @Override
    public Void visit(VarDeclNode node) {
        if (node.getInit() != null) node.getInit().accept(this);
        // would declare symbol here
        return null;
    }

    @Override
    public Void visit(LiteralNode node) { return null; }

    @Override
    public Void visit(IdentifierNode node) { return null; }

    @Override
    public Void visit(BlockNode node) {
        for (ASTNode s : node.getStatements()) {
            s.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FunctionDeclNode node) {
        // declare function, then visit body
        if (node.getBody() != null) node.getBody().accept(this);
        return null;
    }

    @Override
    public Void visit(ReturnNode node) {
        if (node.getExpr() != null) node.getExpr().accept(this);
        return null;
    }

    @Override
    public Void visit(IfNode node) {
        node.getCond().accept(this);
        if (node.getThenBlock() != null) node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) node.getElseBlock().accept(this);
        return null;
    }

    @Override
    public Void visit(ForNode node) {
        if (node.getInit() != null) node.getInit().accept(this);
        if (node.getCond() != null) node.getCond().accept(this);
        if (node.getUpdate() != null) node.getUpdate().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);
        return null;
    }

    @Override
    public Void visit(WhileNode node) {
        if (node.getCond() != null) node.getCond().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);
        return null;
    }
}
