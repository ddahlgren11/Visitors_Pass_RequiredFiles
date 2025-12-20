
import compiler.infra.CompilerContext;
import compiler.infra.CompilerOrchestrator;
import compiler.frontend.FrontEndPass;
import compiler.frontend.SymbolTableBuilderPass;
import compiler.frontend.TypeCheckingPass;
import compiler.middle.tac.TACConversionPass;
import compiler.backend.BytecodeGeneratorPass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class BranchingTest {

    @AfterEach
    public void cleanup() {
        File m = new File("Main.class");
        if (m.exists()) m.delete();
        File p = new File("Helper.class");
        if (p.exists()) p.delete();
    }

    @Test
    public void testIfElse() throws Exception {
        String source = """
            class Helper {
                int abs(int n) {
                    if (n < 0) {
                        return -n;
                    } else {
                        return n;
                    }
                }
            }
            void main() {
                Helper h = new Helper();
                h.abs(-10);
            }
        """;

        compile(source);

        File f = new File("Helper.class");
        assertTrue(f.exists(), "Helper.class should exist");
    }

    @Test
    public void testWhileLoop() throws Exception {
        String source = """
            void main() {
                int i = 0;
                while (i < 10) {
                    i = i + 1;
                }
            }
        """;

        compile(source);

        File f = new File("Main.class");
        assertTrue(f.exists(), "Main.class should exist");
    }

    private void compile(String source) throws Exception {
        CompilerContext context = new CompilerContext();
        context.setInputStream(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
        CompilerOrchestrator orchestrator = new CompilerOrchestrator();
        orchestrator.addPass(new FrontEndPass());
        orchestrator.addPass(new SymbolTableBuilderPass());
        orchestrator.addPass(new TypeCheckingPass());
        orchestrator.addPass(new TACConversionPass());
        orchestrator.addPass(new BytecodeGeneratorPass());

        orchestrator.runPasses(context);
    }
}
