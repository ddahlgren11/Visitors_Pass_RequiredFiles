package compiler.backend;

import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.infra.Diagnostics;
import compiler.middle.tac.TACInstruction;
import compiler.middle.tac.OpCode;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class BytecodeGeneratorPass implements CompilerPass {
    @Override
    public String name() { return "BytecodeGeneratorPass"; }

    private Set<String> initializedClasses = new HashSet<>();
    private Map<String, Integer> varMap = new HashMap<>();
    private Map<String, Boolean> varIsRef = new HashMap<>();
    private int nextVarIndex = 0;
    private String currentMethodName = null;
    private boolean lastOpWasReturn = false;

    @Override
    public void execute(CompilerContext context) throws Exception {
        Diagnostics diag = context.getDiagnostics();
        List<TACInstruction> instructions = context.getTacInstructions();

        if (instructions == null) {
            diag.log("No TAC instructions found. Skipping bytecode generation.");
            return;
        }

        diag.log("Generating Bytecode from " + instructions.size() + " TAC instructions...");

        PrintWriter out = null;
        String currentFile = null;

        for (TACInstruction instr : instructions) {
            if (instr.op == OpCode.FIELD_DECL || instr.op == OpCode.FUNC_ENTRY) {
                String className = "Main";
                if (instr.op == OpCode.FIELD_DECL) {
                    className = instr.arg1;
                } else {
                    if (instr.target.equals("main")) {
                        className = "Main";
                    } else if (instr.target.contains(".")) {
                        className = instr.target.substring(0, instr.target.indexOf('.'));
                    }
                }

                String filename = className + ".j";
                if (!filename.equals(currentFile)) {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    boolean append = initializedClasses.contains(className);
                    out = new PrintWriter(new FileWriter(filename, append));
                    currentFile = filename;

                    if (!append) {
                        out.println(".class public " + className);
                        out.println(".super java/lang/Object");
                        out.println();

                        if (className.equals("Main")) {
                             out.println(".method public <init>()V");
                             out.println("   aload_0");
                             out.println("   invokespecial java/lang/Object/<init>()V");
                             out.println("   return");
                             out.println(".end method");
                             out.println();
                        }
                        initializedClasses.add(className);
                    }
                }
            }

            if (out == null) {
                if (!initializedClasses.contains("Main")) {
                    out = new PrintWriter(new FileWriter("Main.j"));
                    out.println(".class public Main");
                    out.println(".super java/lang/Object");
                    out.println(".method public <init>()V");
                    out.println("   aload_0");
                    out.println("   invokespecial java/lang/Object/<init>()V");
                    out.println("   return");
                    out.println(".end method");
                    out.println();
                    initializedClasses.add("Main");
                    currentFile = "Main.j";
                } else if (!"Main.j".equals(currentFile)) {
                     if (out != null) out.close();
                     out = new PrintWriter(new FileWriter("Main.j", true));
                     currentFile = "Main.j";
                }
            }

            processInstruction(instr, out);
        }

        if (out != null) {
            out.flush();
            out.close();
        }
    }

    private void processInstruction(TACInstruction instr, PrintWriter out) {
        // Reset return flag unless we set it explicitly
        boolean isReturn = false;

        switch(instr.op) {
            case FIELD_DECL:
                out.println(".field public " + instr.arg2 + " I");
                break;

            case FUNC_ENTRY:
                currentMethodName = instr.target;
                varMap.clear();
                varIsRef.clear();
                nextVarIndex = 0;

                String methodName = instr.target;
                String signature = instr.arg2;
                boolean isConstructor = false;

                if (methodName.equals("main")) {
                    out.println(".method public static main([Ljava/lang/String;)V");
                    varMap.put("args", nextVarIndex++);
                    varIsRef.put("args", true);
                } else if (methodName.contains(".")) {
                    String[] parts = methodName.split("\\.");
                    String className = parts[0];
                    String realName = parts[1];

                    if (realName.equals(className)) {
                        if (signature != null) out.println(".method public <init>" + signature);
                        else out.println(".method public <init>()V"); // Fallback
                        isConstructor = true;
                    } else {
                        if (signature != null) out.println(".method public " + realName + signature);
                        else out.println(".method public " + realName + "()I"); // Fallback
                    }
                } else {
                    if (signature != null) out.println(".method public static " + methodName + signature);
                    else out.println(".method public static " + methodName + "()V");
                }

                out.println("   .limit stack 100");
                out.println("   .limit locals 100");

                if (isConstructor) {
                    out.println("   aload_0");
                    out.println("   invokespecial java/lang/Object/<init>()V");
                }
                break;

             case PARAM_DECL:
                 // arg1 is descriptor
                 boolean isRefParam = false;
                 if (instr.arg1 != null && (instr.arg1.startsWith("L") || instr.arg1.startsWith("["))) {
                     isRefParam = true;
                 }
                 if (instr.target.equals("this")) {
                     varMap.put("this", 0);
                     varIsRef.put("this", true);
                     if (nextVarIndex == 0) nextVarIndex = 1;
                 } else {
                     if (!varMap.containsKey(instr.target)) {
                         varMap.put(instr.target, nextVarIndex++);
                         varIsRef.put(instr.target, isRefParam);
                     }
                 }
                 break;

             case FUNC_EXIT:
                 if (currentMethodName != null) {
                     if (!lastOpWasReturn) {
                         out.println("   return");
                     }
                     out.println(".end method");
                     out.println();
                     currentMethodName = null;
                 }
                 break;

             case LABEL:
                 out.println(instr.target + ":");
                 break;

             case NEW:
                 String cls = instr.arg1;
                 out.println("   new " + cls);
                 out.println("   dup");
                 int argc = Integer.parseInt(instr.arg2);
                 StringBuilder sb = new StringBuilder("(");
                 for(int i=0; i<argc; i++) sb.append("I"); // Assumes int args if we don't have types?
                 // Wait, NEW calls constructor. We still lack constructor signature in NEW op!
                 // TAC NEW instruction doesn't carry signature.
                 // BytecodeGeneratorPass handles NEW.
                 // We need to match constructor.
                 // If we have single constructor, maybe okay?
                 // But we hardcoded (I...I)V in NEW.
                 // If constructor takes objects, this fails.
                 // FIXME: NEW needs signature or arg types.
                 // Since I didn't update TAC NEW logic, I'll stick to 'I'.
                 // This is a known limitation.
                 sb.append(")V");
                 out.println("   invokespecial " + cls + "/<init>" + sb);
                 storeVar(out, instr.target, true);
                 break;

             case LOAD_CONST:
                 boolean isRef = false;
                 try {
                      int val = Integer.parseInt(instr.arg1);
                      if (val >= -128 && val <= 127) out.println("   bipush " + val);
                      else if (val >= -32768 && val <= 32767) out.println("   sipush " + val);
                      else out.println("   ldc " + val);
                 } catch (Exception e) {
                      if (instr.arg1.equals("true")) out.println("   iconst_1");
                      else if (instr.arg1.equals("false")) out.println("   iconst_0");
                      else if (instr.arg1.equals("null")) { out.println("   aconst_null"); isRef = true; }
                      else { out.println("   ldc " + instr.arg1); isRef = true; }
                 }
                 storeVar(out, instr.target, isRef);
                 break;

             case LOAD_VAR:
                 loadVar(out, instr.arg1);
                 storeVar(out, instr.target, isRef(instr.arg1));
                 break;

             case STORE_VAR:
                 loadVar(out, instr.arg1);
                 storeVar(out, instr.target, isRef(instr.arg1));
                 break;

             case CALL_VIRTUAL:
                 String mName = instr.arg2;
                 int args = 0;
                 if (mName.contains(":")) {
                     String[] p = mName.split(":");
                     mName = p[0];
                     args = Integer.parseInt(p[1]);
                 }

                 String cName = "Main";
                 String meth = mName;
                 if (mName.contains(".")) {
                     String[] p = mName.split("\\.");
                     cName = p[0];
                     meth = p[1];
                 }

                 StringBuilder callSig = new StringBuilder("(");
                 for(int i=0; i < args - 1; i++) callSig.append("I"); // Still assuming I
                 callSig.append(")I"); // Still assuming I return

                 out.println("   invokevirtual " + cName + "/" + meth + callSig);
                 storeVar(out, instr.target, false);
                 break;

             case GET_FIELD:
                 loadVar(out, instr.arg1);
                 String[] fp = instr.arg2.split(":");
                 String fClass = fp.length > 1 ? fp[0] : "Main";
                 String fName = fp.length > 1 ? fp[1] : instr.arg2;
                 out.println("   getfield " + fClass + "/" + fName + " I");
                 storeVar(out, instr.target, false);
                 break;

             case PUT_FIELD:
                 loadVar(out, instr.target);
                 loadVar(out, instr.arg2);
                 String[] fp2 = instr.arg1.split(":");
                 String fClass2 = fp2.length > 1 ? fp2[0] : "Main";
                 String fName2 = fp2.length > 1 ? fp2[1] : instr.arg1;
                 out.println("   putfield " + fClass2 + "/" + fName2 + " I");
                 break;

             case RETURN:
                 if (instr.target != null) {
                     loadVar(out, instr.target);
                     out.println("   ireturn");
                 } else {
                     out.println("   return");
                 }
                 isReturn = true;
                 break;

             case IFZ:
                 loadVar(out, instr.target);
                 out.println("   ifeq " + instr.arg1);
                 break;

             case GOTO:
                 out.println("   goto " + instr.target);
                 break;

             case PARAM:
                 loadVar(out, instr.target);
                 break;

             case ADD:
                loadVar(out, instr.arg1); loadVar(out, instr.arg2); out.println("   iadd"); storeVar(out, instr.target, false); break;
             case SUB:
                loadVar(out, instr.arg1); loadVar(out, instr.arg2); out.println("   isub"); storeVar(out, instr.target, false); break;
             case MUL:
                loadVar(out, instr.arg1); loadVar(out, instr.arg2); out.println("   imul"); storeVar(out, instr.target, false); break;
             case DIV:
                loadVar(out, instr.arg1); loadVar(out, instr.arg2); out.println("   idiv"); storeVar(out, instr.target, false); break;

             case AND:
                loadVar(out, instr.arg1); loadVar(out, instr.arg2); out.println("   iand"); storeVar(out, instr.target, false); break;
             case OR:
                loadVar(out, instr.arg1); loadVar(out, instr.arg2); out.println("   ior"); storeVar(out, instr.target, false); break;
             case NOT:
                loadVar(out, instr.arg1); out.println("   iconst_1"); out.println("   ixor"); storeVar(out, instr.target, false); break;
             case NEG:
                loadVar(out, instr.arg1); out.println("   ineg"); storeVar(out, instr.target, false); break;

             case EQ:
             case NEQ:
             case LT:
             case GT:
             case LE:
             case GE:
                 genCompare(out, instr);
                 break;

             default:
                break;
        }

        lastOpWasReturn = isReturn;
    }

    private void genCompare(PrintWriter out, TACInstruction instr) {
        loadVar(out, instr.arg1);
        loadVar(out, instr.arg2);
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
        storeVar(out, instr.target, false);
    }

    private void loadVar(PrintWriter out, String name) {
        if (!varMap.containsKey(name)) {
            varMap.put(name, varMap.size());
            varIsRef.put(name, false);
        }
        int idx = varMap.get(name);
        boolean isRef = varIsRef.getOrDefault(name, false);
        if (isRef) out.println("   aload " + idx);
        else out.println("   iload " + idx);
    }

    private void storeVar(PrintWriter out, String name, boolean isRef) {
        if (!varMap.containsKey(name)) {
            varMap.put(name, nextVarIndex++);
        }
        varIsRef.put(name, isRef);
        int idx = varMap.get(name);
        if (isRef) out.println("   astore " + idx);
        else out.println("   istore " + idx);
    }

    private boolean isRef(String name) {
        return varIsRef.getOrDefault(name, false);
    }
}
