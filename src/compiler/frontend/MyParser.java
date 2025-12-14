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
            if (getToken(1).kind == CLASS) {
                stmts.add(ClassDecl());
            } else {
                stmts.add(Statement());
            }
        }
        jj_consume_token(EOF);
        return new BlockNode(stmts);
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

            Token t1 = getToken(1);
            Token t2 = getToken(2);
            Token t3 = getToken(3);

            if (t2.kind == LPAREN) {
                // Constructor: Name ( ...
                jj_consume_token(IDENTIFIER); // name
                jj_consume_token(LPAREN);
                List<VarDeclNode> params = new ArrayList<>();
                if (getToken(1).kind != RPAREN) params = ParamList();
                jj_consume_token(RPAREN);
                BlockNode body = Block();
                methods.add(new FunctionDeclNode(name.image, name.image, params, body));
            } else if (t3.kind == LPAREN) {
                // Method: Type Name ( ...
                String type = ParseType();
                Token mName = jj_consume_token(IDENTIFIER);
                jj_consume_token(LPAREN);
                List<VarDeclNode> params = new ArrayList<>();
                if (getToken(1).kind != RPAREN) params = ParamList();
                jj_consume_token(RPAREN);
                BlockNode body = Block();
                methods.add(new FunctionDeclNode(type, mName.image, params, body));
            } else {
                // Field: Type Name ...
                String type = ParseType();
                Token fName = jj_consume_token(IDENTIFIER);
                ExpressionNode init = null;
                if (getToken(1).kind == ASSIGN) {
                    jj_consume_token(ASSIGN);
                    init = Expression();
                }
                jj_consume_token(SEMI);
                fields.add(new VarDeclNode(type, fName.image, init));
            }
        }
        jj_consume_token(RBRACE);
        return new ClassDeclNode(name.image, fields, methods);
    }

    String ParseType() throws ParseException {
        if (getToken(1).kind == INT) return jj_consume_token(INT).image;
        if (getToken(1).kind == VOID) return jj_consume_token(VOID).image;
        return jj_consume_token(IDENTIFIER).image;
    }

    // ---------- Statements ----------
    final public ASTNode Statement() throws ParseException {
        switch (getToken(1).kind) {
            case INT:
                return VarDecl();
            case IDENTIFIER:
                // Check if it's a Declaration (Type ID ...)
                if (getToken(2).kind == IDENTIFIER) {
                     if (getToken(3).kind == LPAREN) return FunctionDecl();
                     return VarDecl();
                }
                // Otherwise it's an Expression Statement (assignment or call)
                return ExprStmt();
            case IF: return IfStmt();
            case WHILE: return WhileStmt();
            case FOR: return ForStmt();
            case LBRACE: return Block();
            case VOID: return FunctionDecl();
            case RETURN: return ReturnStmt();
            case SEMI:
                jj_consume_token(SEMI);
                return new EmptyNode();
            default:
                return ExprStmt();
        }
    }

    final public ASTNode ReturnStmt() throws ParseException {
        jj_consume_token(RETURN);
        ExpressionNode expr = null;
        if (getToken(1).kind != SEMI) {
            expr = Expression();
        }
        jj_consume_token(SEMI);
        return new ReturnNode(expr);
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

    final public AssignmentNode Assignment() throws ParseException {
        Token id = jj_consume_token(IDENTIFIER);
        jj_consume_token(ASSIGN);
        ExpressionNode expr = Expression();
        jj_consume_token(SEMI);
        return new AssignmentNode(new IdentifierNode(id.image), expr);
    }

    final public ASTNode ExprStmt() throws ParseException {
        ExpressionNode e = Expression();
        if (getToken(1).kind == ASSIGN) {
            jj_consume_token(ASSIGN);
            ExpressionNode rhs = Expression();
            jj_consume_token(SEMI);
            return new AssignmentNode(e, rhs);
        }
        jj_consume_token(SEMI);
        return e;
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
        while (getToken(1).kind == LT || getToken(1).kind == GT) {
            Token op = jj_consume_token(getToken(1).kind);
            ExpressionNode right = Additive();
            left = new BinaryOpNode(op.image, left, right);
        }
        return left;
    }

    final public ExpressionNode Additive() throws ParseException {
        ExpressionNode left = Term();
        while (getToken(1).kind == PLUS || getToken(1).kind == MINUS) {
            Token op = jj_consume_token(getToken(1).kind);
            ExpressionNode right = Term();
            left = new BinaryOpNode(op.image, left, right);
        }
        return left;
    }

    final public ExpressionNode Term() throws ParseException {
        ExpressionNode left = Factor();
        while (getToken(1).kind == MULT || getToken(1).kind == DIV) {
            Token op = jj_consume_token(getToken(1).kind);
            ExpressionNode right = Factor();
            left = new BinaryOpNode(op.image, left, right);
        }
        return left;
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

        while (getToken(1).kind == DOT) {
            jj_consume_token(DOT);
            Token member = jj_consume_token(IDENTIFIER);
            if (getToken(1).kind == LPAREN) {
                jj_consume_token(LPAREN);
                List<ExpressionNode> mArgs = new ArrayList<>();
                if (getToken(1).kind != RPAREN) {
                    mArgs = ArgList();
                }
                jj_consume_token(RPAREN);
                e = new MethodCallNode(e, member.image, mArgs);
            } else {
                e = new MemberAccessNode(e, member.image);
            }
        }
        return e;
    }

    List<ExpressionNode> ArgList() throws ParseException {
        List<ExpressionNode> args = new ArrayList<>();
        args.add(Expression());
        while (getToken(1).kind == COMMA) {
            jj_consume_token(COMMA);
            args.add(Expression());
        }
        return args;
    }

    // ---------- Control Structures ----------
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

        ASTNode init = null;
        if (getToken(1).kind == INT) {
            init = VarDeclForLoop();
        } else if (getToken(1).kind == IDENTIFIER) {
             if (getToken(2).kind == ASSIGN) {
                 init = AssignmentForLoop();
             } else if (getToken(2).kind == IDENTIFIER) {
                 init = VarDeclForLoop();
             }
        }
        jj_consume_token(SEMI);

        ExpressionNode cond = null;
        if (getToken(1).kind != SEMI) cond = Expression();
        jj_consume_token(SEMI);

        ExpressionNode update = null;
        if (getToken(1).kind != RPAREN) update = Expression();
        jj_consume_token(RPAREN);

        BlockNode body = Block();

        return new ForNode(init, cond, update, body);
    }

    final public VarDeclNode VarDeclForLoop() throws ParseException {
        String type = ParseType();
        Token id = jj_consume_token(IDENTIFIER);
        ExpressionNode expr = null;
        if (getToken(1).kind == ASSIGN) {
            jj_consume_token(ASSIGN);
            expr = Expression();
        }
        return new VarDeclNode(type, id.image, expr);
    }

    final public AssignmentNode AssignmentForLoop() throws ParseException {
        Token id = jj_consume_token(IDENTIFIER);
        jj_consume_token(ASSIGN);
        ExpressionNode expr = Expression();
        return new AssignmentNode(new IdentifierNode(id.image), expr);
    }

    final public BlockNode Block() throws ParseException {
        jj_consume_token(LBRACE);
        List<ASTNode> stmts = new ArrayList<>();
        while (getToken(1).kind != RBRACE) {
            stmts.add(Statement());
        }
        jj_consume_token(RBRACE);
        return new BlockNode(stmts);
    }

    final public ASTNode FunctionDecl() throws ParseException {
        String retType = ParseType();
        Token name = jj_consume_token(IDENTIFIER);
        jj_consume_token(LPAREN);
        List<VarDeclNode> params = new ArrayList<>();
        if (getToken(1).kind != RPAREN) params = ParamList();
        jj_consume_token(RPAREN);
        BlockNode body = Block();
        return new FunctionDeclNode(retType, name.image, params, body);
    }

    final public List<VarDeclNode> ParamList() throws ParseException {
        List<VarDeclNode> params = new ArrayList<>();
        params.add(Param());
        while (getToken(1).kind == COMMA) {
            jj_consume_token(COMMA);
            params.add(Param());
        }
        return params;
    }

    VarDeclNode Param() throws ParseException {
        String type = ParseType();
        Token id = jj_consume_token(IDENTIFIER);
        return new VarDeclNode(type, id.image, null);
    }

    // Infrastructure

  /** Generated Token Manager. */
  public MyParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  /** Whether we are looking ahead. */
  private boolean jj_lookingAhead = false;
  private boolean jj_semLA;
  private int jj_gen;
  final private int[] jj_la1 = new int[28];
  static private int[] jj_la1_0;
  static private int[] jj_la1_1;
  static {
	   jj_la1_init_0();
	   jj_la1_init_1();
	}
	private static void jj_la1_init_0() {
	   jj_la1_0 = new int[] {0x3e80,0x0,0xc1c000,0x2000,0x2000000,0x0,0x0,0x1000000,0x1000000,0x0,0x0,0x30000000,0x30000000,0xc0000000,0xc0000000,0xc000000,0xc1c000,0x100,0x2000,0x2000000,0x0,0xc1c000,0xc1c000,0x2800,0x2000,0x2000,0x0,0x2000,};
	}
	private static void jj_la1_init_1() {
	   jj_la1_1 = new int[] {0x1100,0x400,0x1040,0x1000,0x0,0x10,0x8,0x4,0x4,0x3,0x3,0x0,0x0,0x0,0x0,0x0,0x1040,0x0,0x1000,0x0,0x1000,0x1040,0x1040,0x1000,0x1000,0x1000,0x800,0x1000,};
	}
  final private JJCalls[] jj_2_rtns = new JJCalls[3];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  /** Constructor with InputStream. */
  public MyParser(java.io.InputStream stream) {
	  this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public MyParser(java.io.InputStream stream, String encoding) {
	 try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
	 token_source = new MyParserTokenManager(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 28; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
	  ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
	 try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
	 token_source.ReInit(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 28; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor. */
  public MyParser(java.io.Reader stream) {
	 jj_input_stream = new SimpleCharStream(stream, 1, 1);
	 token_source = new MyParserTokenManager(jj_input_stream);
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 28; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
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
	 for (int i = 0; i < 28; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor with generated Token Manager. */
  public MyParser(MyParserTokenManager tm) {
	 token_source = tm;
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 28; i++) jj_la1[i] = -1;
	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(MyParserTokenManager tm) {
	 token_source = tm;
	 token = new Token();
	 jj_ntk = -1;
	 jj_gen = 0;
	 for (int i = 0; i < 28; i++) jj_la1[i] = -1;
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


/** Get the next Token. */
  final public Token getNextToken() {
	 if (token.next != null) token = token.next;
	 else token = token.next = token_source.getNextToken();
	 jj_ntk = -1;
	 jj_gen++;
	 return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
	 Token t = jj_lookingAhead ? jj_scanpos : token;
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

  /** Generate ParseException. */
  public ParseException generateParseException() {
	 jj_expentries.clear();
	 boolean[] la1tokens = new boolean[47];
	 if (jj_kind >= 0) {
	   la1tokens[jj_kind] = true;
	   jj_kind = -1;
	 }
	 for (int i = 0; i < 28; i++) {
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

/** Trace enabled. */
  final public boolean trace_enabled() {
	 return trace_enabled;
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

  private void jj_rescan_token() {
	 jj_rescan = true;
	 for (int i = 0; i < 3; i++) {
	   try {
		 JJCalls p = jj_2_rtns[i];

		 do {
		   if (p.gen > jj_gen) {
			 jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
			 switch (i) {
			   // case 0: jj_3_1(); break;
			   // case 1: jj_3_2(); break;
			   // case 2: jj_3_3(); break;
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
