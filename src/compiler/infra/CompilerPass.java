package compiler.infra;

/**
 * Represents a single compiler phase or transformation.
 * Each CompilerPass should perform one logical task such as
 * lexing, parsing, semantic checking, IR generation, or code generation.
 */
public interface CompilerPass {

    /**
     * Returns the display name of this pass.
     * Example: "Lexical Analysis", "Syntax Analysis", etc.
     */
    String name();

    /**
     * Executes this compiler pass using the provided context.
     * @param context the shared compilation context containing state and data
     * @throws Exception if this pass encounters an unrecoverable error
     */
    void execute(CompilerContext context) throws Exception;
}