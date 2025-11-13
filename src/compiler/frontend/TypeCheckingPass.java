package compiler.frontend;

import compiler.frontend.ast.ASTNode;
import compiler.frontend.visitor.TypeCheckingVisitor;
import compiler.infra.*;
import compiler.middle.*;

/**
 * Type checking pass that performs basic semantic validation.
 */
public class TypeCheckingPass implements CompilerPass {
    @Override
    public String name() { return "TypeCheckingPass"; }

    @Override
    public void execute(CompilerContext context) throws Exception {
        Diagnostics diag = context.getDiagnostics();
        SymbolTable table = context.getSymbolTable();
        if (table == null || context.getAst() == null) return;

        Object ast = context.getAst();
        if (ast instanceof ASTNode) {
            TypeCheckingVisitor visitor = new TypeCheckingVisitor(table, diag);
            ((ASTNode) ast).accept(visitor);
        }
    }
}
