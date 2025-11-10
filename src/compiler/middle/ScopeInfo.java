package compiler.middle;

import java.util.List;

/**
 * A class representing a snapshot of a single scope level,
 * primarily used for debugging and testing the symbol table structure.
 */
public class ScopeInfo {
    private final int level;
    private final List<Symbol> symbols;

    public ScopeInfo(int level, List<Symbol> symbols) {
        this.level = level;
        this.symbols = symbols;
    }

    public int level() { return level; }
    public List<Symbol> symbols() { return symbols; }

    public int size() { return symbols.size(); }

    @Override
    public String toString() {
        return String.format("ScopeInfo[level=%d, symbols=%s]", level, symbols);
    }
}
