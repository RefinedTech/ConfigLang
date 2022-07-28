package dev.refinedtech.configlang;

import dev.refinedtech.configlang.scope.Scope;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class TestLanguage {

    public static void main(String[] args) {
        ConfigLang lang = new ConfigLang();
        lang.setErrorLogger(Logger.getGlobal());

        lang.submit(new ConfigStructure("exec", ConfigStructure.keyStructure("message")) {

            @Override
            protected Object run(ConfigSection section, Scope scope, Object... args) {
                System.out.println(section.getObject("message"));
                return null;
            }
        });

        lang.submit(new ConfigStructure("operation",
                ConfigStructure.keyStructure("left"),
                ConfigStructure.keyStructure("operator"),
                ConfigStructure.keyStructure("right")) {
            @Override
            public boolean returnsData() {
                return true;
            }

            @Override
            protected Object run(ConfigSection section, Scope scope, Object... args) {
                Object left = lang.getData(section, "left", scope.childScope("left"), args);
                Object right = lang.getData(section, "right", scope.childScope("right"), args);
                String operator = String.valueOf(lang.getData(section, "operator", scope.childScope("operator"), args));

                if (left == null || right == null) return false;

                if (left instanceof Number && right instanceof Number)  {
                    double leftDouble = ((Number)left).doubleValue();
                    double rightDouble = ((Number)right).doubleValue();

                    int leftInt = ((Number)left).intValue();
                    int rightInt = ((Number)right).intValue();

                    switch (operator) {
                        case "*": return leftDouble * rightDouble;
                        case "/": return leftDouble / rightDouble;
                        case "%": return leftDouble % rightDouble;
                        case "+": return leftDouble + rightDouble;
                        case "-": return leftDouble - rightDouble;
                        case "<<": return leftInt << rightInt;
                        case ">>": return leftInt >> rightInt;
                        case ">>>": return leftInt >>> rightInt;
                        case "<": return leftDouble < rightDouble;
                        case ">": return leftDouble > rightDouble;
                        case "<=": return leftDouble <= rightDouble;
                        case ">=": return leftDouble >= rightDouble;
                        default: return false;
                    }
                }

                if (left instanceof Boolean && right instanceof Boolean) {
                    boolean leftBoolean = (Boolean) left;
                    boolean rightBoolean = (Boolean) right;
                    switch (operator) {
                        case "&": return leftBoolean & rightBoolean;
                        case "^": return leftBoolean ^ rightBoolean;
                        case "|": return leftBoolean | rightBoolean;
                        case "&&": return leftBoolean && rightBoolean;
                        case "||": return leftBoolean || rightBoolean;
                        default: return false;
                    }
                }

                switch (operator) {
                    case "==": return left == right;
                    case "!=": return left != right;
                    case "+": return String.valueOf(left) + right;
                    default: return false;
                }
            }
        });

        lang.submit(new ConfigStructure("condition") {
            @Override
            public boolean returnsData() {
                return true;
            }

            @Override
            protected Object run(ConfigSection section, Scope scope, Object... args) {

                AtomicBoolean res = new AtomicBoolean(true);

                lang.executeChildren(section, scope.childScope("condition")).forEach(result -> {
                    if (result instanceof Boolean)
                        res.set(res.get() && (Boolean) result);
                });

                return res.get();
            }
        });

        lang.submit(new ConfigStructure("if", lang.get("condition").orElseThrow()) {
            @Override
            protected Object run(ConfigSection section, Scope scope, Object... args) {
                ConfigSection condition = section.getConfigSection("condition").orElseThrow();

                Object res = lang.execute(condition, scope.childScope("if"));

                if (res instanceof Boolean && (Boolean) res && section.isConfigSection("then")) {
                    lang.executeChildrenRecursive(section.getConfigSection("then").orElseThrow(), scope.childScope("then"));
                } else if (section.isConfigSection("else")) {
                    lang.executeChildrenRecursive(section.getConfigSection("else").orElseThrow(), scope.childScope("else"));
                }

                return null;
            }
        });

        ConfigSection section = new HashMapConfigSection("if","if");
        ConfigSection condition = new HashMapConfigSection("if.condition","condition");
        ConfigSection operation = new HashMapConfigSection("if.condition.operation","operation");

        ConfigSection operation2 = new HashMapConfigSection("if.condition.operation.operation2","operation");

        operation2.set("left", 5);
        operation2.set("operator", "+");
        operation2.set("right", 6);

        operation.set("left", operation2);
        operation.set("operator", "<");
        operation.set("right", 10);

        condition.set("operation", operation);
        section.set("condition", condition);

        ConfigSection then = new HashMapConfigSection("if.then","then");

        ConfigSection exec = new HashMapConfigSection("if.then.exec","exec");
        exec.set("message", "Hello World!");

        then.set("exec", exec);

        section.set("then", then);
        ConfigSection else_ = new HashMapConfigSection("if.else","else");
        ConfigSection exec2 = new HashMapConfigSection("if.then.exec","exec");
        exec2.set("message", "Goodbye World!");
        else_.set("exec", exec2);
        section.set("else", else_);

        System.out.println(section.treeString(0));

        Scope scope = new Scope("root");

        lang.execute(section, scope);
    }

}

class HashMapConfigSection extends ConfigSection {

    private final String path;
    private final String name;
    private final HashMap<String, Object> map = new HashMap<>();

    public HashMapConfigSection(String path, String name) {
        this.path = path;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public boolean isConfigSection(String key) {
        return this.map.get(key) instanceof ConfigSection;
    }

    @Override
    public Optional<ConfigSection> getConfigSection(String key) {
        Object obj = this.map.get(key);

        if (obj instanceof ConfigSection) return Optional.of((ConfigSection) obj);

        return Optional.empty();
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        if (deep) {
            Set<String> keys = new HashSet<>();

            for (String key : this.map.keySet()) {
                Object obj = this.map.get(key);
                if (obj instanceof ConfigSection) {
                    keys.addAll(((ConfigSection) obj).getKeys(true));
                } else {
                    keys.add(key);
                }
            }

            return keys;
        }

        return this.map.keySet();
    }

    @Override
    public boolean contains(String key) {
        return this.map.containsKey(key);
    }

    @Override
    public <T> Optional<T> get(String key) {
        Object obj = this.getObject(key);
        try {
            return Optional.of((T) obj);
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    @Override
    public <T> T get(String key, T def) {
        return this.<T>get(key).orElse(def);
    }

    @Override
    public Optional<Object> getObject(String key) {
        return Optional.of(this.map.get(key));
    }

    @Override
    public Object getObject(String key, Object def) {
        return this.getObject(key).orElse(def);
    }

    @Override
    public void set(String key, Object value) {
        this.map.put(key, value);
    }
}