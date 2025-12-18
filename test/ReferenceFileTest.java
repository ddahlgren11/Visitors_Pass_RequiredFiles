import compiler.frontend.MyParser;
import compiler.frontend.ParseException;
import compiler.frontend.Token;
import compiler.frontend.ast.ASTNode;
import org.junit.jupiter.api.Test;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ReferenceFileTest {

    private static final String REFERENCE_FILE_PATH = "src/compiler/reference_test.txt";

    @Test
    void testLexer() throws IOException {
        System.out.println("Testing Lexer on " + REFERENCE_FILE_PATH);
        try (InputStream stream = new FileInputStream(REFERENCE_FILE_PATH)) {
            MyParser parser = new MyParser(stream, StandardCharsets.UTF_8.name());
            Token token;
            int count = 0;
            do {
                token = parser.getNextToken();
                count++;
                // System.out.println("Token: " + token.image + " Kind: " + token.kind);
            } while (token.kind != 0); // 0 is EOF
            System.out.println("Lexer successfully processed " + count + " tokens.");
        } catch (Error e) {
            fail("Lexer failed with error: " + e.getMessage());
        }
    }

    @Test
    void testParser() throws IOException, ParseException {
        System.out.println("Testing Parser on " + REFERENCE_FILE_PATH);
        try (InputStream stream = new FileInputStream(REFERENCE_FILE_PATH)) {
            MyParser parser = new MyParser(stream, StandardCharsets.UTF_8.name());
            ASTNode node = parser.Program();
            assertNotNull(node, "Parser returned null ASTNode");
            System.out.println("Parser successfully built AST.");
            // Optional: print AST tree representation to stdout for manual verification
            // System.out.println(node.toASTTestTree().toString());
        }
    }
}
