package compiler.frontend;

import compiler.frontend.ast.*;
import compiler.frontend.visitor.SymbolTableBuilderVisitor;
import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.infra.Diagnostics;
import compiler.middle.SymbolTable;
import compiler.middle.SymbolTableImpl;
import compiler.middle.Symbol;
import compiler.middle.Kind;

public class SymbolTableBuilderPass implements CompilerPass {
    @Override
    public String name() { return "SymbolTableBuilderPass"; }

    @Override
    public void execute(CompilerContext context) throws Exception {
        Diagnostics diag = context.getDiagnostics();
        diag.log("=== Starting " + name() + " ===");

        SymbolTable table = new SymbolTableImpl();
        table.enterScope(); // global scope
        context.setSymbolTable(table);
        diag.log("Created symbol table with global scope.");

        Object ast = context.getAst();
        if (ast == null) {
            diag.log("No AST found — symbol table will be empty.");
            return;
        }

        if (ast instanceof ASTNode) {
            diag.log("Building symbol table from visitor-style frontend.ast AST...");
            SymbolTableBuilderVisitor visitor = new SymbolTableBuilderVisitor(table, diag);
            ((ASTNode) ast).accept(visitor);
            diag.log("Symbol table complete: " + table.getScopeInfo().size() + " scope(s).");
        } else {
            diag.log("Unknown AST type — skipping symbol table construction.");
        }
    }
}
