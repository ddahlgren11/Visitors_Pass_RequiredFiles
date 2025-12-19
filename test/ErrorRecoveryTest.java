package compiler.test;

import compiler.frontend.MyParser;
import compiler.frontend.ParseException;
import compiler.frontend.TokenMgrError;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ErrorRecoveryTest {

    @Test
    public void testIllegalTokens() {
        // @ is illegal char. "unclosed is bad string.
        String input = "int i = 0; @ String s = \"unclosed";

        try {
            MyParser parser = new MyParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            parser.Program();

            // Should not throw exception, but should have errors.
            assertFalse(parser.getSyntaxErrors().isEmpty(), "Should have syntax errors for illegal tokens");

            boolean foundUnexpected = false;
            boolean foundBadString = false;
            for (String err : parser.getSyntaxErrors()) {
                System.out.println("Error: " + err);
                if (err.contains("UNEXPECTED_CHAR") || err.contains("@")) foundUnexpected = true;
                if (err.contains("BAD_STRING") || err.contains("unclosed")) foundBadString = true;
            }
            // Note: Exact message depends on generated code.
            // "Encountered ... UNEXPECTED_CHAR"

            // If UNEXPECTED_CHAR is a token, ParseException will say "Encountered: <UNEXPECTED_CHAR> (@)"

        } catch (TokenMgrError e) {
            fail("Should not throw TokenMgrError, should be handled as token: " + e.getMessage());
        } catch (ParseException e) {
            fail("Should recover from ParseException: " + e.getMessage());
        }
    }

    @Test
    public void testMultipleErrors() {
        String input = """
            class A {
                int a = ;
                int b = 2;
                void f() {
                    a = = 5;
                    b = 3;
                }
            }
            """;

        try {
            MyParser parser = new MyParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            parser.Program();

            assertEquals(2, parser.getSyntaxErrors().size(), "Should detect 2 errors");
            // 1. int a = ; (Missing expr, unexpected SEMI)
            // 2. a = = 5; (Unexpected ASSIGN)

        } catch (Exception e) {
             fail("Should not throw exception: " + e.getMessage());
        }
    }
}
