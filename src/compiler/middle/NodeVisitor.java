package compiler.middle;

import compiler.middle.ast.AssignmentNode;
import compiler.middle.ast.FunctionDeclNode;
import compiler.middle.ast.VarDeclNode;

/**
 * The Visitor interface for the AST.
 * Declares a visit method for every concrete AST node type.
 */
public interface NodeVisitor {
    void visit(VarDeclNode node);
    void visit(AssignmentNode node);
    void visit(FunctionDeclNode node);
    // Add visit methods for other nodes (e.g., IfNode, BlockNode, CallNode, etc.)
}
