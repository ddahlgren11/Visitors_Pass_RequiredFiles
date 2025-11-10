package compiler.middle;

import compiler.middle.ast.AssignmentNode;
import compiler.middle.ast.FunctionDeclNode;
import compiler.middle.ast.VarDeclNode;
import compiler.middle.ast.LiteralNode;
import compiler.middle.ast.IdentifierNode;

import java.util.List;
import java.util.Arrays;

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
        ASTNodeBase literal1 = new LiteralNode(1);
        ASTNodeBase literal5 = new LiteralNode(5);

        // Global Variable Declaration: var global_x = 1;
        VarDeclNode globalXDecl = new VarDeclNode("global_x", literal1);

        // Function Body Statements (Inner scope)
        // Inside function: var result = ...
        VarDeclNode resultDecl = new VarDeclNode("result", null); // Simplified, no RHS
        
        // Inside function: result = global_x; (Variable 'result' is local, 'global_x' is outer scope)
        AssignmentNode assignGlobalX = new AssignmentNode("result", new IdentifierNode("global_x"));

        // Inside function: undeclared_y = 5; (This will cause a semantic error)
        AssignmentNode undeclaredAssign = new AssignmentNode("undeclared_y", literal5);

        // The function body block (a custom anonymous node for demonstration purposes)
        ASTNodeBase functionBodyBlock = new ASTNodeBase() { 
            @Override 
            public void accept(NodeVisitor visitor) {
                System.out.println("  [Visitor: Visiting function body statements...]");
                // These statements are visited within the function's scope (Level 1)
                resultDecl.accept(visitor);      // Declare 'result'
                assignGlobalX.accept(visitor);   // Lookup 'global_x' (finds it in Level 0)
                undeclaredAssign.accept(visitor); // Lookup 'undeclared_y' (fails)
            }
        };

        // Function Declaration: func add(a, b) { ... }
        FunctionDeclNode addFunction = new FunctionDeclNode(
            "add", 
            Arrays.asList("a", "b"), // Parameters
            functionBodyBlock
        );

        // Global Variable Declaration: var global_y = 2;
        VarDeclNode globalYDecl = new VarDeclNode("global_y", new LiteralNode(2));


        // The main sequence of statements (Root of the AST)
        List<ASTNodeBase> programAST = Arrays.asList(
            globalXDecl,
            addFunction,
            globalYDecl
        );

        // --- 2. Initialize Symbol Table and Visitor ---
        SymbolTable table = new SymbolTableImpl();
        // The builder enters the initial scope (Level 0) upon construction
        SymbolTableBuilder builder = new SymbolTableBuilder(table);

        System.out.println("\n--- Starting Semantic Pass (Symbol Table Building) ---\n");
        
        // --- 3. Traverse the AST ---
        for (ASTNodeBase node : programAST) {
            node.accept(builder);
        }

        System.out.println("\n--- Semantic Pass Complete ---\n");
        
        // --- 4. Print Final Scope Information (Should be back at Global Scope) ---
        table.exitScope(); // Exit the final global scope
        
        System.out.println("--- Final Scope Snapshot (Table is empty) ---");
        List<ScopeInfo> finalInfo = table.getScopeInfo();
        if (finalInfo.isEmpty()) {
            System.out.println("All scopes closed. Symbol table stack is empty.");
        }
    }
}
