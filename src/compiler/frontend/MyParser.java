/* MyParser.java */
package compiler.frontend;

import java.util.*;
import compiler.frontend.ast.*;

public class MyParser implements MyParserConstants {
    public static void main(String[] args) {
        try {
            MyParser parser = new MyParser(System.in);
            ASTNode prog = parser.Program();
            System.out.println(prog.toASTTestTree().toString());
        } catch (TokenMgrError e) {
            System.err.println("LEXER ERROR: " + e.getMessage());
        } catch (ParseException e) {
            System.err.println("PARSE ERROR: " + e.getMessage());
        }
    }

    // ---------- Program ----------
    final public ASTNode Program() throws ParseException {
        List<ASTNode> stmts = new ArrayList<>();
        while (getToken(1).kind != EOF) {
            // Skip empty statements or handle them if needed, but here we expect valid statements or declarations
            if (getToken(1).kind == CLASS) {
                stmts.add(ClassDecl());
            } else {
                stmts.add(Statement());
            }
        }
        jj_consume_token(EOF);
        return new BlockNode(stmts);
    }

    // ---------- Statements ----------
    final public ASTNode Statement() throws ParseException {
        switch (getToken(1).kind) {
            case LBRACE:
                return Block();
            case IF:
                return IfStmt();
            case WHILE:
                return WhileStmt();
            case FOR:
                return ForStmt();
            case RETURN:
                return ReturnStmt();
            case INT: // Variable declaration starting with type 'int'
                return VarDecl();
            case IDENTIFIER:
                // Could be VarDecl (Type ID ...), FunctionDecl (Type ID( ...), Assignment (ID = ...), Call (ID( ...), or just an ID expression
                // Lookahead to disambiguate
                Token t1 = getToken(1); // Type or ID
                Token t2 = getToken(2);
                Token t3 = getToken(3);

                if (t2.kind == IDENTIFIER) {
                    // Type Identifier ... -> Decl
                     if (t3.kind == LPAREN) return FunctionDecl();
                     return VarDecl();
                }

                // If t2 is ASSIGN, it's assignment: ID = ...
                if (t2.kind == ASSIGN) {
                    return Assignment();
                }

                // Otherwise expression statement
                return ExprStmt();
            case VOID:
                return FunctionDecl();
            case SEMI:
                jj_consume_token(SEMI);
                return new EmptyNode();
            default:
                return ExprStmt();
        }
    }

    final public ClassDeclNode ClassDecl() throws ParseException {
        jj_consume_token(CLASS);
        Token name = jj_consume_token(IDENTIFIER);
        jj_consume_token(LBRACE);
        List<VarDeclNode> fields = new ArrayList<>();
        List<FunctionDeclNode> methods = new ArrayList<>();

        while (getToken(1).kind != RBRACE && getToken(1).kind != EOF) {
            // Consume modifiers (ignore them for now)
            while (getToken(1).kind == PUBLIC || getToken(1).kind == STATIC) {
                jj_consume_token(getToken(1).kind);
            }

            // Lookahead to distinguish Field vs Constructor vs Method
            // Constructor: Name ( ...
            // Method: Type Name ( ...
            // Field: Type Name ...

            // We need to parse the type first or checking if the first token matches the class name (Constructor)
            Token t1 = getToken(1);
            Token t2 = getToken(2);
            Token t3 = getToken(3);

            boolean isConstructor = (t1.kind == IDENTIFIER && t1.image.equals(name.image) && t2.kind == LPAREN);

            if (isConstructor) {
                 jj_consume_token(IDENTIFIER); // name
                 jj_consume_token(LPAREN);
                 List<VarDeclNode> params = new ArrayList<>();
                 if (getToken(1).kind != RPAREN) params = ParamList();
                 jj_consume_token(RPAREN);
                 BlockNode body = Block();
                 methods.add(new FunctionDeclNode(name.image, name.image, params, body));
            } else {
                // Method or Field
                // Both start with Type Name
                String type = ParseType();
                Token memberName = jj_consume_token(IDENTIFIER);

                if (getToken(1).kind == LPAREN) {
                    // Method
                    jj_consume_token(LPAREN);
                    List<VarDeclNode> params = new ArrayList<>();
                    if (getToken(1).kind != RPAREN) params = ParamList();
                    jj_consume_token(RPAREN);
                    BlockNode body = Block();
                    methods.add(new FunctionDeclNode(type, memberName.image, params, body));
                } else {
                    // Field
                    ExpressionNode init = null;
                    if (getToken(1).kind == ASSIGN) {
                        jj_consume_token(ASSIGN);
                        init = Expression();
                    }
                    jj_consume_token(SEMI);
                    fields.add(new VarDeclNode(type, memberName.image, init));
                }
            }
        }
        jj_consume_token(RBRACE);
        return new ClassDeclNode(name.image, fields, methods);
    }

