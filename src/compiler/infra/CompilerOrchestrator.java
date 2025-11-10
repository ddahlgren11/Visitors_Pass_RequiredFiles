package compiler.infra;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the execution of all compiler passes.
 * Implements a simple PassManager using the Chain of Responsibility pattern.
 */
public class CompilerOrchestrator {

    private final List<CompilerPass> passes = new ArrayList<>();

    /**
     * Adds a compiler pass to the pipeline.
     * @param pass the CompilerPass to add
     */
    public void addPass(CompilerPass pass) {
        passes.add(pass);
    }

    /**
     * Runs all registered compiler passes sequentially.
     * Each pass may read and modify the shared CompilerContext.
     */
    public void runPasses(CompilerContext context) {
        for (CompilerPass pass : passes) {
            try {
                System.out.println("🟢 Running pass: " + pass.name());
                pass.execute(context);
                System.out.println("✅ Completed pass: " + pass.name());
            } catch (Exception e) {
                System.err.println("❌ Pass failed: " + pass.name());
                e.printStackTrace();
                break; // stop pipeline on fatal error
            }
        }
    }
}