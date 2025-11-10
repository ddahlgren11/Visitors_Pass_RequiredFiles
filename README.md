# Java Compiler

This is a simple Java-based compiler designed to process a source file and generate output. It is structured into several passes, including lexical analysis, syntax analysis, type checking, and code generation.

## Project Structure

The compiler is organized into the following directories:

*   `src/compiler/cli`: Contains the command-line interface for the compiler. This is the entry point of the application.
*   `src/compiler/frontend`: Includes the frontend passes of the compiler, such as lexical analysis, syntax analysis, and type checking.
*   `src/compiler/middle`:  This directory is intended for intermediate representations and optimizations.
*   `src/compiler/infra`: Provides the core infrastructure for the compiler, including diagnostics, compiler passes, and the compiler context.

## Usage

To use the compiler, you first need to compile the Java source files. From the `src` directory, you can run:

```bash
javac compiler/cli/Main.java
```

Once the compiler is built, you can run it by providing a source file as an argument:

```bash
java compiler.cli.Main <source-file>
```