    final public ASTNode FunctionDecl() throws ParseException {
        String type = "void";
        if (getToken(1).kind == VOID) {
            jj_consume_token(VOID);
        } else {
            type = ParseType();
        }

        Token id = jj_consume_token(IDENTIFIER);
        jj_consume_token(LPAREN);
        List<VarDeclNode> params = new ArrayList<>();
        if (getToken(1).kind != RPAREN) {
            params = ParamList();
        }
        jj_consume_token(RPAREN);
        BlockNode body = Block();
        return new FunctionDeclNode(type, id.image, params, body);
    }

    final public ASTNode VarDecl() throws ParseException {
        String type = ParseType();
        Token id = jj_consume_token(IDENTIFIER);
        ExpressionNode expr = null;
        if (getToken(1).kind == ASSIGN) {
            jj_consume_token(ASSIGN);
            expr = Expression();
        }
        jj_consume_token(SEMI);
        return new VarDeclNode(type, id.image, expr);
    }

    String ParseType() throws ParseException {
        if (getToken(1).kind == INT) return jj_consume_token(INT).image;
        if (getToken(1).kind == VOID) return jj_consume_token(VOID).image; // Though usually void is handled separately
        if (getToken(1).kind == IDENTIFIER) return jj_consume_token(IDENTIFIER).image; // Class types
        throw new ParseException("Expected type, found " + getToken(1));
    }

    final public AssignmentNode Assignment() throws ParseException {
        Token id = jj_consume_token(IDENTIFIER);
        jj_consume_token(ASSIGN);
        ExpressionNode expr = Expression();
        jj_consume_token(SEMI);
        return new AssignmentNode(new IdentifierNode(id.image), expr);
    }

    final public ASTNode ExprStmt() throws ParseException {
        ExpressionNode e = Expression();
        // Check if it was actually an assignment masquerading as expression (e.g. if we parsed an identifier)
        // But the Statement() method dispatches to Assignment() if it sees IDENTIFIER followed by ASSIGN.
        // However, if we are here, it might be a method call or other expression.

        // Wait, Statement() handles Assignment() separately.
        // But what if we have `x.y = 5;`? That starts with IDENTIFIER.
        // My parser in Statement() only checks `getToken(2) == ASSIGN`.
        // `x.y` is IDENTIFIER DOT IDENTIFIER.

        // Let's rely on ExprStmt handling assignments if they were not caught by Statement().
        // But `AssignmentNode` expects `IdentifierNode` on LHS, not arbitrary expression (in this simple language?).
        // If the language supports field assignment `obj.field = val`, we need `MemberAccessNode` or similar.
        // For now, let's assume `Expression` consumes everything.

        // If the next token is ASSIGN, then `Expression()` returned the LHS.
        if (getToken(1).kind == ASSIGN) {
             jj_consume_token(ASSIGN);
             ExpressionNode rhs = Expression();
             jj_consume_token(SEMI);
             return new AssignmentNode(e, rhs);
        }

        jj_consume_token(SEMI);
        return e;
    }

