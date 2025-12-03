package compiler.frontend;

public class ParseException extends Exception {
    public ParseException() { super(); }
    public ParseException(String message) { super(message); }
    public ParseException(Token currentTokenVal, int[][] expectedTokenSequencesVal, String[] tokenImageVal) {
        super("Encountered \"" + currentTokenVal.next + "\" at line " + currentTokenVal.next.beginLine + ", column " + currentTokenVal.next.beginColumn + ".");
    }
}
