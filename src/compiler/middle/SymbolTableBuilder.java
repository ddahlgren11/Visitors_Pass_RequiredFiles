package compiler.middle;

import compiler.middle.ast.AssignmentNode;
import compiler.middle.ast.FunctionDeclNode;
import compiler.middle.ast.VarDeclNode;

import java.util.List;

/**
 * A concrete implementation of NodeVisitor that performs a semantic pass
 * to build the symbol table and check for basic semantic errors (e.g., undeclared variables).
 */
public class SymbolTableBuilder implements NodeVisitor {

    private final SymbolTable table;

    /**
     * Constructs the builder with a reference to the symbol table instance.
     */
    public SymbolTableBuilder(SymbolTable table) {
        this.table = table;
        // Enter the global scope when the builder is initialized
        table.enterScope(); 
        System.out.println("Entered global scope.");
    }

    /**
     * Handles Variable Declaration nodes: `var x = value;`
     * Declares the variable in the current scope.
     */
    @Override
    public void visit(VarDeclNode node) {
        String varName = node.getVarName();
        Symbol symbol = new Symbol(varName, Kind.VARIABLE, node);

        if (!table.declare(symbol)) {
            System.err.printf("Semantic Error: Variable '%s' already declared in this scope.%n", varName);
        } else {
            System.out.printf("Declared VARIABLE '%s' in level %d%n", varName, table.getScopeInfo().size() - 1);
        }

        // Recursively visit child nodes (e.g., the right-hand side expression)
        // node.getValue().accept(this);
    }

    /**
     * Handles Assignment nodes: `x = 10;`
     * Looks up the variable to ensure it has been declared in any visible scope.
     */
    @Override
    public void visit(AssignmentNode node) {
        String varName = node.getVarName();

        if (table.lookup(varName).isEmpty()) {
            System.err.printf("Semantic Error: Variable '%s' used but not declared.%n", varName);
        } else {
            System.out.printf("Resolved use of variable '%s'.%n", varName);
        }

        // Recursively visit child nodes (e.g., the right-hand side expression)
        // node.getValue().accept(this);
    }

    /**
     * Handles Function Declaration nodes: `func name(param1, param2) { ... }`
     * 1. Declares the function name in the *enclosing* scope.
     * 2. Enters a new scope for the function body.
     * 3. Declares all parameters in the new scope.
     * 4. Visits the function body.
     * 5. Exits the function's scope.
     */
    @Override
    public void visit(FunctionDeclNode node) {
        String funcName = node.getFunctionName();
        List<String> params = node.getParameters();

        // 1. Declare the function itself (in the outer scope)
        Symbol funcSymbol = new Symbol(funcName, Kind.FUNCTION, node);
        if (!table.declare(funcSymbol)) {
             System.err.printf("Semantic Error: Function '%s' already declared in this scope.%n", funcName);
        } else {
            System.out.printf("Declared FUNCTION '%s' in level %d%n", funcName, table.getScopeInfo().size() - 1);
        }

        // 2. Enter a new scope for the function's parameters and body
        table.enterScope();
        System.out.printf("Entered function scope for '%s'.%n", funcName);

        // 3. Declare parameters in the new scope
        for (String paramName : params) {
            Symbol paramSymbol = new Symbol(paramName, Kind.PARAMETER, null);
            if (!table.declare(paramSymbol)) {
                System.err.printf("Semantic Error: Parameter '%s' already declared in this function's scope.%n", paramName);
            } else {
                System.out.printf("\tDeclared PARAMETER '%s' in function scope.%n", paramName);
            }
        }

        // 4. Visit the function body
        node.getBody().accept(this);
        System.out.println("  [Visitor: End of function body statements]");


        // 5. Exit the function's scope
        table.exitScope();
        System.out.printf("Exited function scope for '%s'.%n", funcName);
    }

    /**
     * Returns the symbol table for inspection/testing.
     */
    public SymbolTable getSymbolTable() {
        return table;
    }
}
