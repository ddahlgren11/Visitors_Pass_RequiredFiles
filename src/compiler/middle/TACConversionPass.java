package compiler.middle;

import compiler.infra.CompilerContext;
import compiler.infra.CompilerPass;
import compiler.frontend.ASTNode;
import compiler.middle.tac.TACInstruction;
import java.util.List;

public class TACConversionPass implements CompilerPass {

    @Override
    public String name() {
        return "Three-Address Code Conversion";
    }

    @Override
    public void execute(CompilerContext context) throws Exception {
        ASTNode root = (ASTNode) context.getAst();
        if (root == null) {
            // No AST, nothing to do
            return;
        }

        TACVisitor visitor = new TACVisitor();
        root.accept(visitor);

        List<TACInstruction> tac = visitor.getTac();
        context.setTac(tac);
    }
}
