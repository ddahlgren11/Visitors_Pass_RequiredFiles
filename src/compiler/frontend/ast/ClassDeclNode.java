package compiler.frontend.ast;

import java.util.List;
import compiler.frontend.ASTTestTree;

public class ClassDeclNode extends ASTNode {
    public String className;
    public List<VarDeclNode> fields;
    public List<FunctionDeclNode> methods;

    public ClassDeclNode(String className, List<VarDeclNode> fields, List<FunctionDeclNode> methods) {
        this.className = className;
        this.fields = fields;
        this.methods = methods;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitClassDeclNode(this);
    }

    @Override
    public ASTTestTree toASTTestTree() {
        ASTTestTree tree = new ASTTestTree("ClassDecl: " + className);
        for (VarDeclNode field : fields) {
            tree.addChild(field.toASTTestTree());
        }
        for (FunctionDeclNode method : methods) {
            tree.addChild(method.toASTTestTree());
        }
        return tree;
    }

    @Override
    public String toString() {
        return "ClassDeclNode{" +
                "className='" + className + '\'' +
                ", fields=" + fields +
                ", methods=" + methods +
                '}';
    }
}
