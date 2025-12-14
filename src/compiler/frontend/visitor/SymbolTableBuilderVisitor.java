package compiler.frontend.visitor;

import compiler.frontend.ast.*;
import compiler.infra.Diagnostics;
import compiler.middle.SymbolTable;
import compiler.middle.Symbol;
import compiler.middle.Kind;

public class SymbolTableBuilderVisitor implements ASTVisitor {
    private final SymbolTable table;
    private final Diagnostics diag;

    public SymbolTableBuilderVisitor(SymbolTable table, Diagnostics diag) {
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
        table.enterScope();
        for (ASTNode statement : node.getStatements()) {
            statement.accept(this);
        }
        table.exitScope();
    }

    @Override
    public void visitForNode(ForNode node) {
        table.enterScope();
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
        table.exitScope();
    }

    @Override
    public void visitFunctionDeclNode(FunctionDeclNode node) {
        Symbol sym = new Symbol(node.getName(), Kind.FUNCTION, node);
        if (!table.declare(sym)) {
            diag.addError("Duplicate declaration: " + node.getName());
        }
        table.enterScope();
        for (VarDeclNode param : node.getParams()) {
            param.accept(this);
        }
        node.getBody().accept(this);
        table.exitScope();
    }

    @Override
    public void visitIdentifierNode(IdentifierNode node) {
        if (!table.lookup(node.getName()).isPresent()) {
            diag.addError("Use of undeclared variable: " + node.getName());
        }
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
        Symbol sym = new Symbol(node.getName(), Kind.VARIABLE, node);
        if (!table.declare(sym)) {
            diag.addError("Duplicate declaration: " + node.getName());
        }
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

    @Override
    public void visitEmptyNode(EmptyNode emptyNode) {
        // Do nothing
    }
}