    final public ASTNode IfStmt() throws ParseException {
        jj_consume_token(IF);
        jj_consume_token(LPAREN);
        ExpressionNode cond = Expression();
        jj_consume_token(RPAREN);
        BlockNode thenBlock = Block();
        BlockNode elseBlock = null;

        if (getToken(1).kind == ELSE) {
            jj_consume_token(ELSE);
            if (getToken(1).kind == IF) {
                // else if ... -> treat as else { if ... }
                ASTNode elseIf = IfStmt();
                List<ASTNode> stmts = new ArrayList<>();
                stmts.add(elseIf);
                elseBlock = new BlockNode(stmts);
            } else {
                elseBlock = Block();
            }
        }
        return new IfNode(cond, thenBlock, elseBlock);
    }

    final public ASTNode WhileStmt() throws ParseException {
        jj_consume_token(WHILE);
        jj_consume_token(LPAREN);
        ExpressionNode cond = Expression();
        jj_consume_token(RPAREN);
        BlockNode body = Block();
        return new WhileNode(cond, body);
    }

    final public ASTNode ForStmt() throws ParseException {
        jj_consume_token(FOR);
        jj_consume_token(LPAREN);

        VarDeclNode varDecl = null;
        AssignmentNode assign = null;

        // Init: VarDecl or Assignment or Empty
        if (getToken(1).kind == INT) {
            // VarDecl without SEMI (handled manually here)
            String type = jj_consume_token(INT).image;
            Token id = jj_consume_token(IDENTIFIER);
            ExpressionNode init = null;
            if (getToken(1).kind == ASSIGN) {
                jj_consume_token(ASSIGN);
                init = Expression();
            }
            varDecl = new VarDeclNode(type, id.image, init);
        } else if (getToken(1).kind == IDENTIFIER) {
            // Assignment without SEMI
            Token id = jj_consume_token(IDENTIFIER);
            jj_consume_token(ASSIGN);
            ExpressionNode init = Expression();
            assign = new AssignmentNode(new IdentifierNode(id.image), init);
        }

        jj_consume_token(SEMI);

        ExpressionNode cond = null;
        if (getToken(1).kind != SEMI) {
            cond = Expression();
        }
        jj_consume_token(SEMI);

        ExpressionNode update = null;
        if (getToken(1).kind != RPAREN) {
            update = Expression();
        }
        jj_consume_token(RPAREN);

        BlockNode body = Block();

        if (varDecl != null) {
            return new ForNode(varDecl, cond, update, body);
        } else {
            return new ForNode(assign, cond, update, body);
        }
    }

    final public ASTNode ReturnStmt() throws ParseException {
        jj_consume_token(RETURN);
        ExpressionNode e = null;
        if (getToken(1).kind != SEMI) {
            e = Expression();
        }
        jj_consume_token(SEMI);
        return new ReturnNode(e);
    }

    final public BlockNode Block() throws ParseException {
        jj_consume_token(LBRACE);
        List<ASTNode> stmts = new ArrayList<>();
        while (getToken(1).kind != RBRACE && getToken(1).kind != EOF) {
            stmts.add(Statement());
        }
        jj_consume_token(RBRACE);
        return new BlockNode(stmts);
    }

    final public List<VarDeclNode> ParamList() throws ParseException {
        List<VarDeclNode> params = new ArrayList<>();
        do {
            String type = ParseType();
            Token id = jj_consume_token(IDENTIFIER);
            params.add(new VarDeclNode(type, id.image, null));

            if (getToken(1).kind == COMMA) {
                jj_consume_token(COMMA);
            } else {
                break;
            }
        } while (true);
        return params;
    }

    final public List<ExpressionNode> ArgList() throws ParseException {
        List<ExpressionNode> args = new ArrayList<>();
        do {
            args.add(Expression());
            if (getToken(1).kind == COMMA) {
                jj_consume_token(COMMA);
            } else {
                break;
            }
        } while (true);
        return args;
    }

