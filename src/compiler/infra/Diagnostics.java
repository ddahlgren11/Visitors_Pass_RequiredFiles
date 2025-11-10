package compiler.infra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive diagnostics collector and reporter for the compiler pipeline.
 * Supports errors and warnings with source locations and logging capabilities.
 */
public class Diagnostics {
    public enum Severity { WARNING, ERROR }

    /** Represents a single diagnostic message. */
    private static class Diagnostic {
        final Severity severity;
        final String message;
        final SourceLocation location;
        final LocalDateTime timestamp;

        Diagnostic(Severity severity, String message, SourceLocation location, LocalDateTime timestamp) {
            this.severity = severity;
            this.message = message;
            this.location = location;
            this.timestamp = timestamp;
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            // [YYYY-MM-DD HH:mm:ss] ERROR file:line:col: message
            sb.append('[')
              .append(timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
              .append("] ")
              .append(severity)
              .append(' ');
            
            if (location != null) {
                sb.append(location).append(": ");
            }
            
            sb.append(message);
            return sb.toString();
        }
    }

    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private PrintStream logStream = System.out;  // default to stdout
    private boolean consoleEcho = true;          // echo to console by default

    /**
     * Report an error with source location.
     */
    public void reportError(String message, SourceLocation location) {
        report(Severity.ERROR, message, location);
    }

    /**
     * Report an error without location info.
     */
    public void reportError(String message) {
        report(Severity.ERROR, message, null);
    }

    /**
     * Report a warning with source location.
     */
    public void reportWarning(String message, SourceLocation location) {
        report(Severity.WARNING, message, location);
    }

    /**
     * Report a warning without location info.
     */
    public void reportWarning(String message) {
        report(Severity.WARNING, message, null);
    }

    /**
     * Legacy support for addError (maps to reportError).
     */
    public void addError(String message) {
        reportError(message);
    }

    private void report(Severity severity, String message, SourceLocation location) {
        Diagnostic d = new Diagnostic(severity, message, location, LocalDateTime.now());
        diagnostics.add(d);
        if (consoleEcho) {
            logStream.println(d);
            logStream.flush();
        }
    }

    /** Log a general message (non-error) to the output. */
    public void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String formatted = String.format("[%s] %s", timestamp, message);
        if (consoleEcho) {
            logStream.println(formatted);
            logStream.flush();
        }
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity == Severity.ERROR);
    }

    public boolean hasWarnings() {
        return diagnostics.stream().anyMatch(d -> d.severity == Severity.WARNING);
    }

    public List<Diagnostic> getDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    public List<String> getErrors() {
        return diagnostics.stream()
            .filter(d -> d.severity == Severity.ERROR)
            .map(Diagnostic::toString)
            .collect(java.util.stream.Collectors.toList());
    }

    public List<String> getWarnings() {
        return diagnostics.stream()
            .filter(d -> d.severity == Severity.WARNING)
            .map(Diagnostic::toString)
            .collect(java.util.stream.Collectors.toList());
    }

    /** Redirect logging output (default System.out). */
    public void setLogStream(PrintStream stream) {
        this.logStream = stream != null ? stream : System.out;
    }

    /** Control whether messages are echoed to console (true by default). */
    public void setConsoleEcho(boolean echo) {
        this.consoleEcho = echo;
    }

    /** Clear all accumulated diagnostics. */
    public void clear() {
        diagnostics.clear();
    }

    /** Return a summary string of all diagnostics. */
    public String getSummary() {
        long errors = diagnostics.stream().filter(d -> d.severity == Severity.ERROR).count();
        long warnings = diagnostics.stream().filter(d -> d.severity == Severity.WARNING).count();
        return String.format("%d error(s), %d warning(s)", errors, warnings);
    }
}
