package compiler.middle;

import java.util.List;
import java.util.Optional;

/**
 * Defines the interface for a Symbol Table supporting nested scope management.
 */
public interface SymbolTable {

    /**
     * Declare a new symbol in the current scope.
     * @param symbol the symbol to declare
     * @return true if successfully declared, false if duplicate in this scope
     */
    boolean declare(Symbol symbol);

    /**
     * Lookup a symbol in the current and enclosing scopes (shadowing applies).
     * @param name the name to lookup
     * @return an Optional containing the symbol if found, or empty if not found
     */
    Optional<Symbol> lookup(String name);

    /**
     * Lookup a symbol in the current scope only.
     * @param name the name to lookup
     * @return an Optional containing the symbol if found, or empty if not found
     */
    Optional<Symbol> lookupLocal(String name);

    /**
     * Enter a new scope (e.g., function, block).
     */
    void enterScope();

    /**
     * Exit the current scope, discarding its symbols.
     */
    void exitScope();

    /**
     * Return a structured view of current scopes (for testing/debugging).
     */
    List<ScopeInfo> getScopeInfo();
}
