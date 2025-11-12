package compiler.frontend.visitor;

import compiler.frontend.ast.*;

/**
 * Example type checking visitor stub. Real implementation should track types
 * and report errors to Diagnostics.
 */
public class TypeCheckingVisitor implements ASTVisitor {

    public void visit(BinaryExprNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        // In a real checker, check operand types and set expression type
    }

    public void visit(AssignmentNode node) { //change
        node.getTarget().accept(this);
        node.getValue().accept(this);
    }

    public void visit(VarDeclNode node) {
        if (node.getInit() != null) node.getInit().accept(this);
    }

    public void visit(LiteralNode node) { }
    public void visit(IdentifierNode node) { }

    public void visit(BlockNode node) {
        for (compiler.frontend.ASTNode s : node.getStatements()) s.accept(this);
    }

    public void visit(FunctionDeclNode node) {
        if (node.getBody() != null) node.getBody().accept(this);
    }

    public void visit(ReturnNode node) {
        if (node.getExpr() != null) node.getExpr().accept(this);
    }

    public void visit(IfNode node) {
        node.getCond().accept(this);
        if (node.getThenBlock() != null) node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) node.getElseBlock().accept(this);
    }

    public void visit(ForNode node) {
        if (node.getInit() != null) node.getInit().accept(this);
        if (node.getCond() != null) node.getCond().accept(this);
        if (node.getUpdate() != null) node.getUpdate().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);
    }

    public void visit(WhileNode node) {
        if (node.getCond() != null) node.getCond().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);
    }
}
