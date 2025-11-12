package compiler.frontend.ast;

/**
 * Generic visitor interface for the frontend AST.
 */
public interface ASTVisitor {
    void visit(BinaryExprNode node);
    void visit(AssignmentNode node);
    void visit(VarDeclNode node);
    void visit(LiteralNode node);
    void visit(IdentifierNode node);
    void visit(BlockNode node);
    void visit(FunctionDeclNode node);
    void visit(ReturnNode node);
    void visit(IfNode node);
    void visit(ForNode node);
    void visit(WhileNode node);
}
