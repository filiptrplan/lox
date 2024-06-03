package si.trplan.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static si.trplan.lox.TokenType.*;

/*
 * PROGRAM GRAMMAR:
 * program -> declaration* EOF;
 * declaration -> classDecl | varDecl | statement | function;
 * statement -> exprStmt | printStmt | block | ifStmt | whileStmt | forStmt | breakStmt | returnStmt;
 *
 * breakStmt -> "break" ";" ;
 * forStmt -> "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement;
 * whileStmt -> "while" "(" expression ")" statement;
 * ifStmt -> "if" "(" expression ")" statement ( "else" statement )?
 * block -> "{" declaration* "}";
 *
 * exprStmt -> expression ";";
 * printStmt -> "print" expression ";";
 * 
 * classDecl -> "class" IDENTIFIER "{" function* "}" ;
 *
 * varDecl -> "var" IDENTIFIER ( "=" expression )? ";" ;
 * funDecl -> "fun" function;
 * function -> IDENTIFIER "(" parameters? ")" block;
 * parameters -> IDENTIFIER ( "," IDENTIFIER )* ;
 * returnStmt -> "return" expression? ";" ;
 *
 * arguments -> expression ( "," expression )* ;
 *
 * EXPRESSION GRAMMAR:
 * expression → assignment ;
 * assignment -> logic_or | IDENTIFIER "=" assignment;
 * logic_or -> logic_and ( "||" logic_and )*;
 * logic_and -> equality ( "&&" equality)*;
 * equality → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term  → factor ( ( "-" | "+" ) factor )* ;
 * factor → unary ( ( "/" | "*" ) unary )* ;
 * unary  → ( "!" | "-" ) unary | funcExpr;
 * funcExpr -> "fun" "(" arguments? ")" block | call;
 * call -> primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
 * primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER ;
 */

public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current;

    private boolean allowExpression;
    private boolean foundExpression = false;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Object parseRepl() {
        allowExpression = true;
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());

            if (foundExpression) {
                Stmt last = statements.getLast();
                return ((Stmt.Expression) last).expression;
            }

            allowExpression = false;
        }

        return statements;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            if (match(FUN)) return function("function");
            if (match(CLASS)) return classDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }
    
    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect identifier after class keyword.");
        consume(LEFT_BRACE, "Expect '{' after class name.");
        List<Stmt.Function> methods = new ArrayList<>();
        while(!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add((Stmt.Function)function("method")); 
        }
        
        consume(RIGHT_BRACE, "Expect '}' after class body");
        return new Stmt.Class(name, methods);
    }

    private Stmt function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");

        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");

        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    // We don't need to synchronize here
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expecting only identifiers as " + kind + " parameters"));
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after " + kind + " arguments.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();

        return new Stmt.Function(name, parameters, body);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expecting variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expecting ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(FOR)) return forStatement();
        if (match(BREAK)) return breakStatement();
        if (match(RETURN)) return returnStatement();

        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (match(SEMICOLON)) return new Stmt.Return(keyword, value);
        value = expression();
        consume(SEMICOLON, "Expect ';' after value in return statement");
        return new Stmt.Return(keyword, value);
    }

    private Stmt breakStatement() {
        Token breakToken = previous();
        consume(SEMICOLON, "Expect ';' after break statement.");
        return new Stmt.Break(breakToken);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expecting '(' after for keyword.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expecting ';' after loop condition");

        Expr increment = null;
        if (!check(SEMICOLON)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expecting ')' after loop condition");

        Stmt body;
        body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expecting '(' after while keyword.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expecting closing ')'.");

        Stmt statement = statement();
        return new Stmt.While(condition, statement);
    }

    private Stmt ifStatement() {

        consume(LEFT_PAREN, "Expecting '(' after if keyword.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expecting closing ')'.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expecting ';' after value in print statement.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        if (allowExpression && isAtEnd()) {
            foundExpression = true;
        } else {
            consume(SEMICOLON, "Expecting ';' after expression in expression statement.");
        }
        return new Stmt.Expression(value);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expecting closing '}' after block");
        return statements;
    }

    private Expr assignment() {
        Expr expr = logicOr();
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof  Expr.Get) {
                Expr.Get getExpr = (Expr.Get)expr;
                return new Expr.Set(getExpr.object, getExpr.name, value);
            }

            // We don't throw the error because we don't need the synchronization. We just report it.
            //noinspection ThrowableNotThrown
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr logicOr() {
        return parseLeftAssocLogical(this::logicAnd, OR);
    }

    private Expr logicAnd() {
        return parseLeftAssocLogical(this::equality, AND);
    }

    private Expr equality() {
        return parseLeftAssocBinary(this::comparison, EQUAL_EQUAL, BANG_EQUAL);
    }

    private Expr comparison() {
        return parseLeftAssocBinary(this::term, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL);
    }

    private Expr term() {
        return parseLeftAssocBinary(this::factor, PLUS, MINUS);
    }

    private Expr factor() {
        return parseLeftAssocBinary(this::unary, STAR, SLASH);
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return funcExpr();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect identifier after dot.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr funcExpr() {

        if (!match(FUN)) {
            return call();
        }
        consume(LEFT_PAREN, "Expect '(' after fun keyword.");

        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    // We don't need to synchronize here
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expecting only identifiers as function parameters"));
            } while (match(COMMA));
        }

        consume(RIGHT_PAREN, "Expect ')' after function arguments.");

        consume(LEFT_BRACE, "Expect '{' before function body.");
        List<Stmt> body = block();

        return new Expr.Function(parameters, body);
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    //noinspection ThrowableNotThrown
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expecting ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        throw error(peek(), "Expecting expression.");
    }

    private interface IExpressionParser {
        Expr parse();
    }

    /**
     * Parse a binary left associative rule defined as: original -> next ( ( op1 | op2 | ... ) next)*
     *
     * @param nextParser The "next" rule
     * @param types      Operators to match
     * @return An expression with any number of operators and next expansions
     */
    private Expr parseLeftAssocBinary(IExpressionParser nextParser, TokenType... types) {
        Expr expr = nextParser.parse();
        while (match(types)) {
            Token operator = previous();
            Expr right = nextParser.parse();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * Parse a logical left associative rule defined as: original -> next ( ( op1 | op2 | ... ) next)*
     *
     * @param nextParser The "next" rule
     * @param types      Operators to match
     * @return An expression with any number of operators and next expansions
     */
    private Expr parseLeftAssocLogical(IExpressionParser nextParser, TokenType... types) {
        Expr expr = nextParser.parse();
        while (match(types)) {
            Token operator = previous();
            Expr right = nextParser.parse();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     * If the current token matches any of the arguments we return true and advance,
     * else just stay where we are
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    /**
     * Checks whether the current token matches the argument
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * @return The current token
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * @return Whether the current token is EOF
     */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /**
     * Increments current by 1
     *
     * @return The token pre-increment
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * @return The previous token
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            // We reached the start of a new statment, we can now synchronize
            if (previous().type == SEMICOLON) return;
            // If we reach any of these token it is also probably the start of a new statement
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
}
