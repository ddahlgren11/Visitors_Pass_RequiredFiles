package compiler.frontend;

import java.util.ArrayList;
import java.util.List;

public class ASTTestTree {
    private final String label;
    private final List<ASTTestTree> children = new ArrayList<>();

    public ASTTestTree(String label) {
        this.label = label;
    }

    public void addChild(ASTTestTree child) {
        children.add(child);
    }

    @Override
    public String toString() {
        if (children.isEmpty()) return label;
        StringBuilder sb = new StringBuilder("(").append(label);
        for (ASTTestTree child : children) {
            sb.append(" ").append(child.toString());
        }
        sb.append(")");
        return sb.toString();
    }

    public String prettyPrint() {
        return prettyPrint(0);
    }

    private String prettyPrint(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) sb.append("  ");
        sb.append(label).append("\n");
        for (ASTTestTree child : children) {
            sb.append(child.prettyPrint(indent + 1));
        }
        return sb.toString();
    }
}