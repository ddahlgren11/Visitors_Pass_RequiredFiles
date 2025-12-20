
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
        File m = new File("Main.j");
        if (m.exists()) m.delete();
        File p = new File("Helper.j");
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

        File f = new File("Helper.j");
        assertTrue(f.exists());
        String content = readFile(f);

        // Check for branching instructions
        assertTrue(content.contains("ifeq"), "Should contain conditional jump");
        assertTrue(content.contains("goto"), "Should contain goto");
        assertTrue(content.contains("ineg"), "Should contain negation for -n");
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

        File f = new File("Main.j");
        assertTrue(f.exists());
        String content = readFile(f);

        assertTrue(content.contains("ifeq"), "Loop should have condition check");
        assertTrue(content.contains("goto"), "Loop should loop back");
        assertTrue(content.contains("iadd"), "Loop body should increment");
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

    private String readFile(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        Scanner s = new Scanner(f);
        while(s.hasNextLine()) sb.append(s.nextLine()).append("\n");
        s.close();
        return sb.toString();
    }
}
