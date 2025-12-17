# Modular Java Compiler

A modular, extensible compiler implementation in Java. This project demonstrates a multi-pass compiler architecture, currently supporting lexical analysis, parsing, AST generation, symbol table construction, and type checking for a Java-like language.

## 🚀 Features

*   **Modular Architecture**: Uses a `CompilerPass` system orchestrated by a `CompilerOrchestrator`.
*   **Frontend**: JavaCC-based Lexer and Parser that generates a typed Abstract Syntax Tree (AST).
*   **Semantic Analysis**:
    *   **Symbol Table**: Scope-aware symbol management for variables and functions.
    *   **Type Checking**: Validates type safety, operator usage, and assignment compatibility.
*   **Infrastructure**: Robust error reporting via `DiagnosticReporter` and shared state via `CompilerContext`.
*   **Testing**: Comprehensive JUnit 5 test suite.

## 📂 Project Structure

The source code is organized into the following packages within `src/compiler`:

*   **`cli`**: Contains the command-line entry point (`Main.java`).
*   **`frontend`**: Handles the initial processing of source code.
    *   `grammar.jj`: JavaCC grammar file defining the lexer and parser.
    *   `ast`: Abstract Syntax Tree node definitions.
    *   `FrontEndPass.java`: Combines lexing and parsing to produce an AST.
*   **`middle`**: Contains intermediate analysis and representations.
    *   `SymbolTable`: Interfaces and implementations for scope management.
    *   `tac`: (Experimental) Three-Address Code generation infrastructure.
*   **`infra`**: Core shared utilities.
    *   `CompilerContext`: Holds the state of the compilation (AST, Symbol Table, Errors).
    *   `CompilerPass`: Interface for all compiler phases.
    *   `CompilerOrchestrator`: Manages the execution order of passes.
    *   `Diagnostics`: Error and warning reporting.

## 🛠 Building and Running

### Prerequisites
*   Java JDK 11+
*   Bash (for script execution)

### Compilation
To compile the project, use the provided script:
```bash
./bin/compile.sh
```
This will compile all source files into the `out/` directory.

### Running the Compiler
To compile a source file:
```bash
java -cp out compiler.cli.Main <path-to-source-file>
```

Example:
```bash
java -cp out compiler.cli.Main test_inputs/test1.txt
```

### Running Tests
To run the JUnit test suite:
```bash
./bin/run-tests.sh
```

## 🧩 Architecture Detail

The compiler operates as a linear pipeline of passes:

1.  **FrontEndPass**:
    *   Reads the source file.
    *   Lexes the input into tokens.
    *   Parses tokens into an Abstract Syntax Tree (AST).
    *   Reports syntax errors.

2.  **SymbolTableBuilderPass**:
    *   Traverses the AST.
    *   Builds a Symbol Table with nested scopes (Global, Function, Block).
    *   Registers variable and function declarations.
    *   Reports "Duplicate declaration" errors.

3.  **TypeCheckingPass**:
    *   Traverses the AST using the Symbol Table.
    *   Resolves variable references to their symbols.
    *   Validates types for expressions (e.g., ensuring arithmetic operations use numbers).
    *   Checks return types and assignment compatibility.
    *   Reports "Type mismatch" or "Undeclared variable" errors.

## 🤝 Extending the Compiler

New passes can be added by implementing the `CompilerPass` interface and registering them in `src/compiler/cli/Main.java`:

```java
orchestrator.addPass(new MyNewPass());
```

The `CompilerContext` allows data to be shared between passes without tight coupling.
