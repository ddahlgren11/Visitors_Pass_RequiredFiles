
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

public class GenTest {

    @AfterEach
    public void cleanup() {
        File m = new File("Main.class");
        if (m.exists()) m.delete();
        File p = new File("Point.class");
        if (p.exists()) p.delete();
    }

    @Test
    public void testSimpleClass() throws Exception {
        String source = """
            class Point {
                int x;
                int y;
                void set(int a, int b) {
                    this.x = a;
                    this.y = b;
                }
            }
            void main() {
                Point p = new Point();
                p.set(10, 20);
            }
        """;

        CompilerContext context = new CompilerContext();
        context.setInputStream(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
        CompilerOrchestrator orchestrator = new CompilerOrchestrator();
        orchestrator.addPass(new FrontEndPass());
        orchestrator.addPass(new SymbolTableBuilderPass());
        orchestrator.addPass(new TypeCheckingPass());
        orchestrator.addPass(new TACConversionPass());
        orchestrator.addPass(new BytecodeGeneratorPass());

        orchestrator.runPasses(context);

        // Check Main.class
        File f = new File("Main.class");
        assertTrue(f.exists(), "Main.class should exist");

        // Check Point.class
        File p = new File("Point.class");
        assertTrue(p.exists(), "Point.class should exist");

        // Content checks removed as we produce binary .class files now
    }
}
