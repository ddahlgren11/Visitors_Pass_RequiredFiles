
import compiler.frontend.ast.*;

/**
 * Example type checking visitor stub. Real implementation should track types
 * and report errors to Diagnostics.
 */
public class TypeCheckingVisitor implements ASTVisitor<Void> {

    public Void visit(BinaryExprNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        // In a real checker, check operand types and set expression type
        return null;
    }

    public Void visit(AssignmentNode node) { //change
        node.getTarget().accept(this);
        node.getValue().accept(this);
        return null;
    }

    public Void visit(VarDeclNode node) {
        if (node.getInit() != null) node.getInit().accept(this);
        return null;
    }

    public Void visit(LiteralNode node) { return null; }
    public Void visit(IdentifierNode node) { return null; }

    public Void visit(BlockNode node) {
        for (ASTNode s : node.getStatements()) s.accept(this);
        return null;
    }

    public Void visit(FunctionDeclNode node) {
        if (node.getBody() != null) node.getBody().accept(this);
        return null;
    }

    public Void visit(ReturnNode node) {
        if (node.getExpr() != null) node.getExpr().accept(this);
        return null;
    }

    public Void visit(IfNode node) {
        node.getCond().accept(this);
        if (node.getThenBlock() != null) node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) node.getElseBlock().accept(this);
        return null;
    }

    public Void visit(ForNode node) {
        if (node.getInit() != null) node.getInit().accept(this);
        if (node.getCond() != null) node.getCond().accept(this);
        if (node.getUpdate() != null) node.getUpdate().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);
        return null;
    }

    public Void visit(WhileNode node) {
        if (node.getCond() != null) node.getCond().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);
        return null;
    }
}