    // ---------- Expressions ----------

    final public ExpressionNode Expression() throws ParseException {
        return LogicalOr();
    }

    final public ExpressionNode LogicalOr() throws ParseException {
        ExpressionNode left = LogicalAnd();
        while (getToken(1).kind == OR) {
            Token op = jj_consume_token(OR);
            ExpressionNode right = LogicalAnd();
            left = new BinaryOpNode(op.image, left, right);
        }
        return left;
    }

    final public ExpressionNode LogicalAnd() throws ParseException {
        ExpressionNode left = Equality();
        while (getToken(1).kind == AND) {
            Token op = jj_consume_token(AND);
            ExpressionNode right = Equality();
            left = new BinaryOpNode(op.image, left, right);
        }
        return left;
    }

    final public ExpressionNode Equality() throws ParseException {
        ExpressionNode left = Relational();
        while (getToken(1).kind == EQ || getToken(1).kind == NEQ) {
            Token op = jj_consume_token(getToken(1).kind);
            ExpressionNode right = Relational();
            left = new BinaryOpNode(op.image, left, right);
        }
        return left;
    }

    final public ExpressionNode Relational() throws ParseException {
        ExpressionNode left = Additive();
        while (getToken(1).kind == LT || getToken(1).kind == GT /* || getToken(1).kind == LTE || getToken(1).kind == GTE */) {
            Token op = jj_consume_token(getToken(1).kind);
            ExpressionNode right = Additive();
            left = new BinaryOpNode(op.image, left, right);
        }
        return left;
    }

    final public ExpressionNode Additive() throws ParseException {
        ExpressionNode left = Multiplicative();
        while (getToken(1).kind == PLUS || getToken(1).kind == MINUS) {
            Token op = jj_consume_token(getToken(1).kind);
            ExpressionNode right = Multiplicative();
            left = new BinaryOpNode(op.image, left, right);
        }
        return left;
    }

    final public ExpressionNode Multiplicative() throws ParseException {
        ExpressionNode left = Unary();
        while (getToken(1).kind == MULT || getToken(1).kind == DIV) {
            Token op = jj_consume_token(getToken(1).kind);
            ExpressionNode right = Unary();
            left = new BinaryOpNode(op.image, left, right);
        }
        return left;
    }

    final public ExpressionNode Unary() throws ParseException {
        if (getToken(1).kind == MINUS || getToken(1).kind == PLUS) { // Unary minus/plus
             Token op = jj_consume_token(getToken(1).kind);
             ExpressionNode e = Unary(); // Right associative or just Factor? Usually Unary.
             // But for - - 5 it should work.
             // UnaryOpNode might expect "negate" or similar? Or just "-"
             // The AST expects "negate"? Or just the operator string?
             // Checking AST usage: `new UnaryOpNode("post++", ...)`
             // I'll use the operator string.
             return new UnaryOpNode(op.image, e);
        }
        // logical not !
        // Token '!' is not in the constants list explicitly?
        // Ah, checked constants: LT, GT, NEQ... I don't see NOT/BANG.
        // Wait, `!=` is NEQ.
        // If `!` is supported, it should be in constants.
        // Checked MyParserConstants.java: I don't see `!` or `NOT`.
        // Maybe it's missing?
        // But `!=` exists.
        // If the language supports `!boolean`, it should be there.
        // Assuming it's not supported or I missed it.
        // I'll skip logical not for now unless I see it.

        return Factor();
    }

