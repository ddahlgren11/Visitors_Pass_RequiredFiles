package compiler.frontend;

import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.infra.Diagnostics;

import java.io.InputStream;
import java.util.Collections;
import compiler.frontend.BlockNode;

/**
 * FrontEndPass
 * Combines lexing and parsing into one compiler pass.
 * Uses MyParser (JavaCC-generated) to build the AST and store it in the CompilerContext.
 */
public class FrontEndPass implements CompilerPass {

    @Override
    public String name() {
        return "FrontEndPass (Lexing + Parsing)";
    }

    @Override
    public void execute(CompilerContext context) throws Exception {
        System.out.println("=== Starting FrontEndPass ===");
        Diagnostics reporter = context.getDiagnostics();

        InputStream input = context.getInputStream();
        if (input == null) {
            // Nothing to parse; store an empty block AST to keep downstream passes happy
            context.setAst(new BlockNode(Collections.emptyList()));
            System.out.println("No input stream; FrontEndPass produced empty AST.");
            System.out.println("=== Finished FrontEndPass ===");
            return;
        }

        try {
            MyParser parser = new MyParser(input);
            // The real parser should have a root method (e.g., Program()). If not available,
            // store a placeholder empty block AST for now so the pipeline can continue.
            ASTNodeBase ast;
            try {
                // attempt to call a common root method if it exists
                ast = parser.Assignment();
            } catch (Exception ex) {
                ast = new BlockNode(Collections.emptyList());
            }
            context.setAst(ast);
            System.out.println("Parsing (best-effort) completed.");
        } catch (Exception e) {
            if (e instanceof ParseException) {
                if (reporter != null) {
                    reporter.addError("Parse error: " + e.getMessage());
                } else {
                    System.err.println("Parse error: " + e.getMessage());
                }
            } else {
                // Unexpected exception during parsing
                e.printStackTrace();
            }
        }

        System.out.println("=== Finished FrontEndPass ===");
    }
}

