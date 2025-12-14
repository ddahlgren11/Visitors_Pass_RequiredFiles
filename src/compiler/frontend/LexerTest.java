package compiler.frontend;

import java.io.*;

public class LexerTest {
    public static void main(String[] args) throws Exception {
        Reader r = new InputStreamReader(System.in);
        SimpleCharStream stream = new SimpleCharStream(r, 1, 1);
        MyParserTokenManager tm = new MyParserTokenManager(stream);

        Token t;
        while (true) {
            t = tm.getNextToken();
            if (t.kind == MyParserConstants.EOF) {
                break;
            }
            System.out.println("TOKEN: kind=" + t.kind + " image=\"" + t.image + "\"");
        }
    }
}