    final public ExpressionNode Factor() throws ParseException {
        ExpressionNode e;
        switch(getToken(1).kind) {
            case INT_LITERAL:
                e = new LiteralNode(jj_consume_token(INT_LITERAL).image);
                break;
            case STRING_LITERAL:
                e = new LiteralNode(jj_consume_token(STRING_LITERAL).image);
                break;
            case TRUE:
                jj_consume_token(TRUE);
                e = new LiteralNode("true");
                break;
            case FALSE:
                jj_consume_token(FALSE);
                e = new LiteralNode("false");
                break;
            case NULL:
                jj_consume_token(NULL);
                e = new LiteralNode("null");
                break;
            case THIS:
                jj_consume_token(THIS);
                e = new IdentifierNode("this");
                if (getToken(1).kind == DOT) {
                     jj_consume_token(DOT);
                     Token member = jj_consume_token(IDENTIFIER);
                     if (getToken(1).kind == LPAREN) {
                         jj_consume_token(LPAREN);
                         List<ExpressionNode> callArgs = new ArrayList<>();
                         if (getToken(1).kind != RPAREN) {
                             callArgs = ArgList();
                         }
                         jj_consume_token(RPAREN);
                         e = new MethodCallNode(new IdentifierNode("this"), member.image, callArgs);
                     } else {
                         e = new MemberAccessNode(new IdentifierNode("this"), member.image);
                     }
                }
                break;
            case NEW:
                jj_consume_token(NEW);
                Token cName = jj_consume_token(IDENTIFIER);
                jj_consume_token(LPAREN);
                List<ExpressionNode> args = new ArrayList<>();
                if (getToken(1).kind != RPAREN) {
                    args = ArgList();
                }
                jj_consume_token(RPAREN);
                e = new NewExprNode(cName.image, args);
                break;
            case IDENTIFIER:
                Token t = jj_consume_token(IDENTIFIER);
                if (getToken(1).kind == LPAREN) {
                    jj_consume_token(LPAREN);
                    List<ExpressionNode> callArgs = new ArrayList<>();
                    if (getToken(1).kind != RPAREN) {
                        callArgs = ArgList();
                    }
                    jj_consume_token(RPAREN);
                    e = new MethodCallNode(null, t.image, callArgs);
                } else if (getToken(1).kind == DOT) {
                     // Handle Member Access or Method Call on object
                     // t.field or t.method()
                     jj_consume_token(DOT);
                     Token member = jj_consume_token(IDENTIFIER);
                     if (getToken(1).kind == LPAREN) {
                         jj_consume_token(LPAREN);
                         List<ExpressionNode> callArgs = new ArrayList<>();
                         if (getToken(1).kind != RPAREN) {
                             callArgs = ArgList();
                         }
                         jj_consume_token(RPAREN);
                         e = new MethodCallNode(new IdentifierNode(t.image), member.image, callArgs);
                     } else {
                         // Field access
                         e = new MemberAccessNode(new IdentifierNode(t.image), member.image);
                     }
                } else if (getToken(1).kind == INCR) {
                    jj_consume_token(INCR);
                    e = new UnaryOpNode("post++", new IdentifierNode(t.image));
                } else if (getToken(1).kind == DECR) {
                    jj_consume_token(DECR);
                    e = new UnaryOpNode("post--", new IdentifierNode(t.image));
                } else {
                    e = new IdentifierNode(t.image);
                }
                break;
            case LPAREN:
                jj_consume_token(LPAREN);
                e = Expression();
                jj_consume_token(RPAREN);
                break;
            default:
                throw new ParseException("Unexpected token in Factor: " + getToken(1));
        }
        return e;
    }


  // =========================================================================
  // Generated Token Manager Infrastructure (Do Not Modify)
  // =========================================================================

