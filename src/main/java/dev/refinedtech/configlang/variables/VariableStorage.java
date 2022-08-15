package dev.refinedtech.configlang.variables;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;

public class VariableStorage {

    private final HashMap<String, Object> variables = new HashMap<>();

    public void set(String key, Object value) {
        this.variables.put(key, value);
    }

    public <T> Optional<T> get(String key) {
        Optional<Object> opt = this.getRaw(key);
        if (opt.isEmpty()) return Optional.empty();

        try {
            return Optional.ofNullable((T) opt.get());
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    public Optional<Object> getRaw(String key) {
        return Optional.ofNullable(this.variables.get(key));
    }

    public boolean exists(String key) {
        return this.variables.containsKey(key);
    }

    public Optional<Object> remove(String key) {
        return Optional.ofNullable(this.variables.remove(key));
    }

    public <T> Optional<T> parseVariable(String variableAccessor) {
        Optional<Object> opt = this.parseVariableRaw(variableAccessor);
        if (opt.isEmpty()) return Optional.empty();
        try {
            return Optional.ofNullable((T) opt.get());
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    public Optional<Object> parseVariableRaw(String variableAccessor) {
        if (variableAccessor.isEmpty())
            return Optional.empty();

        if (variableAccessor.startsWith("$")) {
            String accessor = variableAccessor.substring(1);
            int index = accessor.indexOf(' ');
            if (index == -1)
                index = accessor.length();
            return this.getRaw(accessor.substring(0, index));
        }

        String[] accessors = Arrays.stream(variableAccessor.split("'s")).map(String::trim).toArray(String[]::new);
        if (accessors.length == 1)
            return this.get(accessors[0]);

        try {
            String[] sub = new String[accessors.length - 1];
            System.arraycopy(accessors, 1, sub, 0, sub.length);
            return Optional.ofNullable(this.getFromAccessors(this.getRaw(accessors[0]).orElseThrow(), sub));
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException | NoSuchElementException e) {
            return Optional.empty();
        }
    }

    public void copy(VariableStorage other) {
        this.variables.putAll(other.variables);
    }

    private Object getFromAccessors(Object obj, String... accessors) throws NoSuchFieldException, IllegalAccessException {
        if (accessors.length == 0) return obj;
        String[] sub = new String[accessors.length - 1];
        System.arraycopy(accessors, 1, sub, 0, sub.length);
        Field field = obj.getClass().getField(accessors[0]);
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        Object value = field.get(obj);
        field.setAccessible(accessible);
        return getFromAccessors(value, sub);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Variables:\n");
        for (String key : this.variables.keySet()) {
            sb.append("  ").append(key).append(" = ").append(this.variables.get(key)).append("\n");
        }
        return sb.toString();
    }
}
