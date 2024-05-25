package si.trplan.lox;

import java.util.ArrayList;
import java.util.List;

import static si.trplan.lox.TokenType.*;

/*
 * PROGRAM GRAMMAR:
 * program -> declaration* EOF;
 * declaration -> varDecl | statement;
 * statement -> exprStmt | printStmt | block;
 * block -> "{" declaration* "}";
 * 
 * exprStmt -> expression ";";
 * printStmt -> "print" expression ";";
 *
 * varDecl -> "var" IDENTIFIER ( "=" expression )? ";" ;
 * 
 * EXPRESSION GRAMMAR:
 * expression → assignment ;
 * assignment -> equality | IDENTIFIER "=" assignment;
 * equality → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term  → factor ( ( "-" | "+" ) factor )* ;
 * factor → unary ( ( "/" | "*" ) unary )* ;
 * unary  → ( "!" | "-" ) unary | primary ;
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
        while(!isAtEnd()) {
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
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }
    
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expecting variable name.");
        
        Expr initializer = null;
        if(match(EQUAL)) {
            initializer = expression();
        }
        
        consume(SEMICOLON, "Expecting ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if(match(PRINT)) return printStatement();
        if(match(LEFT_BRACE)) return new Stmt.Block(block());
        
        return expressionStatement();
    }
    
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expecting ';' after value in print statement.");
        return new Stmt.Print(value);
    }
    
    private Stmt expressionStatement() {
        Expr value = expression();
        if(allowExpression && isAtEnd()) {
            foundExpression = true;
        } else {
            consume(SEMICOLON, "Expecting ';' after expression in expression statement.");
        }
        return new Stmt.Expression(value);
    }
    
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        
        while(!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        
        consume(RIGHT_BRACE, "Expecting closing '}' after block");
        return statements;
    }
    
    private Expr assignment() {
        Expr expr = equality();
        if(match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            
            if(expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            // We don't throw the error because we don't need the synchronization. We just report it.
            //noinspection ThrowableNotThrown
            error(equals, "Invalid assignment target.");
        }
        return expr;
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
        return primary();
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
     * Parse a left associative rule defined as: original -> next ( ( op1 | op2 | ... ) next)*
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
        }
    }
}
