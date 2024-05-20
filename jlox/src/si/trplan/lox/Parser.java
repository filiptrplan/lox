package si.trplan.lox;

import java.util.List;

import static si.trplan.lox.TokenType.*;

/**
 * Our expression grammar:
 * expression → equality ;
 * equality → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term  → factor ( ( "-" | "+" ) factor )* ;
 * factor → unary ( ( "/" | "*" ) unary )* ;
 * unary  → ( "!" | "-" ) unary | primary ;
 * primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
 */

public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return equality();
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
