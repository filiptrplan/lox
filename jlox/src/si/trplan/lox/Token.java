package si.trplan.lox;

public class Token {
    final TokenType type;
    // The raw lexeme
    final String lexeme;
    // This is the value that would be in NUMBER or STRING
    final Object literal;
    // For error handling
    final int line;
    
    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }
    
    public String toString() {
        return type + " " + lexeme + " " + literal;
    } 
}
