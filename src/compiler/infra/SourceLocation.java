package compiler.infra;

/**
 * Records source location information for diagnostic messages.
 */
public class SourceLocation {
    private final String file;     // source file path or "<stdin>"
    private final int line;        // 1-based line number
    private final int column;      // 1-based column number
    private final String context;  // optional contextual string (e.g., current function/class)

    public SourceLocation(String file, int line, int column, String context) {
        this.file = file;
        this.line = line;
        this.column = column;
        this.context = context;
    }
    /** Create a location with empty context. */
    public SourceLocation(String file, int line, int column) {
        this(file, line, column, null);
    }

    /** Create a location from another, with new context. */
    public SourceLocation withContext(String newContext) {
        return new SourceLocation(file, line, column, newContext);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(file == null ? "<unknown>" : file);
        if (line > 0) {
            sb.append(':').append(line);
            if (column > 0) sb.append(':').append(column);
        }
        if (context != null && !context.isEmpty()) {
            sb.append(" (in ").append(context).append(')');
        }
        return sb.toString();
    }
}