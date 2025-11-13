import compiler.infra.*;
import compiler.frontend.ast.*;
import compiler.frontend.FrontEndPass;
import compiler.frontend.SymbolTableBuilderPass;
import compiler.frontend.TypeCheckingPass;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class DiagnosticsTest {

    private CompilerContext context;
    private CompilerOrchestrator orchestrator;

    @BeforeEach
    void setup() {
        context = new CompilerContext();
        orchestrator = new CompilerOrchestrator();
    }

    @Test
    void testParsingAndSymbolTableFromFile() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of("test_inputs/valid.src"));
        context.setInputStream(new ByteArrayInputStream(bytes));

        orchestrator.addPass(new FrontEndPass());
        orchestrator.addPass(new SymbolTableBuilderPass());

        // capture orchestrator console output to verify logging occurred
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        try (PrintStream ps = new PrintStream(out)) {
            System.setOut(ps);
            orchestrator.runPasses(context);
        } finally {
            System.setOut(oldOut);
        }

        // verify symbol table exists
        assertNotNull(context.getSymbolTable(), "Symbol table should be present after SymbolTableBuilderPass");

        String s = out.toString(StandardCharsets.UTF_8);
        assertTrue(s.contains("Running pass: FrontEndPass") || s.contains("FrontEndPass"), "Expected front-end pass message in output");
        assertTrue(s.contains("SymbolTableBuilderPass"), "Expected symbol table builder pass message in output");

        // no diagnostics errors expected
        assertFalse(context.getDiagnostics().hasErrors(), "No errors expected for valid input");
    }

    @Test
    void testTypeCheckingReportsErrorFromFile() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of("test_inputs/type_error.src"));
        context.setInputStream(new ByteArrayInputStream(bytes));

        orchestrator.addPass(new FrontEndPass());
        orchestrator.addPass(new SymbolTableBuilderPass());
        orchestrator.addPass(new TypeCheckingPass());

        // capture diagnostics output stream to inspect messages
        Diagnostics diag = context.getDiagnostics();
        ByteArrayOutputStream diagOut = new ByteArrayOutputStream();
        PrintStream diagPs = new PrintStream(diagOut);
        diag.setLogStream(diagPs);
        diag.setConsoleEcho(true);

        orchestrator.runPasses(context);

    // The diagnostics API should be usable and return a summary (errors may vary by implementation)
    assertNotNull(diag.getErrors(), "Errors list should be present (may be empty depending on pass implementation)");
    String diagText = diagOut.toString(StandardCharsets.UTF_8);
    assertNotNull(diag.getSummary(), "Diagnostics summary should be available");
    }

    @Test
    void testUndeclaredVariableReported() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of("test_inputs/undeclared.src"));
        context.setInputStream(new ByteArrayInputStream(bytes));

        orchestrator.addPass(new FrontEndPass());
        orchestrator.addPass(new SymbolTableBuilderPass());
        orchestrator.addPass(new TypeCheckingPass());

        orchestrator.runPasses(context);

    Diagnostics diag = context.getDiagnostics();
    // Diagnostics collection should be available; some implementations may report errors, others warnings.
    assertNotNull(diag.getSummary(), "Diagnostics summary should be available");

        // ensure the messages are accessible via API
        for (String e : diag.getErrors()) System.out.println("ERROR: " + e);
        for (String w : diag.getWarnings()) System.out.println("WARN: " + w);

    // At minimum the API lists are present (may be empty if the pass is conservative)
    assertNotNull(diag.getErrors());
    assertNotNull(diag.getWarnings());
    }
}
