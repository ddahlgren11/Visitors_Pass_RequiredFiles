import compiler.frontend.MyParser;
import compiler.frontend.ParseException;
import compiler.frontend.ast.ASTNode;
import compiler.frontend.visitor.SymbolTableBuilderVisitor;
import compiler.infra.Diagnostics;
import compiler.middle.Symbol;
import compiler.middle.SymbolTable;
import compiler.middle.SymbolTableImpl;
import compiler.middle.Kind;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SymbolTableTest {

    private ASTNode parse(String input) throws ParseException {
        ByteArrayInputStream stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        MyParser parser = new MyParser(stream);
        return parser.Program();
    }

    private static class Result {
        SymbolTable table;
        Diagnostics diagnostics;
    }

    private Result analyze(String source) throws ParseException {
        ASTNode root = parse(source);
        SymbolTable table = new SymbolTableImpl();
        Diagnostics diag = new Diagnostics();
        diag.setConsoleEcho(false); // Silence output during tests

        table.enterScope(); // Global scope
        SymbolTableBuilderVisitor visitor = new SymbolTableBuilderVisitor(table, diag);

        // Unwrap the root BlockNode to simulate that these statements are IN the global scope
        // rather than in a child scope that gets popped.
        if (root instanceof compiler.frontend.ast.BlockNode) {
            for (ASTNode stmt : ((compiler.frontend.ast.BlockNode)root).statements) {
                stmt.accept(visitor);
            }
        } else {
            root.accept(visitor);
        }
        // We do NOT exit the global scope so we can inspect it.

        Result res = new Result();
        res.table = table;
        res.diagnostics = diag;
        return res;
    }

    @Test
    void testGlobalVariables() throws ParseException {
        Result res = analyze("int x; boolean y;");
        assertFalse(res.diagnostics.hasErrors(), "Should have no errors: " + res.diagnostics.getErrors());

        Optional<Symbol> x = res.table.lookup("x");
        assertTrue(x.isPresent(), "Symbol 'x' not found");
        assertEquals(Kind.VARIABLE, x.get().kind());

        Optional<Symbol> y = res.table.lookup("y");
        assertTrue(y.isPresent(), "Symbol 'y' not found");
        assertEquals(Kind.VARIABLE, y.get().kind());
    }

    @Test
    void testDuplicateGlobalVariables() throws ParseException {
        Result res = analyze("int x; boolean x;");
        assertTrue(res.diagnostics.hasErrors(), "Should detect duplicate variable declaration");
        assertEquals(1, res.diagnostics.getErrors().size());
        assertTrue(res.diagnostics.getErrors().get(0).contains("Duplicate declaration: x"));
    }

    @Test
    void testUndeclaredVariableUsage() throws ParseException {
        Result res = analyze("x = 5;");
        assertTrue(res.diagnostics.hasErrors(), "Should detect undeclared variable usage");
        assertTrue(res.diagnostics.getErrors().get(0).contains("Use of undeclared variable: x"));
    }

    @Test
    void testFunctions() throws ParseException {
        Result res = analyze("void foo() {} int bar() { return 1; }");
        assertFalse(res.diagnostics.hasErrors(), "Should have no errors: " + res.diagnostics.getErrors());

        Optional<Symbol> foo = res.table.lookup("foo");
        assertTrue(foo.isPresent(), "Symbol 'foo' not found");
        assertEquals(Kind.FUNCTION, foo.get().kind());

        Optional<Symbol> bar = res.table.lookup("bar");
        assertTrue(bar.isPresent(), "Symbol 'bar' not found");
        assertEquals(Kind.FUNCTION, bar.get().kind());
    }

    @Test
    void testScopeShadowing() throws ParseException {
        // Shadowing is allowed
        Result res = analyze("""
            int x;
            void foo() {
                int x;
            }
        """);
        assertFalse(res.diagnostics.hasErrors(), "Should have no errors: " + res.diagnostics.getErrors());
    }

    @Test
    void testScopeVisibility() throws ParseException {
        // Variable inside block not visible outside
        Result res = analyze("""
            void foo() {
                {
                    int x;
                }
                x = 5;
            }
        """);
        assertTrue(res.diagnostics.hasErrors(), "Should detect undeclared variable outside scope");
        assertTrue(res.diagnostics.getErrors().stream().anyMatch(e -> e.contains("undeclared variable: x")));
    }

    @Test
    void testParameters() throws ParseException {
        // Parameters should be visible inside function body
        Result res = analyze("""
            void foo(int p) {
                p = 10;
            }
        """);
        assertFalse(res.diagnostics.hasErrors(), "Should have no errors: " + res.diagnostics.getErrors());
    }

    @Test
    void testClassMembers() throws ParseException {
         Result res = analyze("""
            class A {
                int f;
                void m() {
                    f = 10;
                }
            }
        """);
        assertFalse(res.diagnostics.hasErrors(), "Should have no errors: " + res.diagnostics.getErrors());

        Optional<Symbol> classA = res.table.lookup("A");
        assertTrue(classA.isPresent(), "Symbol 'A' not found");
        assertEquals(Kind.TYPE, classA.get().kind());
    }

    @Test
    void testClassForwardReference() throws ParseException {
        Result res = analyze("""
            class A {
                void m() {
                    f = 10;
                }
                int f;
            }
        """);
        assertFalse(res.diagnostics.hasErrors(), "Should have no errors: " + res.diagnostics.getErrors());
    }
}
