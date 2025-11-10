package compiler.middle.tac;

import compiler.frontend.BinaryOpNode;
import compiler.middle.ast.*;

public interface TACVisitor<T> {
    T visit(LiteralNode node);
    T visit(IdentifierNode node);
    T visit(BinaryOpNode node);
    T visit(AssignmentNode node);
    T visit(VarDeclNode node);
    T visit(FunctionDeclNode node);
    // Add others later when needed
}