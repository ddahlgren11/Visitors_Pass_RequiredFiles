package compiler.frontend.visitor;

import compiler.frontend.ast.*;
import compiler.infra.Diagnostics;
import compiler.middle.SymbolTable;
import compiler.middle.Symbol;
import compiler.middle.Kind;

public class SymbolTableBuilderVisitor implements ASTVisitor<Void> {
    private final SymbolTable table;
    private final Diagnostics diag;

    public SymbolTableBuilderVisitor(SymbolTable table, Diagnostics diag) {
        this.table = table;
        this.diag = diag;
    }

    @Override
    public Void visitAssignmentNode(AssignmentNode node) {
        node.getTarget().accept(this);
        node.getExpression().accept(this);
        return null;
    }

    @Override
    public Void visitBinaryExprNode(BinaryExprNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        return null;
    }

    @Override
    public Void visitBinaryOpNode(BinaryOpNode node) {
        node.left.accept(this);
        node.right.accept(this);
        return null;
    }

    @Override
    public Void visitBlockNode(BlockNode node) {
        table.enterScope();
        for (ASTNode statement : node.getStatements()) {
            statement.accept(this);
        }
        table.exitScope();
        return null;
    }

    @Override
    public Void visitForNode(ForNode node) {
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
        return null;
    }

    @Override
    public Void visitFunctionDeclNode(FunctionDeclNode node) {
        Symbol sym = new Symbol(node.getName(), Kind.FUNCTION, node);
        if (!table.declare(sym)) {
            diag.addError("Duplicate declaration: " + node.getName());
        }
        table.enterScope();
        for (VarDeclNode param : node.getParams()) {
             // Params are like variables in local scope
             Symbol paramSym = new Symbol(param.name, Kind.PARAMETER, param);
             if (!table.declare(paramSym)) {
                 diag.addError("Duplicate parameter: " + param.name);
             }
        }
        node.getBody().accept(this);
        table.exitScope();
        return null;
    }

    @Override
    public Void visitIdentifierNode(IdentifierNode node) {
        if (node.getName().equals("this")) return null;
        if (!table.lookup(node.getName()).isPresent()) {
            diag.addError("Use of undeclared variable: " + node.getName());
        }
        return null;
    }

    @Override
    public Void visitIfNode(IfNode node) {
        node.getCond().accept(this);
        node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) {
            node.getElseBlock().accept(this);
        }
        return null;
    }

    @Override
    public Void visitLiteralNode(LiteralNode node) {
        return null;
    }

    @Override
    public Void visitReturnNode(ReturnNode node) {
        if (node.getExpr() != null) {
            node.getExpr().accept(this);
        }
        return null;
    }

    @Override
    public Void visitVarDeclNode(VarDeclNode node) {
        Symbol sym = new Symbol(node.getName(), Kind.VARIABLE, node);
        if (!table.declare(sym)) {
            diag.addError("Duplicate declaration: " + node.getName());
        }
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
        }
        return null;
    }

    @Override
    public Void visitWhileNode(WhileNode node) {
        node.getCond().accept(this);
        node.getBody().accept(this);
        return null;
    }

    @Override
    public Void visitUnaryOpNode(UnaryOpNode node) {
        node.expr.accept(this);
        return null;
    }

    @Override
    public Void visitEmptyNode(EmptyNode emptyNode) {
        return null;
    }

    @Override
    public Void visitClassDeclNode(ClassDeclNode node) {
        Symbol sym = new Symbol(node.className, Kind.TYPE, node);
        if (!table.declare(sym)) {
            diag.addError("Duplicate declaration: " + node.className);
        }
        table.enterScope();
        for (VarDeclNode field : node.fields) {
            field.accept(this);
        }
        for (FunctionDeclNode method : node.methods) {
            method.accept(this);
        }
        table.exitScope();
        return null;
    }

    @Override
    public Void visitNewExprNode(NewExprNode node) {
        for (ASTNode arg : node.args) {
            arg.accept(this);
        }
        return null;
    }

    @Override
    public Void visitMethodCallNode(MethodCallNode node) {
        if (node.object != null) {
            node.object.accept(this);
        }
        for (ASTNode arg : node.args) {
            arg.accept(this);
        }
        return null;
    }

    @Override
    public Void visitMemberAccessNode(MemberAccessNode node) {
        node.object.accept(this);
        return null;
    }
}
