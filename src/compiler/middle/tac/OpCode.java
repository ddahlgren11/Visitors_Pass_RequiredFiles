package compiler.middle.tac;

public enum OpCode {
    LOAD_CONST,
    LOAD_VAR,
    STORE_VAR,
    ADD,
    SUB,
    MUL,
    DIV,
    RETURN,
    LABEL,
    GOTO,
    IFZ,
    PARAM,
    CALL,          // Generic call, deprecated in favor of specific ones if possible, or used for dynamic
    CALL_STATIC,
    CALL_VIRTUAL,
    NEW,
    NEW_ALLOC,     // Allocate memory (new Class; dup)
    NEW_CONSTRUCT, // Call constructor (invokespecial)
    GET_FIELD,
    PUT_FIELD,
    EQ,
    NEQ,
    LT,
    LE,
    GT,
    GE,
    AND,
    OR,
    NOT,
    NEG,
    FUNC_ENTRY,
    FUNC_EXIT,
    PARAM_DECL,    // For declaring a parameter in a function body
    FIELD_DECL     // For declaring a class field
}