  public MyParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  public Token token;
  public Token jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  private int jj_gen;
  final private int[] jj_la1 = new int[30];
  static private int[] jj_la1_0;
  static private int[] jj_la1_1;
  static {
	   jj_la1_init_0();
	   jj_la1_init_1();
	}
	private static void jj_la1_init_0() {
	   jj_la1_0 = new int[] {0xc1fe80,0xc1fe80,0xc1c000,0x2000,0x2000000,0x0,0x0,0x1000000,0x1000000,0x0,0x0,0x30000000,0x30000000,0xc0000000,0xc0000000,0xc000000,0xc000000,0xc1c000,0x100,0x2000,0x2000000,0x0,0xc1c000,0xc1c000,0xc1fe80,0x2800,0x2000,0x2000,0x0,0x2000,};
	}
	private static void jj_la1_init_1() {
	   jj_la1_1 = new int[] {0x1540,0x1540,0x1040,0x1000,0x0,0x10,0x8,0x4,0x4,0x3,0x3,0x0,0x0,0x0,0x0,0x0,0x0,0x1040,0x0,0x1000,0x0,0x1000,0x1040,0x1040,0x1540,0x1000,0x1000,0x1000,0x800,0x1000,};
	}
  final private JJCalls[] jj_2_rtns = new JJCalls[2];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  public MyParser(java.io.InputStream stream) {
	  this(stream, null);
  }
  public MyParser(java.io.InputStream stream, String encoding) {
	 try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
	 token_source = new MyParserTokenManager(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 30; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }
  public void ReInit(java.io.InputStream stream) {
	  ReInit(stream, null);
  }
  public void ReInit(java.io.InputStream stream, String encoding) {
	 try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
	 token_source.ReInit(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 30; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }
  public MyParser(java.io.Reader stream) {
	 jj_input_stream = new SimpleCharStream(stream, 1, 1);
	 token_source = new MyParserTokenManager(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 30; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }
  public void ReInit(java.io.Reader stream) {
	if (jj_input_stream == null) {
	   jj_input_stream = new SimpleCharStream(stream, 1, 1);
	} else {
	   jj_input_stream.ReInit(stream, 1, 1);
	}
	if (token_source == null) {
 token_source = new MyParserTokenManager(jj_input_stream);
	}
	 token_source.ReInit(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 30; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }
  public MyParser(MyParserTokenManager tm) {
	 token_source = tm;
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 30; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }
  public void ReInit(MyParserTokenManager tm) {
	 token_source = tm;
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 30; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }
  private Token jj_consume_token(int kind) throws ParseException {
	 Token oldToken;
	 if ((oldToken = token).next != null) token = token.next;
	 else token = token.next = token_source.getNextToken();
	 jj_ntk = -1;
	 if (token.kind == kind) {
	   jj_gen++;
	   if (++jj_gc > 100) {
		 jj_gc = 0;
		 for (int i = 0; i < jj_2_rtns.length; i++) {
		   JJCalls c = jj_2_rtns[i];
		   while (c != null) {
			 if (c.gen < jj_gen) c.first = null;
			 c = c.next;
		   }
		 }
	   }
	   return token;
	 }
	 token = oldToken;
	 jj_kind = kind;
	 throw generateParseException();
  }
  @SuppressWarnings("serial")
  static private final class LookaheadSuccess extends java.lang.Error {
    @Override
    public Throwable fillInStackTrace() {
      return this;
    }
  }
  static private final LookaheadSuccess jj_ls = new LookaheadSuccess();
  private boolean jj_scan_token(int kind) {
	 if (jj_scanpos == jj_lastpos) {
	   jj_la--;
	   if (jj_scanpos.next == null) {
		 jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
	   } else {
		 jj_lastpos = jj_scanpos = jj_scanpos.next;
	   }
	 } else {
	   jj_scanpos = jj_scanpos.next;
	 }
	 if (jj_rescan) {
	   int i = 0; Token tok = token;
	   while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
	   if (tok != null) jj_add_error_token(kind, i);
	 }
	 if (jj_scanpos.kind != kind) return true;
	 if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
	 return false;
  }
  final public Token getNextToken() {
	 if (token.next != null) token = token.next;
	 else token = token.next = token_source.getNextToken();
	 jj_ntk = -1;
	 jj_gen++;
	 return token;
  }
  final public Token getToken(int index) {
	 Token t = token;
	 for (int i = 0; i < index; i++) {
	   if (t.next != null) t = t.next;
	   else t = t.next = token_source.getNextToken();
	 }
	 return t;
  }
  private int jj_ntk_f() {
	 if ((jj_nt=token.next) == null)
	   return (jj_ntk = (token.next=token_source.getNextToken()).kind);
	 else
	   return (jj_ntk = jj_nt.kind);
  }
  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;
  private void jj_add_error_token(int kind, int pos) {
	 if (pos >= 100) {
		return;
	 }
	 if (pos == jj_endpos + 1) {
	   jj_lasttokens[jj_endpos++] = kind;
	 } else if (jj_endpos != 0) {
	   jj_expentry = new int[jj_endpos];
	   for (int i = 0; i < jj_endpos; i++) {
		 jj_expentry[i] = jj_lasttokens[i];
	   }
	   for (int[] oldentry : jj_expentries) {
		 if (oldentry.length == jj_expentry.length) {
		   boolean isMatched = true;
		   for (int i = 0; i < jj_expentry.length; i++) {
			 if (oldentry[i] != jj_expentry[i]) {
			   isMatched = false;
			   break;
			 }
		   }
		   if (isMatched) {
			 jj_expentries.add(jj_expentry);
			 break;
		   }
		 }
	   }
	   if (pos != 0) {
		 jj_lasttokens[(jj_endpos = pos) - 1] = kind;
	   }
	 }
  }
  public ParseException generateParseException() {
	 jj_expentries.clear();
	 boolean[] la1tokens = new boolean[47];
	 if (jj_kind >= 0) {
	   la1tokens[jj_kind] = true;
	   jj_kind = -1;
	 }
	 for (int i = 0; i < 30; i++) {
	   if (jj_la1[i] == jj_gen) {
		 for (int j = 0; j < 32; j++) {
		   if ((jj_la1_0[i] & (1<<j)) != 0) {
			 la1tokens[j] = true;
		   }
		   if ((jj_la1_1[i] & (1<<j)) != 0) {
			 la1tokens[32+j] = true;
		   }
		 }
	   }
	 }
	 for (int i = 0; i < 47; i++) {
	   if (la1tokens[i]) {
		 jj_expentry = new int[1];
		 jj_expentry[0] = i;
		 jj_expentries.add(jj_expentry);
	   }
	 }
	 jj_endpos = 0;
	 jj_rescan_token();
	 jj_add_error_token(0, 0);
	 int[][] exptokseq = new int[jj_expentries.size()][];
	 for (int i = 0; i < jj_expentries.size(); i++) {
	   exptokseq[i] = jj_expentries.get(i);
	 }
	 return new ParseException(token, exptokseq, tokenImage);
  }
  private boolean trace_enabled;
  final public boolean trace_enabled() {
	 return trace_enabled;
  }
  final public void enable_tracing() {
  }
  final public void disable_tracing() {
  }
  private void jj_rescan_token() {
	 jj_rescan = true;
	 for (int i = 0; i < 2; i++) {
	   try {
		 JJCalls p = jj_2_rtns[i];
		 do {
		   if (p.gen > jj_gen) {
			 jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
			 switch (i) {
			   case 0: break;
			   case 1: break;
			 }
		   }
		   p = p.next;
		 } while (p != null);
		 } catch(LookaheadSuccess ls) { }
	 }
	 jj_rescan = false;
  }
  private void jj_save(int index, int xla) {
	 JJCalls p = jj_2_rtns[index];
	 while (p.gen > jj_gen) {
	   if (p.next == null) { p = p.next = new JJCalls(); break; }
	   p = p.next;
	 }
	 p.gen = jj_gen + xla - jj_la; 
	 p.first = token;
	 p.arg = xla;
  }
  static final class JJCalls {
	 int gen;
	 Token first;
	 int arg;
	 JJCalls next;
  }
}
