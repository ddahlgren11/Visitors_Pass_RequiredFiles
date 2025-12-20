package compiler.frontend.ast;

/**
 * Generic visitor interface for the frontend AST.
 * @param <T> visitor result type
 */
public interface ASTVisitor<T> {
    T visitBinaryExprNode(BinaryExprNode node);
    T visitBinaryOpNode(BinaryOpNode node);
    T visitAssignmentNode(AssignmentNode node);
    T visitVarDeclNode(VarDeclNode node);
    T visitLiteralNode(LiteralNode node);
    T visitIdentifierNode(IdentifierNode node);
    T visitBlockNode(BlockNode node);
    T visitFunctionDeclNode(FunctionDeclNode node);
    T visitReturnNode(ReturnNode node);
    T visitIfNode(IfNode node);
    T visitForNode(ForNode node);
    T visitWhileNode(WhileNode node);
    T visitUnaryOpNode(UnaryOpNode node);
    T visitEmptyNode(EmptyNode emptyNode);
    T visitClassDeclNode(ClassDeclNode node);
    T visitNewExprNode(NewExprNode node);
    T visitMethodCallNode(MethodCallNode node);
    T visitMemberAccessNode(MemberAccessNode node);
}
