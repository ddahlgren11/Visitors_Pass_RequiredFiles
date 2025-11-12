package compiler.middle.tac;

public enum OpCode {
    // Binary Operations
    ADD,
    SUB,
    MUL,
    DIV,

    // Memory Operations
    LOAD_CONST,
    STORE_VAR,
    LOAD_VAR,

    // Control Flow
    JUMP,
    JUMP_IF_ZERO,
    LABEL,

    // Function Calls
    CALL,
    RETURN,
    PARAM,

    // No operation
    NOOP
}
