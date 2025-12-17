package compiler.backend;

import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.middle.tac.TACInstruction;
import compiler.middle.tac.OpCode;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

public class BytecodeGeneratorPass implements CompilerPass {

    @Override
    public String name() {
        return "Bytecode Generation";
    }

    // Simple mapping from var/temp name to local index
    private Map<String, Integer> localMap = new HashMap<>();
    private int nextLocal = 0;

    private int getLocalIndex(String name) {
        if (!localMap.containsKey(name)) {
            localMap.put(name, nextLocal++);
        }
        return localMap.get(name);
    }

    @Override
    public void execute(CompilerContext context) throws Exception {
        List<TACInstruction> instrs = context.getTACInstructions();
        if (instrs == null) {
            return;
        }

        StringBuilder jasminCode = new StringBuilder();

        // Standard Jasmin header
        jasminCode.append(".class public Main\n");
        jasminCode.append(".super java/lang/Object\n\n");

        // Default constructor
        jasminCode.append(".method public <init>()V\n");
        jasminCode.append("   aload_0\n");
        jasminCode.append("   invokenonvirtual java/lang/Object/<init>()V\n");
        jasminCode.append("   return\n");
        jasminCode.append(".end method\n\n");

        boolean inMethod = false;
        localMap.clear();
        nextLocal = 0; // Reset for each method (simplified)

        // If no FUNC_START found at beginning, assume main
        if (!instrs.isEmpty() && instrs.get(0).op != OpCode.FUNC_START) {
            jasminCode.append(".method public static main([Ljava/lang/String;)V\n");
            jasminCode.append("   .limit stack 100\n");
            jasminCode.append("   .limit locals 100\n");
            inMethod = true;
        }

        for (TACInstruction inst : instrs) {
            switch (inst.op) {
                case FUNC_START:
                    if (inMethod) {
                         jasminCode.append(".end method\n\n");
                    }
                    String methodName = inst.target; // target holds name
                    String sig = "()V"; // Default signature
                    if (methodName != null && methodName.equals("main")) sig = "([Ljava/lang/String;)V";

                    jasminCode.append(".method public static ").append(methodName).append(sig).append("\n");
                    jasminCode.append("   .limit stack 100\n");
                    jasminCode.append("   .limit locals 100\n");
                    inMethod = true;
                    localMap.clear();
                    nextLocal = 0;
                    break;

                case FUNC_END:
                    if (inMethod) {
                        jasminCode.append("   return\n"); // Ensure return
                        jasminCode.append(".end method\n\n");
                        inMethod = false;
                    }
                    break;

                case LOAD_CONST:
                    jasminCode.append("   ldc ").append(inst.arg1).append("\n");
                    jasminCode.append("   istore ").append(getLocalIndex(inst.target)).append("\n");
                    break;

                case STORE_VAR:
                    if (inst.arg1 != null) {
                        jasminCode.append("   iload ").append(getLocalIndex(inst.arg1)).append("\n");
                        jasminCode.append("   istore ").append(getLocalIndex(inst.target)).append("\n");
                    }
                    break;

                case ADD:
                    jasminCode.append("   iload ").append(getLocalIndex(inst.arg1)).append("\n");
                    jasminCode.append("   iload ").append(getLocalIndex(inst.arg2)).append("\n");
                    jasminCode.append("   iadd\n");
                    jasminCode.append("   istore ").append(getLocalIndex(inst.target)).append("\n");
                    break;
                case SUB:
                    jasminCode.append("   iload ").append(getLocalIndex(inst.arg1)).append("\n");
                    jasminCode.append("   iload ").append(getLocalIndex(inst.arg2)).append("\n");
                    jasminCode.append("   isub\n");
                    jasminCode.append("   istore ").append(getLocalIndex(inst.target)).append("\n");
                    break;
                case MUL:
                    jasminCode.append("   iload ").append(getLocalIndex(inst.arg1)).append("\n");
                    jasminCode.append("   iload ").append(getLocalIndex(inst.arg2)).append("\n");
                    jasminCode.append("   imul\n");
                    jasminCode.append("   istore ").append(getLocalIndex(inst.target)).append("\n");
                    break;
                case DIV:
                    jasminCode.append("   iload ").append(getLocalIndex(inst.arg1)).append("\n");
                    jasminCode.append("   iload ").append(getLocalIndex(inst.arg2)).append("\n");
                    jasminCode.append("   idiv\n");
                    jasminCode.append("   istore ").append(getLocalIndex(inst.target)).append("\n");
                    break;

                case RETURN:
                    if (inst.target != null) {
                         jasminCode.append("   iload ").append(getLocalIndex(inst.target)).append("\n");
                         jasminCode.append("   ireturn\n");
                    } else {
                         jasminCode.append("   return\n");
                    }
                    break;

                case CALL:
                    jasminCode.append("   invokestatic Main/").append(inst.arg1).append("()I\n");
                    if (inst.target != null) {
                        jasminCode.append("   istore ").append(getLocalIndex(inst.target)).append("\n");
                    } else {
                        jasminCode.append("   pop\n");
                    }
                    break;

                case PARAM:
                    jasminCode.append("   iload ").append(getLocalIndex(inst.target)).append("\n");
                    break;

                default:
                    jasminCode.append("   ; ").append(inst).append("\n");
            }
        }

        if (inMethod) {
            jasminCode.append("   return\n");
            jasminCode.append(".end method\n");
        }

        String code = jasminCode.toString();
        System.out.println("Generated Bytecode:\n" + code);

        try (PrintWriter out = new PrintWriter(new FileWriter("Main.j"))) {
            out.println(code);
        } catch (IOException e) {
            System.err.println("Failed to write Main.j: " + e.getMessage());
        }
    }
}
