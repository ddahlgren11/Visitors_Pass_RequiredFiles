# 🧩 Visitor’s Pass Required

### Compiler Refactor and Design Assignment

Your mission: Refactor your compiler into a modular, extensible, and well-structured system that’s ready for future stages like TAC, SSA, and JVM bytecode generation.

---

## 🎯 Objective

Treat your compiler as a **real software project**.
Your goal is to refactor it for **clarity, extensibility, determinism, and observability** using sound software engineering principles and design patterns.

You already have a working lexer, parser, and semantic checker.
This week, you’ll transform your compiler into a **pipeline of modular passes** with well-defined responsibilities.

---

## 🧱 1. Project Structure and Organization

Reorganize your project into the following package structure:

```
compiler/
  frontend/      → Lexer, Parser, AST
  middle/        → Semantic Analysis
  backend/       → Code Generation (later)
  infra/         → Shared infrastructure (context, diagnostics, passes)
  cli/           → Main entry point and command-line handling
```

### ✅ Checklist

* [ ] Move existing source files into appropriate packages.
* [ ] Update all package declarations and imports.
* [ ] Ensure your JavaCC grammar uses:

  ```java
  PARSER_PACKAGE = "compiler.frontend";
  package compiler.frontend;
  ```
* [ ] Verify your project builds cleanly after reorganization.

---

## ⚙️ 2. Core Infrastructure

Implement the foundational infrastructure that all passes will share.

### Compiler Context

* [ ] Create a `CompilerContext` class to hold shared state (input stream, tokens, AST, symbol table, etc.).
* [ ] No globals — passes share data through the context.

### Compiler Pass Interface

* [ ] Define a `CompilerPass` interface with methods:

  ```java
  String name();
  void execute(CompilerContext context) throws Exception;
  ```

### Compiler Orchestrator (Pass Manager)

* [ ] Implement a `CompilerOrchestrator` class that runs passes in order.
* [ ] Add simple console logging for each pass start/end.
* [ ] Make it easy to add/remove passes without changing orchestration logic.

---

## 🧩 3. Front End: Lexing and Parsing

### Combined Lexical + Syntax Analysis Pass

Because JavaCC typically generates both the **lexer and parser** from the same grammar, it’s perfectly fine to combine these into **a single front-end pass**.

* [ ] Implement a `FrontEndPass` (or `SyntaxAnalysisPass`) that:

  * Uses your JavaCC-generated parser (`MyParser`) to process the input stream.
  * Handles both lexing and parsing in one phase.
  * Builds an **AST** (Abstract Syntax Tree) and stores it in the `CompilerContext`.
  * Handles and reports parsing errors through the `DiagnosticReporter`.

### Grammar Updates

* [ ] Ensure your `Grammar.jj` file includes:

  ```java
  PARSER_PACKAGE = "compiler.frontend";
  package compiler.frontend;
  ```
* [ ] Verify that your parser is non-static (`STATIC = false`) so it integrates cleanly into an object-oriented design.

---

## 🌳 4. Abstract Syntax Tree and Visitor Pattern

Refactor your AST into a clear, consistent hierarchy.

### Base Classes

* [ ] Create an abstract `ASTNode` class in `compiler.frontend.ast`.
* [ ] Define subclasses like `ExpressionNode`, `BinaryExprNode`, `AssignmentNode`, etc.

### Visitor Interface

* [ ] Define an `ASTVisitor<T>` interface:

  ```java
  public interface ASTVisitor<T> {
      T visit(BinaryExprNode node);
      T visit(AssignmentNode node);
      ...
  }
  ```

### Using Visitors

* [ ] Each compiler pass that operates on the AST (e.g., type checking, code generation) should implement its own visitor.
* [ ] Keep **logic out of the AST nodes** — ASTs hold data; visitors hold behavior.
* [ ] Example visitors:

  * `SymbolTableBuilderVisitor`
  * `TypeCheckingVisitor`

---

## 🧠 5. Semantic Analysis Passes

Refactor your semantic checks into clean, phase-driven passes.

### Symbol Table Builder Pass

Use the **SymbolTable** implementation you created in your last assignment.
This pass should **populate** that structure from the AST.

* [ ] Create a `SymbolTableBuilderPass` that:

  * Traverses the AST using a visitor.
  * Defines variables, functions, and types in the `SymbolTable`.
  * Pushes/pops scopes appropriately.
  * Stores the resulting symbol table in the `CompilerContext` for use by later passes.

### Type Checking Pass

Once the symbol table is built, you’ll perform semantic validation of expressions and statements.

* [ ] Create a `TypeCheckingPass` that:

  * Uses the AST and symbol table from the context.
  * Traverses the AST using a visitor.
  * Ensures all operations are type-safe (e.g., `x + y` requires numeric operands).
  * Reports errors through the `DiagnosticReporter`.

---

## ⚗️ 6. Infrastructure Utilities

### Diagnostic Reporter

All compiler passes should use a **shared diagnostic system** for consistent error reporting.

* [ ] Implement a `DiagnosticReporter` class to collect and print compiler errors/warnings.
* [ ] Include:

  * `reportError(String message)`
  * `reportWarning(String message)`
  * `hasErrors()`
* [ ] Add a `DiagnosticReporter` instance to your `CompilerContext`.

### Logging and Observability

* [ ] Add simple logging in each pass (start, end, key actions).
* [ ] Optionally, print to the console or save to a file for easier debugging.

---

## 🚀 7. Putting It All Together

