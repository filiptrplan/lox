package si.trplan.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;
    private final Map<String, LoxFunction> getters;
    final LoxClass superclass;
    
    LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods, Map<String, LoxFunction> getters) {
        this.name = name;
        this.methods = methods;
        this.getters = getters;
        this.superclass = superclass;
    }
    
    public LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) return methods.get(name);
        if (superclass != null) {
            return superclass.findMethod(name);
        }
        return null;
    }
    
    public LoxFunction findGetter(String name) {
        if (getters.containsKey(name)) return getters.get(name);
        return null;
    }
    
    @Override
    public int arity() {
        LoxFunction initializer = this.findMethod("init");
        if(initializer == null) return 0;
        else return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = this.findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);    
        }
        return instance;
    }
    
    @Override
    public String toString() {
        return name;
    }

}
