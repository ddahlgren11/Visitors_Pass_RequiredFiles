
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
        // File m = new File("Main.j");
        // if (m.exists()) m.delete();
        // File p = new File("Point.j");
        // if (p.exists()) p.delete();
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

        // Check Main.j
        File f = new File("Main.j");
        assertTrue(f.exists(), "Main.j should exist");

        System.out.println("=== Main.j ===");
        printFile(f);

        // Check Point.j
        File p = new File("Point.j");
        assertTrue(p.exists(), "Point.j should exist");

        System.out.println("=== Point.j ===");
        printFile(p);

        // Assertions
        String pointContent = readFile(p);
        assertTrue(pointContent.contains(".field public x I"));
        assertTrue(pointContent.contains(".field public y I"));
        // Definition should be correct now
        assertTrue(pointContent.contains(".method public set(II)V"));

        String mainContent = readFile(f);
        assertTrue(mainContent.contains("new Point"));
        assertTrue(mainContent.contains("invokespecial Point/<init>()V"));
        // We now expect correct signature (V return)
        assertTrue(mainContent.contains("invokevirtual Point/set(II)V"));
    }

    private void printFile(File f) throws Exception {
        Scanner s = new Scanner(f);
        while(s.hasNextLine()) System.out.println(s.nextLine());
        s.close();
    }

    private String readFile(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        Scanner s = new Scanner(f);
        while(s.hasNextLine()) sb.append(s.nextLine()).append("\n");
        s.close();
        return sb.toString();
    }
}