In your `compiler.cli.Main` class:

* [ ] Instantiate a `CompilerContext`.
* [ ] Build a `CompilerOrchestrator`.
* [ ] Add passes in order:

  ```java
  orchestrator.addPass(new FrontEndPass());              // Lex + Parse
  orchestrator.addPass(new SymbolTableBuilderPass());    // Build symbols
  orchestrator.addPass(new TypeCheckingPass());          // Type checking
  ```
* [ ] Run the orchestrator with:

  ```java
  orchestrator.runPasses(context);
  ```
* [ ] Print success message or diagnostics at the end.

---

## 🧪 8. Testing and Verification

* [ ] Write small test input programs to validate parsing, symbol table creation, and type checking.
* [ ] Verify that each phase logs correctly and produces expected results.
* [ ] Ensure all errors and warnings are reported clearly through `DiagnosticReporter`.

---

## 🧭 9. Tips for a Clean Refactor

* Keep each class small and focused — **Single Responsibility Principle**.
* Prefer composition over inheritance where possible.
* Avoid “god classes” — the compiler should feel like a *pipeline*, not a monolith.
* Implement one pass completely before moving to the next.
* Use logging and diagnostics to make debugging easier.

---

## ✅ Deliverables

By the end, you should have:

* A modular compiler pipeline.
* A combined **FrontEndPass** (lexing + parsing).
* A `SymbolTableBuilderPass` and a `TypeCheckingPass`.
* A working `DiagnosticReporter`.
* A functioning `CompilerOrchestrator` that ties it all together.

---

Happy refactoring, and may your passes always compile on the first try!


# ⚠️ Important Grading Rules ⚠️
1. Code that does not compile will recieve an automatic 50% penalty or more depending on if I want to go digging through your code to see why it isn't working. Happy to help during office hours tho :-)
2. Gratuitous modification of test code to make your code pass if it doesn't will also recieve a significant up to 50% penalty (at my discression). Please check with me before modifying tests.
3. With the exception of cases where I'm in a good mood :-) you can expect to receive a 0 for any assignment that is not turned in before the deadline if arrangements for a late submission are not made before hand.

## Downloading, Unzipping, and Running the Assignment
### Step 1: Download the assignment ZIP file from your course portal.
### Step 2: Unzip the file into a directory of your choice.
You can use the following command in the terminal to unzip the file:
```sh
unzip the-assignment.zip
```

### Step 3: Open a terminal (Command Prompt on Windows, Terminal on Mac/Linux) and navigate to the unzipped directory using the `cd` command.

### Step 4: Build the Docker image using the following command:
#### Windows only (Ensure Docker Desktop is running)
```sh
docker build -t visitors-pass-required .
```

### Step 5: Once the image is built, run the container using:
#### Linux/Mac:
```sh
docker run -it --rm -v "$(pwd)":/workspace visitors-pass-required
```
#### Windows:
```sh
docker run -it --rm -v "%cd%":/workspace visitors-pass-required
```
### Step 6: Get most recent assignment code.
Once your workspace is running run the following command to get the most recent code from github.
Select your class then select the current assignment short name: visitors-pass-required
```sh
refresh-assignment-files.sh
```

## Using the Installed Scripts
After running the Docker container, you will have access to three scripts that are included in the Docker image:

1. **compile.sh:** This script compiles all code including `Main.java` and `MainTest.java` files.
    ```sh
    compile.sh
    ```
    It will compile the source files and ensure that your code is ready for testing.

2. **run-tests.sh:** This script runs the JUnit tests in the `MainTest.java` file.
    ```sh
    run-tests.sh
    ```
    The results will be saved to a file named `test-results.txt`.

3. **prepare_to_submit.sh:** This script prepares your assignment for submission by running all tests and creating a ZIP file containing your work.
    ```sh
    prepare_to_submit.sh
    ```
    If all tests pass, it will create a file named `Assignment.zip` in the `/workspace` directory, ready for submission.

4. **update-docker.sh:** This script downloads a new Dockerfile to your workspace. This is sometimes necessary if updates are required.
    ```sh
    update-docker.sh
    ```
    You will need to exit, rebuild, and restart your Docker container after running this command.

5. **refresh-assignment-files.sh:** This script downloads a fresh copy of the assignment files into your workspace to a directory named visitors-pass-required.
    ```sh
    refresh-assignment-files.sh
    ```
    You will need to copy the files you want to replace from the visitors-pass-required directory.


# Getting Started with Docker for Your Assignment
This assignment will guide you through creating a Docker-based environment. Follow the instructions below to set up Docker on your system and complete the tasks.

## Setting Up Docker
### Windows
1. Download Docker Desktop from [Docker’s official website](https://www.docker.com/products/docker-desktop/).
2. Run the installer and follow the prompts.
3. After installation, launch Docker Desktop and ensure it is running.

### Mac
1. Open Terminal.
2. Install Homebrew if it is not already installed by running the following command:
    ```sh
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    ```
3. Run the following command to install Docker using Homebrew:
    ```sh
    brew install --cask docker
    ```
4. Start Docker from the command line:
    ```sh
    open /Applications/Docker.app
    ```
5. Verify Docker installation by running:
    ```sh
    docker --version
    ```

### Linux
1. Open a terminal.
2. Run the following commands:
    ```sh
    sudo apt-get update
    sudo apt-get install docker.io
    ```
3. Verify Docker installation with:
    ```sh
    docker --version
    ```

