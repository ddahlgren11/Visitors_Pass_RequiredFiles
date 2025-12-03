package compiler.middle.tac;

public class TACInstruction {
    public final OpCode op;
    public final String target;
    public final String arg1;
    public final String arg2;

    public TACInstruction(OpCode op, String target, String arg1, String arg2) {
        this.op = op;
        this.target = target;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        switch (op) {
            case LOAD_CONST:
                return target + " = " + arg1;
            case STORE_VAR:
                return target + " = " + arg1;
            case ADD:
                return target + " = " + arg1 + " + " + arg2;
            case SUB:
                return target + " = " + arg1 + " - " + arg2;
            case MUL:
                return target + " = " + arg1 + " * " + arg2;
            case DIV:
                return target + " = " + arg1 + " / " + arg2;
            default:
                return op + " " + target + ", " + arg1 + ", " + arg2;
        }
    }
}