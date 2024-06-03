package si.trplan.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    public Object get(Token name, Interpreter interpreter) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }
        
        LoxFunction getter = klass.findGetter(name.lexeme);
        if(getter != null) {
            return getter.bind(this).call(interpreter, new ArrayList<>());
        }
        
        LoxFunction method = klass.findMethod(name.lexeme);
        if(method != null) return method.bind(this);
        
        throw new RuntimeError(
                name,
                String.format("Property '%s' is not defined in '%s;", name.lexeme, this.toString())
        );
    }
    
    public void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
