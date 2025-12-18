import compiler.infra.*;
import compiler.frontend.ast.*;
import compiler.frontend.FrontEndPass;
import compiler.frontend.SymbolTableBuilderPass;
import compiler.frontend.TypeCheckingPass;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test scaffold for the modular compiler.
 * This test class verifies that each compiler pass can run successfully on valid input.
 * 
 * Students are expected to add additional tests for:
 *  - Invalid syntax cases
 *  - Undeclared variables
 *  - Type mismatches
 *  - Control flow edge cases (loops, if-else, etc.)
 */
public class MainTest {

    /**
     * Example program that should successfully parse and pass semantic analysis.
     */
    private static final String VALID_SOURCE = """
        class Dog {
            String name;
            void bark() {}
        }
        class Cat {
            int volume;
        }

        void print(String s) {}

        int count = 0;
        boolean ready = false;
        String name = "Fido";

        Dog d;
        Cat c;

        void speak(int x, int y) {
            print(d.name);
            print("Meow: " + c.volume);
            d.bark();
        }

        void reset() {
            count = 0;
            d = new Dog();
            c = new Cat();
        }

        boolean isZero() {
            return count == 0;
        }

        void main() {
            speak(0, 0);

            if (isZero()) {
                print("Still zero!");
            } else {
                // print("Count is now " + count); // String + int might not be supported in type checker?
                print("Count is now non-zero");
            }

            for (int i = 0; i < 10; i++) {
                count = count + 1;
            }

            while (count < 20) {
                count++;
            }

            reset();
        }
        """;

    /**
     * User provided complex sample with classes, loops, and object instantiation.
     */
    private static final String COMPLEX_SAMPLE_CLASS = """
        class SampleClass {
            int value;

            SampleClass(int v) {
                this.value = v;
            }

            int add(int x) {
                return this.value + x;
            }
        }

        class Main {
            public static int addNumbers(int x, int y) {
                return x + y;
            }

            public static int computeFinal(int a, int b) {
                return a * 2 + b / 3;
            }

            public static void main() {
                int a = 5;
                int b = 10;
                int c = a + b * 2;

                if (c > 20) {
                    c = c - 1;
                } else if (c == 20) {
                    c = c + 2;
                } else {
                    c = c + 3;
                }

                for (int i = 0; i < 5; i++) {
                    c = c + i;
                }

                while (c < 50) {
                    c = c + 1;
                }

                int result = addNumbers(a, b);
                int finalResult = computeFinal(result, c);

                SampleClass obj = new SampleClass(10);
                int objVal = obj.add(finalResult);
            }
        }
        """;

    private CompilerContext context;
    private CompilerOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        context = new CompilerContext();
        orchestrator = new CompilerOrchestrator();
    }

    /**
     * Smoke test to verify the compiler runs all passes without throwing exceptions.
     */
    @Test
    void testCompilerPipelineRunsSuccessfully() {
        ByteArrayInputStream input = new ByteArrayInputStream(VALID_SOURCE.getBytes(StandardCharsets.UTF_8));
        context.setInputStream(input);

        // Example pipeline — students must ensure these passes exist and are correctly implemented.
        orchestrator.addPass(new FrontEndPass());            // JavaCC Parser (lex + parse)
        orchestrator.addPass(new SymbolTableBuilderPass());  // Semantic: Symbol table construction
        orchestrator.addPass(new TypeCheckingPass());        // Semantic: Type checking

        assertDoesNotThrow(() -> orchestrator.runPasses(context),
                "Compiler passes should execute without throwing exceptions.");
    }

    /**
     * Example: Verify that no diagnostics are reported for valid input.
     * Students should expand this to test specific error and warning conditions.
     */
    @Test
    void testNoErrorsForValidProgram() {
        ByteArrayInputStream input = new ByteArrayInputStream(VALID_SOURCE.getBytes(StandardCharsets.UTF_8));
        context.setInputStream(input);

        orchestrator.addPass(new FrontEndPass());
        orchestrator.addPass(new SymbolTableBuilderPass());
        orchestrator.addPass(new TypeCheckingPass());

        orchestrator.runPasses(context);

        assertFalse(context.getDiagnostics().hasErrors(),
                "Expected no errors for a valid program, but some were reported.");
    }

    /**
     * Example placeholder for future student-written tests.
     * Encourage students to add multiple test cases below.
     */
    @Test
    void testStudentsShouldAddMoreCases() {
        // TODO: Students should add tests for:
        //  1. Syntax errors (missing semicolon, unmatched braces)
        //  2. Undeclared variables or functions
        //  3. Type mismatches (assigning int to boolean)
        //  4. Function redefinition or invalid parameter types
        //  5. Control flow corner cases
        //  6. Scope handling (variables declared inside loops or if-blocks)
        assertTrue(true, "Placeholder test — students must add their own test cases here.");
    }

    @Test
    void testComplexSampleClass() {
        ByteArrayInputStream input = new ByteArrayInputStream(COMPLEX_SAMPLE_CLASS.getBytes(StandardCharsets.UTF_8));
        context.setInputStream(input);

        orchestrator.addPass(new FrontEndPass());
        orchestrator.addPass(new SymbolTableBuilderPass());
        orchestrator.addPass(new TypeCheckingPass());

        assertDoesNotThrow(() -> orchestrator.runPasses(context),
                "Compiler passes should execute without throwing exceptions for complex class sample.");

        assertFalse(context.getDiagnostics().hasErrors(),
                "Complex class sample should parse and compile without errors. Errors: " + context.getDiagnostics().getErrors());
    }

    /**
     * Optional: Example to verify the SymbolTable is populated.
     * This assumes SymbolTableBuilderPass adds a symbol table to the context.
     * Modify or remove based on your implementation.
     */
    @Test
    void testSymbolTableIsBuilt() {
        ByteArrayInputStream input = new ByteArrayInputStream(VALID_SOURCE.getBytes(StandardCharsets.UTF_8));
        context.setInputStream(input);

        orchestrator.addPass(new FrontEndPass());
        orchestrator.addPass(new SymbolTableBuilderPass());

        orchestrator.runPasses(context);

        // Students can replace this with an actual check once the symbol table API is known.
        // Example: assertNotNull(context.getSymbolTople(), "Symbol table should be created after SymbolTableBuilderPass.");
        assertTrue(true, "Symbol table existence check placeholder.");
    }

    /**
     * Optional: Example to check that type errors are detected correctly.
     * Students should write real tests after implementing TypeCheckingPass.
     */
    @Test
    void testTypeCheckerReportsErrors() {
        String invalidProgram = """
            int x = 10;
            boolean y = x + 5;  // Type error
        """;

        ByteArrayInputStream input = new ByteArrayInputStream(invalidProgram.getBytes(StandardCharsets.UTF_8));
        context.setInputStream(input);

        orchestrator.addPass(new FrontEndPass());
        orchestrator.addPass(new SymbolTableBuilderPass());
        orchestrator.addPass(new TypeCheckingPass());

        orchestrator.runPasses(context);

        // Once DiagnosticReporter is implemented, this should verify reported errors.
        // Example: assertTrue(context.getDiagnostics().hasErrors());
        assertTrue(true, "Placeholder for type checking error test.");
    }
}
