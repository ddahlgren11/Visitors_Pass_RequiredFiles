package compiler.middle.tac;

public class TACInstruction {
    public final OpCode opCode;
    public final String dest;
    public final String arg1;
    public final String arg2;

    public TACInstruction(OpCode opCode, String dest, String arg1, String arg2) {
        this.opCode = opCode;
        this.dest = dest;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        switch (opCode) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                return String.format("%s = %s %s %s", dest, arg1, opCode, arg2);
            case LOAD_CONST:
                return String.format("%s = %s", dest, arg1);
            case STORE_VAR:
                return String.format("%s = %s", dest, arg1);
            case LABEL:
                return String.format("%s:", dest);
            default:
                return String.format("%s %s %s %s", opCode, dest, arg1, arg2);
        }
    }
}
