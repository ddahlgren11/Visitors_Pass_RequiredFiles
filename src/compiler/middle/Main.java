package compiler.middle;

import compiler.frontend.ast.*;
import compiler.frontend.visitor.SymbolTableBuilderVisitor;
import compiler.infra.Diagnostics;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * Demonstrates the SymbolTable, AST, and Visitor pattern integration.
 * This class builds a small example AST and traverses it with the SymbolTableBuilder.
 *
 * Example Program being analyzed:
 * var global_x = 1;
 * func add(a, b) {
 * var result = a + b;
 * result = global_x; // Uses global_x (resolved via scope lookup)
 * undeclared_y = 5; // Semantic error: undeclared_y
 * }
 * var global_y = 2;
 */
public class Main {
    public static void main(String[] args) {
        
        // --- 1. Build a small Abstract Syntax Tree (AST) ---
        ExpressionNode literal1 = new LiteralNode("1");
        ExpressionNode literal5 = new LiteralNode("5");

        // Global Variable Declaration: var global_x = 1;
        VarDeclNode globalXDecl = new VarDeclNode("int", "global_x", literal1);

        // Function Body Statements (Inner scope)
        // Inside function: var result = ...
        VarDeclNode resultDecl = new VarDeclNode("int", "result", null); // Simplified, no RHS
        
        // Inside function: result = global_x; (Variable 'result' is local, 'global_x' is outer scope)
        AssignmentNode assignGlobalX = new AssignmentNode(new IdentifierNode("result"), new IdentifierNode("global_x"));

        // Inside function: undeclared_y = 5; (This will cause a semantic error)
        AssignmentNode undeclaredAssign = new AssignmentNode(new IdentifierNode("undeclared_y"), literal5);

        // The function body block (a custom anonymous node for demonstration purposes)
        BlockNode functionBodyBlock = new BlockNode(Arrays.asList(resultDecl, assignGlobalX, undeclaredAssign));

        // Function Declaration: func add(a, b) { ... }
        FunctionDeclNode addFunction = new FunctionDeclNode(
            "int",
            "add",
            Arrays.asList(new VarDeclNode("int", "a", null), new VarDeclNode("int", "b", null)), // Parameters
            functionBodyBlock
        );

        // Global Variable Declaration: var global_y = 2;
        VarDeclNode globalYDecl = new VarDeclNode("int", "global_y", new LiteralNode("2"));


        // The main sequence of statements (Root of the AST)
        BlockNode programAST = new BlockNode(Arrays.asList(
            globalXDecl,
            addFunction,
            globalYDecl
        ));

        // --- 2. Initialize Symbol Table and Visitor ---
        SymbolTable table = new SymbolTableImpl();
        Diagnostics diag = new Diagnostics();
        SymbolTableBuilderVisitor builder = new SymbolTableBuilderVisitor(table, diag);

        System.out.println("\n--- Starting Semantic Pass (Symbol Table Building) ---\n");
        
        // --- 3. Traverse the AST ---
        programAST.accept(builder);

        System.out.println("\n--- Semantic Pass Complete ---\n");
        
        // --- 4. Print Final Scope Information (Should be back at Global Scope) ---
        
        System.out.println("--- Final Scope Snapshot ---");
        List<ScopeInfo> finalInfo = table.getScopeInfo();
        if (finalInfo.isEmpty()) {
            System.out.println("All scopes closed. Symbol table stack is empty.");
        } else {
            System.out.println(finalInfo.size() + " scope(s) remaining.");
        }
    }
}
