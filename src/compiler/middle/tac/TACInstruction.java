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
        if (arg2 != null) {
            return target + " = " + arg1 + " " + op + " " + arg2;
        } else {
            return target + " = " + arg1;
        }
    }
}