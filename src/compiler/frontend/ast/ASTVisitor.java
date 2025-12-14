package compiler.frontend.ast;

/**
 * Generic visitor interface for the frontend AST.
 * @param <T> visitor result type
 */
public interface ASTVisitor {
    void visitBinaryExprNode(BinaryExprNode node);
    void visitBinaryOpNode(BinaryOpNode node);
    void visitAssignmentNode(AssignmentNode node);
    void visitVarDeclNode(VarDeclNode node);
    void visitLiteralNode(LiteralNode node);
    void visitIdentifierNode(IdentifierNode node);
    void visitBlockNode(BlockNode node);
    void visitFunctionDeclNode(FunctionDeclNode node);
    void visitReturnNode(ReturnNode node);
    void visitIfNode(IfNode node);
    void visitForNode(ForNode node);
    void visitWhileNode(WhileNode node);
    void visitUnaryOpNode(UnaryOpNode node);
    void visitEmptyNode(EmptyNode emptyNode);
}
