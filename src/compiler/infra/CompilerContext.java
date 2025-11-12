package compiler.infra;

import java.io.InputStream;
import compiler.frontend.ASTNodeBase;
import compiler.middle.SymbolTable;
import compiler.middle.SymbolTableImpl;
import compiler.middle.tac.TACInstruction;

import java.util.List;

/**
 * Shared compiler state that passes can read or modify.
 * This context object acts as the central hub for all compiler data.
 */
public class CompilerContext {
    private InputStream inputStream;
    private final Diagnostics diagnostics = new Diagnostics();
    // Hold a reference to the frontend AST (may be null if parsing hasn't run)
    private ASTNodeBase ast;
    // optional symbol table built by semantic passes
    private SymbolTable symbolTable;

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
    public void setAst(ASTNodeBase ast) {
        this.ast = ast;
    }

    /**
     * Retrieve the AST (may be null if not yet produced).
     */
    public ASTNodeBase getAst() {
        return ast;
    }

    /**
     * Store the symbol table produced by the semantic analysis pass.
     */
    public void setSymbolTable(SymbolTable table) {
        this.symbolTable = table;
    }

    /**
     * Retrieve the symbol table (may be null if not yet built).
     */
    public SymbolTable getSymbolTable() {
        // lazily create a default one to avoid null checks elsewhere
        if (symbolTable == null) {
            symbolTable = new SymbolTableImpl();
        }
        return symbolTable;
    }

    private List<TACInstruction> tac;

    public void setTac(List<TACInstruction> tac) {
        this.tac = tac;
    }

    public List<TACInstruction> getTac() {
        return tac;
    }
}