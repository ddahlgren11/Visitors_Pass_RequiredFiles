package compiler.cli;

import compiler.frontend.FrontEndPass;
import compiler.frontend.SymbolTableBuilderPass;
import compiler.frontend.TypeCheckingPass;
import compiler.infra.CompilerContext;
import compiler.infra.CompilerOrchestrator;
import compiler.infra.CompilerPass;
import compiler.infra.Diagnostics;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <source-file>");
            return;
        }

        String sourceFile = args[0];

        try (InputStream inputStream = new FileInputStream(sourceFile)) {
            // Create the compiler context and set the input stream
            CompilerContext context = new CompilerContext();
            context.setInputStream(inputStream);

            // Create the compiler pass orchestrator and add passes
            CompilerOrchestrator orchestrator = new CompilerOrchestrator();

            // Example passes (to be implemented by students) Note: you may choose to separate
            // your passes differently. E.g. you may make a LexerParserPass class because that makes more
            // sense to you.
            // orchestrator.addPass(new LexicalAnalysisPass());
            // orchestrator.addPass(new SyntaxAnalysisPass());
            // orchestrator.addPass(new TypeCheckingPass());
            // orchestrator.addPass(new CodeGenerationPass());
            orchestrator.addPass(new FrontEndPass());
            orchestrator.addPass(new SymbolTableBuilderPass());
            orchestrator.addPass(new TypeCheckingPass());

            // Run the compiler passes
            orchestrator.runPasses(context);

            // After running passes, print diagnostics summary or success
            Diagnostics diag = context.getDiagnostics();
            System.out.println();
            if (diag.hasErrors()) {
                System.err.println("Compilation finished with errors: " + diag.getSummary());
                for (String e : diag.getErrors()) {
                    System.err.println(e);
                }
                // Exit with non-zero to indicate failure (when run outside IDE)
                System.exit(1);
            } else {
                System.out.println("Compilation successful. " + diag.getSummary());
                if (diag.hasWarnings()) {
                    for (String w : diag.getWarnings()) System.out.println(w);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}