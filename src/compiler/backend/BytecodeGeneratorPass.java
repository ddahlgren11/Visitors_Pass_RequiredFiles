package compiler.backend;

import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.middle.tac.TACInstruction;
import compiler.middle.tac.OpCode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class BytecodeGeneratorPass implements CompilerPass {
    @Override
    public String name() { return "BytecodeGeneratorPass"; }

    private Map<String, Integer> varMap = new HashMap<>();
    private Map<String, Boolean> varIsRef = new HashMap<>();
    private int nextVarIndex = 0;
    private Map<String, Label> labelMap = new HashMap<>();

    @Override
    public void execute(CompilerContext context) throws Exception {
        List<TACInstruction> instructions = context.getTacInstructions();
        if (instructions == null) return;

        Map<String, List<TACInstruction>> classInstructions = new LinkedHashMap<>();
        String currentClass = "Main";

        for (TACInstruction instr : instructions) {
            if (instr.op == OpCode.FIELD_DECL) {
                currentClass = instr.arg1;
            } else if (instr.op == OpCode.FUNC_ENTRY) {
                String target = instr.target;
                if (target.contains(".")) {
                    currentClass = target.substring(0, target.lastIndexOf('.'));
                } else if (!target.equals("main")) {
                    currentClass = "Main";
                } else {
                    currentClass = "Main";
                }
            }
            classInstructions.computeIfAbsent(currentClass, k -> new ArrayList<>()).add(instr);
        }

        for (String className : classInstructions.keySet()) {
            generateClass(className, classInstructions.get(className));
        }
    }

    private void generateClass(String className, List<TACInstruction> instrs) throws IOException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);

        boolean hasInit = false;
        for (TACInstruction instr : instrs) {
            if (instr.op == OpCode.FUNC_ENTRY && instr.target.endsWith(".<init>")) {
                hasInit = true;
                break;
            }
        }

        if (!hasInit && !className.equals("Main")) {
             MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
             mv.visitCode();
             mv.visitVarInsn(Opcodes.ALOAD, 0);
             mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
             mv.visitInsn(Opcodes.RETURN);
             mv.visitMaxs(0, 0);
             mv.visitEnd();
        } else if (className.equals("Main") && !hasInit) {
             MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
             mv.visitCode();
             mv.visitVarInsn(Opcodes.ALOAD, 0);
             mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
             mv.visitInsn(Opcodes.RETURN);
             mv.visitMaxs(0, 0);
             mv.visitEnd();
        }

        for (TACInstruction instr : instrs) {
            if (instr.op == OpCode.FIELD_DECL) {
                cw.visitField(Opcodes.ACC_PUBLIC, instr.arg2, "I", null, null).visitEnd();
            }
        }

        List<TACInstruction> methodInstrs = new ArrayList<>();
        boolean insideMethod = false;

        for (TACInstruction instr : instrs) {
            if (instr.op == OpCode.FUNC_ENTRY) {
                if (insideMethod) {
                    methodInstrs.clear();
                }
                methodInstrs.add(instr);
                insideMethod = true;
            } else if (instr.op == OpCode.FUNC_EXIT) {
                methodInstrs.add(instr);
                generateMethod(cw, className, methodInstrs);
                methodInstrs.clear();
                insideMethod = false;
            } else if (insideMethod) {
                methodInstrs.add(instr);
            }
        }

        cw.visitEnd();
        try (FileOutputStream fos = new FileOutputStream(className + ".class")) {
            fos.write(cw.toByteArray());
        }
    }

    private void generateMethod(ClassWriter cw, String className, List<TACInstruction> instrs) {
        TACInstruction entry = instrs.get(0);
        String methodName = entry.target;
        String signature = entry.arg2;

        String simpleName = methodName;
        if (methodName.contains(".")) {
             simpleName = methodName.substring(methodName.lastIndexOf('.') + 1);
        }

        int access = Opcodes.ACC_PUBLIC;
        if (simpleName.equals("main")) access |= Opcodes.ACC_STATIC;

        if (signature == null) signature = "()V";

        MethodVisitor mv = cw.visitMethod(access, simpleName, signature, null, null);
        mv.visitCode();

        varMap.clear();
        varIsRef.clear();
        nextVarIndex = 0;
        labelMap.clear();

        for (TACInstruction i : instrs) {
            if (i.op == OpCode.LABEL) {
                labelMap.put(i.target, new Label());
            }
        }

        for (TACInstruction instr : instrs) {
            switch (instr.op) {
                case PARAM_DECL:
                    getVarIndex(instr.target);
                    if (instr.arg1 != null && (instr.arg1.startsWith("L") || instr.arg1.startsWith("["))) {
                        varIsRef.put(instr.target, true);
                    } else if (instr.target.equals("this")) {
                        varIsRef.put("this", true);
                    }
                    break;
                case LABEL:
                    mv.visitLabel(getLabel(instr.target));
                    break;
                case LOAD_CONST:
                    visitLdc(mv, instr.arg1);
                    storeVar(mv, instr.target, isRefType(instr.arg1));
                    break;
                case LOAD_VAR:
                    loadVar(mv, instr.arg1);
                    storeVar(mv, instr.target, isRef(instr.arg1));
                    break;
                case STORE_VAR:
                    loadVar(mv, instr.arg1);
                    storeVar(mv, instr.target, isRef(instr.arg1));
                    break;
                case ADD:
                    loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); mv.visitInsn(Opcodes.IADD); storeVar(mv, instr.target, false); break;
                case SUB:
                    loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); mv.visitInsn(Opcodes.ISUB); storeVar(mv, instr.target, false); break;
                case MUL:
                    loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); mv.visitInsn(Opcodes.IMUL); storeVar(mv, instr.target, false); break;
                case DIV:
                    loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); mv.visitInsn(Opcodes.IDIV); storeVar(mv, instr.target, false); break;
                case AND:
                    loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); mv.visitInsn(Opcodes.IAND); storeVar(mv, instr.target, false); break;
                case OR:
                    loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); mv.visitInsn(Opcodes.IOR); storeVar(mv, instr.target, false); break;
                case NOT:
                    loadVar(mv, instr.arg1); mv.visitInsn(Opcodes.ICONST_1); mv.visitInsn(Opcodes.IXOR); storeVar(mv, instr.target, false); break;
                case NEG:
                    loadVar(mv, instr.arg1); mv.visitInsn(Opcodes.INEG); storeVar(mv, instr.target, false); break;

                case PARAM:
                    loadVar(mv, instr.target);
                    break;

                case CALL_VIRTUAL:
                    String mName = instr.arg2;
                    String callSig = "()I";
                    if (mName.contains(":")) {
                        String[] p = mName.split(":");
                        mName = p[0];
                        if (p.length > 1) callSig = p[1];
                    }
                    String cName = "Main";
                    String meth = mName;
                    if (mName.contains(".")) {
                        String[] p = mName.split("\\.");
                        cName = p[0];
                        meth = p[1];
                    }
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, cName, meth, callSig, false);
                    String retType = callSig.substring(callSig.lastIndexOf(')') + 1);
                    if (!retType.equals("V")) {
                         boolean isRetRef = retType.startsWith("L") || retType.startsWith("[");
                         storeVar(mv, instr.target, isRetRef);
                    }
                    break;

                case NEW_ALLOC:
                    mv.visitTypeInsn(Opcodes.NEW, instr.arg1);
                    mv.visitInsn(Opcodes.DUP);
                    break;

                case NEW_CONSTRUCT:
                     String clsCons = instr.arg1;
                     String sigCons = instr.arg2;
                     if (sigCons == null) sigCons = "()V";
                     mv.visitMethodInsn(Opcodes.INVOKESPECIAL, clsCons, "<init>", sigCons, false);
                     storeVar(mv, instr.target, true);
                     break;

                case GET_FIELD:
                     loadVar(mv, instr.arg1);
                     String[] fp = instr.arg2.split(":");
                     String fClass = fp.length > 1 ? fp[0] : "Main";
                     String fName = fp.length > 1 ? fp[1] : instr.arg2;
                     mv.visitFieldInsn(Opcodes.GETFIELD, fClass, fName, "I");
                     storeVar(mv, instr.target, false);
                     break;

                case PUT_FIELD:
                     loadVar(mv, instr.target);
                     loadVar(mv, instr.arg2);
                     String[] fp2 = instr.arg1.split(":");
                     String fClass2 = fp2.length > 1 ? fp2[0] : "Main";
                     String fName2 = fp2.length > 1 ? fp2[1] : instr.arg1;
                     mv.visitFieldInsn(Opcodes.PUTFIELD, fClass2, fName2, "I");
                     break;

                case RETURN:
                    if (instr.target != null) {
                         loadVar(mv, instr.target);
                         mv.visitInsn(Opcodes.IRETURN);
                    } else {
                         mv.visitInsn(Opcodes.RETURN);
                    }
                    break;

                case IFZ:
                    loadVar(mv, instr.target);
                    mv.visitJumpInsn(Opcodes.IFEQ, getLabel(instr.arg1));
                    break;

                case GOTO:
                    mv.visitJumpInsn(Opcodes.GOTO, getLabel(instr.target));
                    break;

                case EQ: loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); genCompare(mv, Opcodes.IF_ICMPEQ, instr.target); break;
                case NEQ: loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); genCompare(mv, Opcodes.IF_ICMPNE, instr.target); break;
                case LT: loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); genCompare(mv, Opcodes.IF_ICMPLT, instr.target); break;
                case GE: loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); genCompare(mv, Opcodes.IF_ICMPGE, instr.target); break;
                case GT: loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); genCompare(mv, Opcodes.IF_ICMPGT, instr.target); break;
                case LE: loadVar(mv, instr.arg1); loadVar(mv, instr.arg2); genCompare(mv, Opcodes.IF_ICMPLE, instr.target); break;
            }
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void genCompare(MethodVisitor mv, int opcode, String target) {
        Label lTrue = new Label();
        Label lEnd = new Label();
        mv.visitJumpInsn(opcode, lTrue);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitJumpInsn(Opcodes.GOTO, lEnd);
        mv.visitLabel(lTrue);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitLabel(lEnd);
        storeVar(mv, target, false);
    }

    private void visitLdc(MethodVisitor mv, String val) {
         try {
              int v = Integer.parseInt(val);
              if (v >= -1 && v <= 5) mv.visitInsn(Opcodes.ICONST_0 + v);
              else if (v >= -128 && v <= 127) mv.visitIntInsn(Opcodes.BIPUSH, v);
              else if (v >= -32768 && v <= 32767) mv.visitIntInsn(Opcodes.SIPUSH, v);
              else mv.visitLdcInsn(v);
         } catch (Exception e) {
              if (val.equals("true")) mv.visitInsn(Opcodes.ICONST_1);
              else if (val.equals("false")) mv.visitInsn(Opcodes.ICONST_0);
              else if (val.equals("null")) mv.visitInsn(Opcodes.ACONST_NULL);
              else mv.visitLdcInsn(val);
         }
    }

    private Label getLabel(String name) {
        return labelMap.computeIfAbsent(name, k -> new Label());
    }

    private int getVarIndex(String name) {
        if (!varMap.containsKey(name)) {
            varMap.put(name, nextVarIndex++);
        }
        return varMap.get(name);
    }

    private void loadVar(MethodVisitor mv, String name) {
        int idx = getVarIndex(name);
        boolean isRef = varIsRef.getOrDefault(name, false);
        mv.visitVarInsn(isRef ? Opcodes.ALOAD : Opcodes.ILOAD, idx);
    }

    private void storeVar(MethodVisitor mv, String name, boolean isRef) {
        int idx = getVarIndex(name);
        varIsRef.put(name, isRef);
        mv.visitVarInsn(isRef ? Opcodes.ASTORE : Opcodes.ISTORE, idx);
    }

    private boolean isRefType(String val) {
         try { Integer.parseInt(val); return false; } catch(Exception e) {}
         if (val.equals("true") || val.equals("false")) return false;
         return true;
    }

    private boolean isRef(String name) {
        return varIsRef.getOrDefault(name, false);
    }
}
