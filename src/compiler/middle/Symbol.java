package compiler.middle;

/**
 * A class representing a symbol in the symbol table.
 */
public class Symbol {
    private final String name;
    private final Kind kind;
    private final Object declaration;

    public Symbol(String name, Kind kind, Object declaration) {
        this.name = name;
        this.kind = kind;
        this.declaration = declaration;
    }

    public String name() { return name; }
    public Kind kind() { return kind; }
    public Object declaration() { return declaration; }

    @Override
    public String toString() {
        return String.format("Symbol[name=%s, kind=%s]", name, kind);
    }
}
