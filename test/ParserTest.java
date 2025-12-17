import compiler.frontend.MyParser;
import compiler.frontend.ParseException;
import compiler.frontend.ast.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    private ASTNode parse(String input) throws ParseException {
        ByteArrayInputStream stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        MyParser parser = new MyParser(stream);
        return parser.Program();
    }

    @Test
    void testEmptyProgram() throws ParseException {
        String input = "";
        ASTNode node = parse(input);
        assertTrue(node instanceof BlockNode);
        assertEquals(0, ((BlockNode) node).statements.size());
    }

    @Test
    void testVariables() throws ParseException {
        String input = "int a = 5; int b;";
        ASTNode node = parse(input);
        assertTrue(node instanceof BlockNode);
        BlockNode block = (BlockNode) node;
        assertEquals(2, block.statements.size());
        assertTrue(block.statements.get(0) instanceof VarDeclNode);
        assertTrue(block.statements.get(1) instanceof VarDeclNode);
    }

    @Test
    void testThisMemberAccess() throws ParseException {
        String input = """
            class A {
                int x;
                void foo() {
                    this.x = 10;
                    this.bar();
                }
                void bar() {}
            }
        """;
        ASTNode node = parse(input);
        assertTrue(node instanceof BlockNode);
        // We expect a ClassDeclNode
        BlockNode block = (BlockNode) node;
        assertEquals(1, block.statements.size());
        assertTrue(block.statements.get(0) instanceof ClassDeclNode);
    }

    @Test
    void testFactorThis() throws ParseException {
        String input = """
             class A {
                void m() {
                   this;
                   this.field;
                   this.method();
                }
             }
        """;
        ASTNode node = parse(input);
        // Just checking parsing passes
        assertNotNull(node);
    }

    @Test
    void testLoopsAndConditionals() throws ParseException {
        String input = """
            void main() {
                if (true) {
                    return;
                } else if (false) {
                    return;
                } else {
                    return;
                }

                while(true) {}

                for(int i=0; i<10; i++) {}
            }
        """;
        ASTNode node = parse(input);
        assertNotNull(node);
    }

    @Test
    void testExpressions() throws ParseException {
        String input = """
            void main() {
                int a = 1 + 2 * 3;
                boolean b = true || false && (1 == 2);
                int c = -a;
            }
        """;
        ASTNode node = parse(input);
        assertNotNull(node);
    }

    @Test
    void testInvalidSyntax() {
        String input = "int a = ;"; // Missing expression
        assertThrows(ParseException.class, () -> parse(input));
    }

    @Test
    void testNestedBlocks() throws ParseException {
         String input = "{ { int x; } }";
         // Program -> Block -> Statement -> Block -> Statement -> Block -> VarDecl
         // But Program() consumes statements until EOF and returns a BlockNode containing them.
         // So input "{ ... }" is interpreted as a Statement (Block) inside the implicit Program block.
         ASTNode node = parse(input);
         assertTrue(node instanceof BlockNode); // Root block
         assertEquals(1, ((BlockNode) node).statements.size());
         assertTrue(((BlockNode) node).statements.get(0) instanceof BlockNode);
    }
}
