package compiler.backend;

import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.infra.Diagnostics;
import compiler.middle.tac.TACInstruction;
import compiler.middle.tac.OpCode;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BytecodeGeneratorPass implements CompilerPass {
    @Override
    public String name() { return "BytecodeGeneratorPass"; }

    @Override
    public void execute(CompilerContext context) throws Exception {
        Diagnostics diag = context.getDiagnostics();
        List<TACInstruction> instructions = context.getTacInstructions();

        if (instructions == null) {
            diag.log("No TAC instructions found. Skipping bytecode generation.");
            return;
        }

        diag.log("Generating Bytecode from " + instructions.size() + " TAC instructions...");

        try (PrintWriter out = new PrintWriter(new FileWriter("Main.j"))) {
            // Header
            out.println(".class public Main");
            out.println(".super java/lang/Object");
            out.println();

            // Default constructor
            out.println(".method public <init>()V");
            out.println("   aload_0");
            out.println("   invokespecial java/lang/Object/<init>()V");
            out.println("   return");
            out.println(".end method");
            out.println();

            // Variable mapping: name -> index
            // We need a fresh map for each method.
            Map<String, Integer> varMap = new HashMap<>();
            int nextVarIndex = 0;

            boolean insideMethod = false;

            // Check if we have loose statements before any function?
            // If the first instruction is not FUNC_ENTRY, we are in main.
            if (!instructions.isEmpty() && instructions.get(0).op != OpCode.FUNC_ENTRY) {
                 out.println(".method public static main([Ljava/lang/String;)V");
                 out.println("   .limit stack 100");
                 out.println("   .limit locals 100");
                 insideMethod = true;
                 varMap.clear();
                 varMap.put("args", nextVarIndex++);
            }

            for (TACInstruction instr : instructions) {
                switch(instr.op) {
                    case FUNC_ENTRY:
                        if (insideMethod) {
                            out.println("   return"); // Safety return if fell through
                            out.println(".end method");
                            out.println();
                        }

                        String methodName = instr.target;
                        int paramCount = Integer.parseInt(instr.arg1);

                        // Heuristic: If method name contains "_", it might be Class_Method (mangled).
                        // In TAC, we emitted "Class_Method" for class methods.
                        // But we want to generate valid JVM methods.
                        // Ideally, we'd emit separate classes. But we are dumping everything to Main.j.
                        // So we make them static methods of Main?
                        // "public static Class_Method(...)V"
                        // This works for simple execution.

                        String sig = "()V";
                        if (methodName.equals("main")) {
                             sig = "([Ljava/lang/String;)V";
                        } else {
                            StringBuilder sb = new StringBuilder("(");
                            for(int k=0; k<paramCount; k++) sb.append("I"); // Default to int params
                            sb.append(")I"); // Default to int return
                            sig = sb.toString();
                        }

                        out.println(".method public static " + methodName + sig);
                        out.println("   .limit stack 100");
                        out.println("   .limit locals 100");

                        insideMethod = true;
                        varMap.clear();
                        nextVarIndex = 0;
                        break;

                    case PARAM_DECL:
                        // Map param name to next index
                        if (!varMap.containsKey(instr.target)) {
                            varMap.put(instr.target, nextVarIndex++);
                        }
                        break;

                    case FUNC_EXIT:
                        if (insideMethod) {
                            out.println(".end method");
                            out.println();
                            insideMethod = false;
                        }
                        break;

                    case LABEL:
                        out.println(instr.target + ":");
                        break;

                    case LOAD_CONST:
                        // Check type of const?
                        // If number -> ldc/bipush
                        // If string -> ldc
                        try {
                            int val = Integer.parseInt(instr.arg1);
                            if (val >= -128 && val <= 127) out.println("   bipush " + val);
                            else if (val >= -32768 && val <= 32767) out.println("   sipush " + val);
                            else out.println("   ldc " + val);
                        } catch (NumberFormatException e) {
                            if (instr.arg1.equals("true")) out.println("   iconst_1");
                            else if (instr.arg1.equals("false")) out.println("   iconst_0");
                            else if (instr.arg1.equals("null")) out.println("   aconst_null");
                            else out.println("   ldc " + instr.arg1); // String
                        }

                        // Store to target?
                        // LOAD_CONST puts on stack.
                        // But TAC says t0 = 5.
                        // So we should store to t0.
                        storeVar(out, instr.target, varMap);
                        break;

                    case LOAD_VAR:
                        loadVar(out, instr.arg1, varMap);
                        storeVar(out, instr.target, varMap);
                        break;

                    case STORE_VAR:
                        // target = arg1
                        // load arg1, store target
                        loadVar(out, instr.arg1, varMap);
                        storeVar(out, instr.target, varMap);
                        break;

                    case ADD:
                        loadVar(out, instr.arg1, varMap);
                        loadVar(out, instr.arg2, varMap);
                        out.println("   iadd");
                        storeVar(out, instr.target, varMap);
                        break;
                    case SUB:
                        loadVar(out, instr.arg1, varMap);
                        loadVar(out, instr.arg2, varMap);
                        out.println("   isub");
                        storeVar(out, instr.target, varMap);
                        break;
                    case MUL:
                        loadVar(out, instr.arg1, varMap);
                        loadVar(out, instr.arg2, varMap);
                        out.println("   imul");
                        storeVar(out, instr.target, varMap);
                        break;
                    case DIV:
                        loadVar(out, instr.arg1, varMap);
                        loadVar(out, instr.arg2, varMap);
                        out.println("   idiv");
                        storeVar(out, instr.target, varMap);
                        break;

                    case RETURN:
                        if (instr.target != null) {
                            loadVar(out, instr.target, varMap);
                            out.println("   ireturn"); // Assume int return
                        } else {
                            out.println("   return");
                        }
                        break;

                    case IFZ:
                        loadVar(out, instr.target, varMap);
                        out.println("   ifeq " + instr.arg1);
                        break;

                    case GOTO:
                        out.println("   goto " + instr.target);
                        break;

                    case PARAM:
                        // Pushing param for call.
                        // JVM expects params on stack.
                        loadVar(out, instr.target, varMap);
                        break;

                    case CALL: // Deprecated, but handle if present
                    case CALL_STATIC:
                    case CALL_VIRTUAL:
                        String obj = instr.arg1;
                        // Parse name and count
                        String rawName = instr.arg2;
                        String mName = rawName;
                        int argCount = 0;
                        if (rawName.contains(":")) {
                            String[] parts = rawName.split(":");
                            mName = parts[0];
                            argCount = Integer.parseInt(parts[1]);
                        }

                        // Construct signature with argCount
                        StringBuilder callSig = new StringBuilder("(");
                        for(int k=0; k<argCount; k++) callSig.append("I");
                        callSig.append(")I");

                        out.println("   invokestatic Main/" + mName + callSig.toString());
                        storeVar(out, instr.target, varMap);
                        break;

                    case GET_FIELD:
                        // getfield Class/Field Type
                        // We don't have types.
                        // Assume int?
                        // Also, if we are using Main class for everything, we need fields in Main?
                        // `getfield Main/fieldName I`
                        loadVar(out, instr.arg1, varMap); // obj
                        out.println("   getfield Main/" + instr.arg2 + " I");
                        storeVar(out, instr.target, varMap);
                        break;

                    case PUT_FIELD:
                        // TAC: PUT_FIELD target=obj, arg1=fieldName, arg2=value
                        loadVar(out, instr.target, varMap); // obj
                        loadVar(out, instr.arg2, varMap);   // val
                        out.println("   putfield Main/" + instr.arg1 + " I");
                        break;

                    case AND:
                        loadVar(out, instr.arg1, varMap);
                        loadVar(out, instr.arg2, varMap);
                        out.println("   iand");
                        storeVar(out, instr.target, varMap);
                        break;
                    case OR:
                        loadVar(out, instr.arg1, varMap);
                        loadVar(out, instr.arg2, varMap);
                        out.println("   ior");
                        storeVar(out, instr.target, varMap);
                        break;
                    case NOT:
                        // xor 1
                        loadVar(out, instr.arg1, varMap);
                        out.println("   iconst_1");
                        out.println("   ixor");
                        storeVar(out, instr.target, varMap);
                        break;
                    case NEG:
                        loadVar(out, instr.arg1, varMap);
                        out.println("   ineg");
                        storeVar(out, instr.target, varMap);
                        break;

                    case NEW:
                        out.println("   new " + instr.arg1);
                        out.println("   dup");
                        out.println("   invokespecial " + instr.arg1 + "/<init>()V");
                        storeVar(out, instr.target, varMap);
                        break;

                    // ... Implement others EQ, NEQ, etc using jumps or logic ...
                    case EQ:
                    case NEQ:
                    case LT:
                    case GT:
                    case LE:
                    case GE:
                         // Implement comparison
                         // load op1, load op2
                         // if_icmpXX L_true
                         // iconst_0
                         // goto L_end
                         // L_true: iconst_1
                         // L_end: store
                         genCompare(out, instr, varMap);
                         break;

                    default:
                        // out.println("; TODO: " + instr.op);
                        break;
                }
            }

            if (insideMethod) {
                out.println("   return");
                out.println(".end method");
            }
        }
    }

    private void genCompare(PrintWriter out, TACInstruction instr, Map<String, Integer> varMap) {
        loadVar(out, instr.arg1, varMap);
        loadVar(out, instr.arg2, varMap);
        String labelTrue = "True" + System.nanoTime();
        String labelEnd = "End" + System.nanoTime();

        String jump = switch(instr.op) {
            case EQ -> "if_icmpeq";
            case NEQ -> "if_icmpne";
            case LT -> "if_icmplt";
            case GE -> "if_icmpge";
            case GT -> "if_icmpgt";
            case LE -> "if_icmple";
            default -> "nop";
        };

        out.println("   " + jump + " " + labelTrue);
        out.println("   iconst_0");
        out.println("   goto " + labelEnd);
        out.println(labelTrue + ":");
        out.println("   iconst_1");
        out.println(labelEnd + ":");
        storeVar(out, instr.target, varMap);
    }

    private void loadVar(PrintWriter out, String name, Map<String, Integer> varMap) {
        if (!varMap.containsKey(name)) {
            varMap.put(name, varMap.size());
        }
        int idx = varMap.get(name);
        out.println("   iload " + idx);
    }

    private void storeVar(PrintWriter out, String name, Map<String, Integer> varMap) {
        if (!varMap.containsKey(name)) {
            varMap.put(name, varMap.size());
        }
        int idx = varMap.get(name);
        out.println("   istore " + idx);
    }
}
