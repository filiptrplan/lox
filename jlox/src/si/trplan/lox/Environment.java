package si.trplan.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();
    
    public static class Unassigned {}
    
    Environment() {
        enclosing = null;
    }
    
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }
    
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            Object value = values.get(name.lexeme);
            if(value instanceof Unassigned) {
                throw new RuntimeError(name, "Unassigned variable '" + name.lexeme + "'");
            }
            return values.get(name.lexeme);
        }
        
        if (enclosing != null) return enclosing.get(name);
        
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
    
    void assign(Token name, Object value) {
        if(values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        
        if(enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
    
    void define(String name, Object value) {
        values.put(name, value);
    }
    
}
