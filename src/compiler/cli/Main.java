package compiler.cli;

import compiler.frontend.FrontEndPass;
import compiler.frontend.SymbolTableBuilderPass;
import compiler.frontend.TypeCheckingPass;
import compiler.middle.tac.TACConversionPass;
import compiler.backend.BytecodeGeneratorPass;
import compiler.infra.CompilerContext;
import compiler.infra.CompilerOrchestrator;
import compiler.infra.CompilerPass;
import compiler.infra.Diagnostics;

import compiler.frontend.MyParser;
import compiler.frontend.MyParserTokenManager;
import compiler.frontend.SimpleCharStream;
import compiler.frontend.Token;
import compiler.frontend.MyParserConstants;
import compiler.frontend.ast.ASTNode;
import compiler.frontend.ast.ClassDeclNode;
import compiler.frontend.ast.BlockNode;
import compiler.middle.SymbolTable;
import compiler.middle.ScopeInfo;
import compiler.middle.Symbol;
import compiler.middle.tac.TACInstruction;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java compiler.cli.Main <source-file> [--debug]");
            return;
        }

        String sourceFile = null;
        boolean debugMode = false;

        for (String arg : args) {
            if (arg.equals("--debug")) {
                debugMode = true;
            } else if (!arg.startsWith("-")) {
                sourceFile = arg;
            }
        }

        if (sourceFile == null) {
            System.out.println("Usage: java compiler.cli.Main <source-file> [--debug]");
            return;
        }

        if (debugMode) {
            System.out.println("\n=== Lexer Output ===");
            dumpLexer(sourceFile);
        }

        try (InputStream inputStream = new FileInputStream(sourceFile)) {
            // Create the compiler context and set the input stream
            CompilerContext context = new CompilerContext();
            context.setInputStream(inputStream);

            // Create the compiler pass orchestrator and add passes
            CompilerOrchestrator orchestrator = new CompilerOrchestrator();

            // Example passes (to be implemented by students) Note: you may choose to separate
            // your passes differently. E.g. you may make a LexerParserPass class because that makes more
            // sense to you.
            // orchestrator.addPass(new LexicalAnalysisPass());
            // orchestrator.addPass(new SyntaxAnalysisPass());
            // orchestrator.addPass(new TypeCheckingPass());
            // orchestrator.addPass(new CodeGenerationPass());
            orchestrator.addPass(new FrontEndPass());
            orchestrator.addPass(new SymbolTableBuilderPass());
            orchestrator.addPass(new TypeCheckingPass());
            orchestrator.addPass(new TACConversionPass());
            orchestrator.addPass(new BytecodeGeneratorPass());

            // Run the compiler passes
            orchestrator.runPasses(context);

            if (debugMode) {
                System.out.println("\n=== Parser Output (AST) ===");
                dumpParser(context);

                System.out.println("\n=== Semantic Output (Symbol Table) ===");
                dumpSemantic(context);

                System.out.println("\n=== IR Output (TAC) ===");
                dumpIR(context);

                System.out.println("\n=== Bytecode Output ===");
                dumpBytecode(context);
            }

            // After running passes, print diagnostics summary or success
            Diagnostics diag = context.getDiagnostics();
            System.out.println();
            if (diag.hasErrors()) {
                System.err.println("Compilation finished with errors: " + diag.getSummary());
                for (String e : diag.getErrors()) {
                    System.err.println(e);
                }
                // Exit with non-zero to indicate failure (when run outside IDE)
                System.exit(1);
            } else {
                System.out.println("Compilation successful. " + diag.getSummary());
                if (diag.hasWarnings()) {
                    for (String w : diag.getWarnings()) System.out.println(w);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dumpLexer(String sourceFile) {
        try (InputStream is = new FileInputStream(sourceFile)) {
            SimpleCharStream stream = new SimpleCharStream(is, 1, 1);
            MyParserTokenManager tm = new MyParserTokenManager(stream);
            Token t = tm.getNextToken();
            while (t.kind != MyParserConstants.EOF) {
                String name = MyParserConstants.tokenImage[t.kind];
                // tokenImage often contains quotes, e.g. "\"if\""
                System.out.println(name + ": " + t.image);
                t = tm.getNextToken();
            }
        } catch (IOException e) {
            System.err.println("Error reading file for lexer dump: " + e.getMessage());
        }
    }

    private static void dumpParser(CompilerContext context) {
        ASTNode ast = context.getAst();
        if (ast != null) {
            System.out.println(ast.toASTTestTree().prettyPrint());
        } else {
            System.out.println("(No AST produced)");
        }
    }

    private static void dumpSemantic(CompilerContext context) {
        SymbolTable table = context.getSymbolTable();
        if (table != null) {
            List<ScopeInfo> scopes = table.getScopeInfo();
            for (ScopeInfo scope : scopes) {
                System.out.println(scope);
            }
        } else {
             System.out.println("(No Symbol Table produced)");
        }
    }

    private static void dumpIR(CompilerContext context) {
        List<TACInstruction> tac = context.getTacInstructions();
        if (tac != null) {
            for (TACInstruction instr : tac) {
                System.out.println(instr);
            }
        } else {
             System.out.println("(No TAC produced)");
        }
    }

    private static void dumpBytecode(CompilerContext context) {
        // Always try to read Main.j
        printFile("Main.j");

        ASTNode ast = context.getAst();
        if (ast instanceof BlockNode) {
            BlockNode block = (BlockNode) ast;
            for (ASTNode stmt : block.getStatements()) {
                if (stmt instanceof ClassDeclNode) {
                    ClassDeclNode cls = (ClassDeclNode) stmt;
                    printFile(cls.className + ".j");
                }
            }
        }
    }

    private static void printFile(String filename) {
        File f = new File(filename);
        if (f.exists()) {
            System.out.println("--- " + filename + " ---");
            try (Scanner sc = new Scanner(f)) {
                while (sc.hasNextLine()) {
                    System.out.println(sc.nextLine());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("-------------------");
        }
    }
}
