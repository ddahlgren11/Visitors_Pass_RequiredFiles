package compiler.infra;

import java.io.InputStream;

/**
 * Shared compiler state that passes can read or modify.
 * This context object acts as the central hub for all compiler data.
 */
public class CompilerContext {
    private InputStream inputStream;
    private final Diagnostics diagnostics = new Diagnostics();
    // Hold a reference to the frontend AST (may be null if parsing hasn't run)
    private Object ast;
    // optional symbol table built by semantic passes
    private Object symbolTable;

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Returns the Diagnostics collector for the current compilation.
     */
    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    /**
     * Store the AST produced by the front-end.
     */
    public void setAst(Object ast) {
        this.ast = ast;
    }

    /**
     * Retrieve the AST (may be null if not yet produced).
     */
    public Object getAst() {
        return ast;
    }

    /**
     * Store the symbol table produced by the semantic analysis pass.
     */
    public void setSymbolTable(Object table) {
        this.symbolTable = table;
    }

    /**
     * Retrieve the symbol table (may be null if not yet built).
     */
    public Object getSymbolTable() {
        // lazily create a default one to avoid null checks elsewhere
        /*
        if (symbolTable == null) {
            symbolTable = new SymbolTableImpl();
        }
        */
        // Note: Can't instantiate SymbolTableImpl here without dependency.
        // Callers must handle null or initialization.
        return symbolTable;
    }

    // Add fields like tokens, ASTNode, IR, symbol tables, etc. Whatever you need to make your compiler work
}
