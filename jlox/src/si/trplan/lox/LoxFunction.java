package si.trplan.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }
    
    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        Object returnValue = null;
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return ret) {
            returnValue = ret.value;
        }
        
        if(isInitializer) return closure.getAt(0, "this");
        return returnValue;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}