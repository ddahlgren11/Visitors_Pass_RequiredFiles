# Java Compiler

This is a simple Java-based compiler designed to process a source file and generate output. It is structured into several passes, including lexical analysis, syntax analysis, type checking, and code generation.

## Project Structure

The compiler is organized into the following directories, each with a distinct role in the compilation process:

*   `src/compiler/cli`: The **Command-Line Interface** is the entry point of the application. Its primary role is to handle user interaction. It parses command-line arguments (such as the path to the source file), initializes the compiler's context, orchestrates the sequence of compilation passes, and reports the final status (success or errors) to the user.

*   `src/compiler/frontend`: The **Frontend** is responsible for analyzing the source code. It transforms the raw text into a structured representation that the rest of the compiler can work with. This process involves:
    *   **Lexical Analysis:** Breaking the source code into a stream of tokens.
    *   **Syntax Analysis (Parsing):** Building an Abstract Syntax Tree (AST) from the tokens to represent the code's grammatical structure.
    *   **Semantic Analysis:** Traversing the AST to check for semantic correctness, including tasks like type checking and building symbol tables to track declared variables and functions.

*   `src/compiler/middle`: The **Middle End** focuses on machine-independent optimizations. It takes the AST from the frontend and typically converts it into an Intermediate Representation (IR). On this IR, it performs various optimizations to improve the code's efficiency, such as constant folding or dead code elimination, without altering its original meaning.

*   `src/compiler/infra`: The **Infrastructure** provides the foundational framework and shared services that support the entire compilation process. It contains reusable components that are essential for the other parts of the compiler to function, including:
    *   A diagnostics engine for collecting and reporting errors and warnings.
    *   A pass manager to orchestrate the execution of different compiler stages.
    *   A central compiler context to hold shared data structures like the AST and symbol tables.

## Usage

To use the compiler, you first need to compile the Java source files. From the `src` directory, you can run:

```bash
javac compiler/cli/Main.java
```

Once the compiler is built, you can run it by providing a source file as an argument:

```bash
java compiler.cli.Main <source-file>
```
