package compiler.middle.tac;

import java.util.ArrayList;
import java.util.List;

import compiler.frontend.BinaryOpNode;
import compiler.frontend.ast.ASTNode;

public class TACConversionPass implements TACVisitor<String> {

    private final List<TACInstruction> instructions = new ArrayList<>();
    private int tempCount = 0;

    private String newTemp() {
        return "t" + (tempCount++);
    }

    public List<TACInstruction> getInstructions() {
        return instructions;
    }

    @Override
    public String visit(LiteralNode node) {
        String temp = newTemp();
        instructions.add(new TACInstruction(OpCode.LOAD_CONST, temp, node.value.toString(), null));
        return temp;
    }

    @Override
    public String visit(IdentifierNode node) {
        return node.name; // no TAC
    }

    @Override
    public String visit(BinaryOpNode node) {
        String left = node.left.accept(this);
        String right = node.right.accept(this);

        String temp = newTemp();
        OpCode op = switch(node.operator) {
            case "+" -> OpCode.ADD;
            case "-" -> OpCode.SUB;
            case "*" -> OpCode.MUL;
            case "/" -> OpCode.DIV;
            default -> throw new RuntimeException("Unknown operator");
        };

        instructions.add(new TACInstruction(op, temp, left, right));
        return temp;
    }

    @Override
    public String visit(VarDeclNode node) {
        String value = node.initializer.accept(this);
        instructions.add(new TACInstruction(OpCode.STORE_VAR, node.name, value, null));
        return node.name;
    }

    @Override
    public String visit(AssignmentNode node) {
        String value = node.expression.accept(this);
        instructions.add(new TACInstruction(OpCode.STORE_VAR, node.target.name, value, null));
        return node.target.name;
    }

    @Override
    public String visit(FunctionDeclNode node) {
        for (ASTNode stmt : node.body) {
            stmt.accept(this);
        }
        return null;
    }
}
