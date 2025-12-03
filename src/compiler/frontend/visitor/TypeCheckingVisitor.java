package compiler.frontend.visitor;

import compiler.frontend.ast.*;
import compiler.infra.Diagnostics;
import compiler.middle.SymbolTable;

public class TypeCheckingVisitor implements ASTVisitor {
    private final SymbolTable table;
    private final Diagnostics diag;

    public TypeCheckingVisitor(SymbolTable table, Diagnostics diag) {
        this.table = table;
        this.diag = diag;
    }

    @Override
    public void visitAssignmentNode(AssignmentNode node) {
        node.getTarget().accept(this);
        node.getExpression().accept(this);
    }

    @Override
    public void visitBinaryExprNode(BinaryExprNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
    }

    @Override
    public void visitBinaryOpNode(BinaryOpNode node) {
        node.left.accept(this);
        node.right.accept(this);
    }

    @Override
    public void visitBlockNode(BlockNode node) {
        for (ASTNode statement : node.getStatements()) {
            statement.accept(this);
        }
    }

    @Override
    public void visitForNode(ForNode node) {
        if (node.getInit() != null) {
            node.getInit().accept(this);
        }
        if (node.getCond() != null) {
            node.getCond().accept(this);
        }
        if (node.getUpdate() != null) {
            node.getUpdate().accept(this);
        }
        node.getBody().accept(this);
    }

    @Override
    public void visitFunctionDeclNode(FunctionDeclNode node) {
        for (VarDeclNode param : node.getParams()) {
            param.accept(this);
        }
        node.getBody().accept(this);
    }

    @Override
    public void visitIdentifierNode(IdentifierNode node) {
        // Do nothing
    }

    @Override
    public void visitIfNode(IfNode node) {
        node.getCond().accept(this);
        node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) {
            node.getElseBlock().accept(this);
        }
    }

    @Override
    public void visitLiteralNode(LiteralNode node) {
        // Do nothing
    }

    @Override
    public void visitReturnNode(ReturnNode node) {
        if (node.getExpr() != null) {
            node.getExpr().accept(this);
        }
    }

    @Override
    public void visitVarDeclNode(VarDeclNode node) {
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
        }
    }

    @Override
    public void visitWhileNode(WhileNode node) {
        node.getCond().accept(this);
        node.getBody().accept(this);
    }

    @Override
    public void visitUnaryOpNode(UnaryOpNode node) {
        node.expr.accept(this);
    }
}
