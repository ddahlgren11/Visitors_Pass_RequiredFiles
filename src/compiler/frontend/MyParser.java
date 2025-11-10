package compiler.frontend;

import java.io.*;
import java.util.*;

import compiler.frontend.*;

public class MyParser {
    private final List<String> tokens;

    public MyParser() {
        this(new StringReader(""));
    }

    public MyParser(java.io.InputStream in) {
        this(new InputStreamReader(in));
    }

    public MyParser(java.io.Reader reader) {
        StringBuilder sb = new StringBuilder();
        try (Reader r = reader) {
            char[] buf = new char[1024];
            int n;
            while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
        } catch (IOException e) {
            // ignore, keep input empty
        }
        this.tokens = tokenize(sb.toString());
    }

    private static List<String> tokenize(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            // skip whitespace
            if (Character.isWhitespace(c)) { i++; continue; }

            // string literal
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                sb.append('"');
                i++;
                while (i < s.length()) {
                    char d = s.charAt(i);
                    sb.append(d);
                    i++;
                    if (d == '"') break;
                    if (d == '\\' && i < s.length()) { // include escaped char
                        sb.append(s.charAt(i));
                        i++;
                    }
                }
                out.add(sb.toString());
                continue;
            }

            // identifier
            if (Character.isLetter(c) || c == '_') {
                int j = i + 1;
                while (j < s.length()) {
                    char d = s.charAt(j);
                    if (Character.isLetterOrDigit(d) || d == '_') j++; else break;
                }
                out.add(s.substring(i, j));
                i = j;
                continue;
            }

            // number literal
            if (Character.isDigit(c)) {
                int j = i + 1;
                while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
                out.add(s.substring(i, j));
                i = j;
                continue;
            }

            // two-char operators
            if (i + 1 < s.length()) {
                String two = s.substring(i, i + 2);
                if (two.equals("==") || two.equals("!=") || two.equals("++") || two.equals("--") || two.equals("&&") || two.equals("||")) {
                    out.add(two);
                    i += 2;
                    continue;
                }
            }

