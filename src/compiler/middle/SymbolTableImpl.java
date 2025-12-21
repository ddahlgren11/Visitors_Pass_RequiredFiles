package compiler.middle;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the SymbolTable interface using a stack of HashMaps
 * to manage nested scopes and symbol lookups.
 */
public class SymbolTableImpl implements SymbolTable {

    // Stack of Maps: each Map represents a scope (block/function) and holds Symbols.
    // The top of the stack (end of list) is always the current, innermost scope.
    private final List<Map<String, Symbol>> scopes;
    // Tracks the current nesting level. 0 is the global scope.
    private int currentLevel;

    /**
     * Initializes the symbol table with an empty stack.
     */
    public SymbolTableImpl() {
        this.scopes = new ArrayList<>();
        this.currentLevel = -1; // -1 means no scope entered yet
    }

    /**
     * Pushes a new scope map onto the stack and increments the level.
     */
    @Override
    public void enterScope() {
        scopes.add(new HashMap<>());
        currentLevel++;
    }

    /**
     * Pops the current scope map from the stack and decrements the level.
     * This removes all symbols declared in the exited scope.
     */
    @Override
    public void exitScope() {
        if (scopes.isEmpty()) {
            throw new IllegalStateException("Cannot exit scope: Symbol table stack is empty.");
        }
        scopes.remove(scopes.size() - 1);
        currentLevel--;
    }

    /**
     * Declares a symbol in the current (top-most) scope.
     * Rejects duplicate declarations within the same scope.
     * @param symbol the symbol to declare
     * @return true if successfully declared, false otherwise
     */
    @Override
    public boolean declare(Symbol symbol) {
        if (scopes.isEmpty()) {
            throw new IllegalStateException("Cannot declare: No scope entered.");
        }

        Map<String, Symbol> currentScope = scopes.get(scopes.size() - 1);

        // Check for local duplicate
        if (currentScope.containsKey(symbol.name())) {
            return false;
        }

        currentScope.put(symbol.name(), symbol);
        return true;
    }

    /**
     * Looks up a symbol only in the current (top-most) scope.
     */
    @Override
    public Optional<Symbol> lookupLocal(String name) {
        if (scopes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(scopes.get(scopes.size() - 1).get(name));
    }

    /**
     * Looks up a symbol starting from the current scope and moving outward
     * to enclosing scopes (top-to-bottom on the stack). The first match found
     * shadows any deeper declarations.
     */
    @Override
    public Optional<Symbol> lookup(String name) {
        // Iterate backward through the stack (from innermost to outermost scope)
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Symbol symbol = scopes.get(i).get(name);
            if (symbol != null) {
                return Optional.of(symbol);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a structured view of all current scopes for testing/debugging.
     */
    @Override
    public List<ScopeInfo> getScopeInfo() {
        List<ScopeInfo> info = new ArrayList<>();
        // Iterate through the stack (from global scope 0 to current scope N)
        for (int i = 0; i < scopes.size(); i++) {
            // Convert Map<String, Symbol> values into a List<Symbol>
            List<Symbol> symbolsInScope = scopes.get(i).values().stream()
                    .collect(Collectors.toList());
            
            // Create a ScopeInfo record for this level
            info.add(new ScopeInfo(i, symbolsInScope));
        }
        return info;
    }
}
