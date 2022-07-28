package me.ikevoodoo.configlang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class ConfigLang {

    private final HashMap<String, ConfigStructure> structureHashMap = new HashMap<>();
    private Logger errorLogger;

    public void submit(ConfigStructure structure) {
        if (structure == null) return;
        structureHashMap.put(structure.key(), structure);
    }

    public void setErrorLogger(Logger logger) {
        this.errorLogger = logger;
    }

    public Optional<ConfigStructure> get(String key) {
        return Optional.ofNullable(structureHashMap.get(key));
    }

    public boolean knows(String key) {
        return this.structureHashMap.containsKey(key);
    }

    public Object execute(ConfigSection section, Consumer<String> error, Object... args) {
        if (section == null) return null;
        ConfigStructure structure = structureHashMap.get(section.getName());
        if (structure == null) return null;
        return structure.execute(section, error, args);
    }

    public Object execute(ConfigSection section, Object... args) {
        return this.execute(section, error -> {
            if (errorLogger != null) errorLogger.severe(error);
        }, args);
    }

    public List<Object> executeChildren(ConfigSection section, Consumer<String> error, Object... args) {
        if (section == null) return null;
        List<Object> obs = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            if (!section.isConfigSection(key)) continue;

            ConfigSection sec = section.getConfigSection(key);

            if (sec == null) continue;

            obs.add(execute(sec, error, args));
        }
        return obs;
    }

    public List<Object> executeChildren(ConfigSection section, Object... args) {
        return this.executeChildren(section, error -> {
            if (errorLogger != null) errorLogger.severe(error);
        }, args);
    }

    public List<Object> executeChildrenRecursive(ConfigSection section, Consumer<String> error, Object... args) {
        if (section == null) return null;
        List<Object> obs = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            if (!section.isConfigSection(key)) continue;

            ConfigSection sec = section.getConfigSection(key);

            if (sec == null) continue;

            if (!knows(sec.getName())) {
                Object result = execute(sec, error, args);
                if (result == null) {
                    executeChildrenRecursive(sec, error);
                } else {
                    obs.add(result);
                }
                continue;
            }

            obs.add(execute(sec, error, args));
        }
        return obs;
    }

    public List<Object> executeChildrenRecursive(ConfigSection section, Object... args) {
        return this.executeChildrenRecursive(section, error -> {
            if (errorLogger != null) errorLogger.severe(error);
        }, args);
    }
}