            // single-char token
            out.add(String.valueOf(c));
            i++;
        }
        return out;
    }

    private static boolean isIdentifier(String t) {
        return t != null && t.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    // Return AST node for an assignment
    public ASTNodeBase Assignment() throws ParseException {
        List<String> t = tokens;
        int semi = t.indexOf(";");
        if (semi == -1) throw new ParseException("missing semicolon");
        List<String> stmt = new ArrayList<>(t.subList(0, semi + 1));
        // Accept: int id = expr ;  OR id = expr ;
        if (stmt.size() >= 4 && "int".equals(stmt.get(0)) && isIdentifier(stmt.get(1)) && "=".equals(stmt.get(2))) {
            // top-level assignment of form: int id = expr; -> produce (= (decl int id) expr)
            String type = stmt.get(0);
            String name = stmt.get(1);
            ASTNodeBase init = tokenToExpr(stmt.subList(3, stmt.size() - 1));
            ASTNodeBase declNoInit = new VarDeclNode(type, name, null);
            return new AssignmentNode(declNoInit, init);
        }
        if (stmt.size() >= 3 && isIdentifier(stmt.get(0)) && "=".equals(stmt.get(1))) {
            ASTNodeBase var = new IdentifierNode(stmt.get(0));
            ASTNodeBase val = tokenToExpr(stmt.subList(2, stmt.size() - 1));
            return new AssignmentNode(var, val);
        }
        throw new ParseException("invalid assignment");
    }

    public ASTNodeBase Declaration() throws ParseException {
        List<String> t = tokens;
        int semi = t.indexOf(";");
        if (semi == -1) throw new ParseException("missing semicolon");
        List<String> stmt = new ArrayList<>(t.subList(0, semi + 1));
        // Expect: int id ;
        if (stmt.size() == 3 && "int".equals(stmt.get(0)) && isIdentifier(stmt.get(1)) && ";".equals(stmt.get(2))) {
            return new VarDeclNode(stmt.get(0), stmt.get(1), null);
        }
        throw new ParseException("invalid declaration");
    }

    public ASTNodeBase PrintStatement() throws ParseException {
        // Not used by tests; keep for completeness
        throw new ParseException("not implemented");
    }

    public ASTNodeBase IfStatement() throws ParseException {
        List<String> stmt = conditionalTokens("if");
        // tokens: if ( ... ) { ... }
        // find rparen and lbrace
        int rparen = stmt.indexOf(")");
        int lbrace = stmt.indexOf("{");
        List<String> condTokens = stmt.subList(2, rparen);
        List<String> bodyTokens = stmt.subList(lbrace + 1, stmt.size() - 1);
        ASTNodeBase cond = tokenToExpr(condTokens);
        ASTNodeBase body = blockTokensToNode(bodyTokens);
        return new IfNode(cond, body, null);
    }

    public ASTNodeBase WhileStatement() throws ParseException {
        List<String> stmt = conditionalTokens("while");
        int rparen = stmt.indexOf(")");
        int lbrace = stmt.indexOf("{");
        ASTNodeBase cond = tokenToExpr(stmt.subList(2, rparen));
        ASTNodeBase body = blockTokensToNode(stmt.subList(lbrace + 1, stmt.size() - 1));
        return new WhileNode(cond, body);
    }

    private List<String> conditionalTokens(String kw) throws ParseException {
        List<String> t = tokens;
        if (t.size() < 1 || !kw.equals(t.get(0))) throw new ParseException("missing " + kw);
        if (t.size() < 4 || !"(".equals(t.get(1))) throw new ParseException("missing paren");
        int idx = 2;
        while (idx < t.size() && !")".equals(t.get(idx))) idx++;
        if (idx >= t.size()) throw new ParseException("unclosed paren");
        int rparen = idx;
        if (rparen + 1 >= t.size()) throw new ParseException("missing statement");
        if (!"{".equals(t.get(rparen + 1))) throw new ParseException("missing block");
        int lbrace = rparen + 1;
        int j = lbrace + 1;
        while (j < t.size() && !"}".equals(t.get(j))) j++;
        if (j >= t.size()) throw new ParseException("unclosed block");
        return new ArrayList<>(t.subList(0, j + 1));
    }

    public ASTNodeBase ForStatement() throws ParseException {
        List<String> t = tokens;
        if (t.size() < 1 || !"for".equals(t.get(0))) throw new ParseException("missing for");
        if (t.size() < 4 || !"(".equals(t.get(1))) throw new ParseException("missing paren");
        int idx = 2;
        while (idx < t.size() && !")".equals(t.get(idx))) idx++;
        if (idx >= t.size()) throw new ParseException("unclosed paren");
        int rparen = idx;
        if (rparen + 1 >= t.size() || !"{".equals(t.get(rparen + 1))) throw new ParseException("missing block");
        int j = rparen + 2;
        while (j < t.size() && !"}".equals(t.get(j))) j++;
        if (j >= t.size()) throw new ParseException("unclosed block");
        List<String> stmt = new ArrayList<>(t.subList(0, j + 1));
        // naive split: init ; cond ; update
        int lparen = 1;
        int firstSemi = findIndex(stmt, ";", lparen);
        int secondSemi = firstSemi == -1 ? -1 : findIndex(stmt, ";", firstSemi + 1);
        ASTNodeBase init = null, cond = null, update = null;
        int rparenIndex = findIndex(stmt, ")", 0);
        int lbraceIndex = findIndex(stmt, "{", 0);
        if (firstSemi > lparen) init = simpleStmtToNode(stmt.subList(lparen + 1, firstSemi + 1));
        if (secondSemi > firstSemi) cond = tokenToExpr(stmt.subList(firstSemi + 1, secondSemi));
        if (rparenIndex > secondSemi) {
            List<String> updTokens = stmt.subList(secondSemi + 1, rparenIndex);
            // update part may not end with semicolon inside for(...) so allow no trailing ;
            if (!updTokens.isEmpty()) {
                // if update looks like an assignment, parse it
                update = simpleStmtToNode(new ArrayList<>(updTokens));
            }
        }
        ASTNodeBase body = blockTokensToNode(stmt.subList(lbraceIndex + 1, stmt.size() - 1));
        return new ForNode(init, cond, update, body);
    }

    public ASTNodeBase FunctionDeclaration() throws ParseException {
        List<String> t = tokens;
        if (t.size() < 6) throw new ParseException("invalid function");
        if (!"int".equals(t.get(0)) && !"void".equals(t.get(0))) throw new ParseException("invalid return type");
        String returnType = t.get(0);
        String name = t.get(1);
        // find closing paren
        int idx = 3;
        while (idx < t.size() && !")".equals(t.get(idx))) idx++;
        if (idx >= t.size()) throw new ParseException("unclosed paren");
        int rparen = idx;
        if (rparen + 1 >= t.size() || !"{".equals(t.get(rparen + 1))) throw new ParseException("missing block");
        int j = rparen + 2;
        while (j < t.size() && !"}".equals(t.get(j))) j++;
        if (j >= t.size()) throw new ParseException("unclosed block");
        // parse params (very simple: pairs of type name separated by commas)
        List<VarDeclNode> params = new ArrayList<>();
        int i = 3;
        while (i < rparen) {
            if (i + 1 < rparen && ("int".equals(t.get(i)) || "void".equals(t.get(i)))) {
                String ptype = t.get(i);
                String pname = t.get(i + 1);
                params.add(new VarDeclNode(ptype, pname, null));
                i += 2;
                if (i < rparen && ",".equals(t.get(i))) i++;
                continue;
            }
            i++;
        }
        ASTNodeBase body = blockTokensToNode(t.subList(rparen + 2, j));
        return new FunctionDeclNode(returnType, name, params, body);
    }

    // Helpers to convert token lists into AST nodes (very small expressions supported)
    private ASTNodeBase tokenToExpr(List<String> toks) throws ParseException {
        if (toks == null || toks.isEmpty()) throw new ParseException("empty expression");
        if (toks.size() == 1) {
            String tk = toks.get(0);
            if (tk.matches("[0-9]+")) return new LiteralNode(tk);
            return new IdentifierNode(tk);
        }
        // binary op of form a op b
        if (toks.size() == 3) {
            ASTNodeBase left = tokenToExpr(toks.subList(0,1));
            String op = toks.get(1);
            ASTNodeBase right = tokenToExpr(toks.subList(2,3));
            return new BinaryOpNode(op, left, right);
        }
        throw new ParseException("unsupported expression: " + toks);
    }

    private ASTNodeBase blockTokensToNode(List<String> toks) throws ParseException {
        // toks contains tokens inside the braces; assume a single statement for tests
        List<ASTNodeBase> stmts = new ArrayList<>();
        if (toks == null || toks.isEmpty()) return new BlockNode(stmts);
        // find semicolon separated statements
        int i = 0;
        while (i < toks.size()) {
            int semi = findIndex(toks, ";", i);
            if (semi == -1) break;
            List<String> stmt = toks.subList(i, semi + 1);
            stmts.add(simpleStmtToNode(stmt));
            i = semi + 1;
        }
        return new BlockNode(stmts);
    }

    private int findIndex(List<String> list, String target, int start) {
        for (int k = start; k < list.size(); k++) {
            if (target.equals(list.get(k))) return k;
        }
        return -1;
    }

    private ASTNodeBase simpleStmtToNode(List<String> stmt) throws ParseException {
        if (stmt == null || stmt.isEmpty()) throw new ParseException("empty stmt");
        // normalize: if last token is ";" remove it for easier matching
        List<String> toks = stmt;
        if (toks.size() > 0 && ";".equals(toks.get(toks.size() - 1))) toks = toks.subList(0, toks.size() - 1);
    if ("int".equals(toks.get(0))) {
            if (toks.size() >= 3 && "=".equals(toks.get(2))) {
                String type = toks.get(0);
                String name = toks.get(1);
                ASTNodeBase initExpr = tokenToExpr(toks.subList(3, toks.size()));
                // represent top-level initialized declaration as VarDeclNode with an init that is an AssignmentNode whose left is a decl node
                VarDeclNode declNoInit = new VarDeclNode(type, name, null);
                AssignmentNode assign = new AssignmentNode(declNoInit, initExpr);
                return new VarDeclNode(type, name, assign);
            } else if (toks.size() == 2) {
                return new VarDeclNode(toks.get(0), toks.get(1), null);
            }
        }
        // return statement
        if ("return".equals(toks.get(0))) {
            if (toks.size() == 2) {
                return new ReturnNode(tokenToExpr(toks.subList(1, 2)));
            } else if (toks.size() == 3) {
                return new ReturnNode(tokenToExpr(toks.subList(1, 3)));
            } else if (toks.size() > 1) {
                // attempt to parse binary return expr like a + b
                return new ReturnNode(tokenToExpr(toks.subList(1, toks.size())));
            } else {
                return new ReturnNode(null);
            }
        }
        // assignment like 'y = y * 2;'
        if (toks.size() >= 3 && isIdentifier(toks.get(0)) && "=".equals(toks.get(1))) {
            ASTNodeBase var = new IdentifierNode(toks.get(0));
            ASTNodeBase val = tokenToExpr(toks.subList(2, toks.size()));
            return new AssignmentNode(var, val);
        }
        throw new ParseException("unsupported simple stmt: " + stmt);
    }
}
