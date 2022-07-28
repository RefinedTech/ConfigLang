package me.ikevoodoo.configlang;

import me.ikevoodoo.configlang.scope.Scope;

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

    public Object getData(ConfigSection section, Consumer<String> error, String key, Scope scope, Object... args) {
        Object obj = section.getObject(key);
        if (obj instanceof ConfigSection) {
            ConfigSection subSection = (ConfigSection) obj;
            Optional<ConfigStructure> structure = this.get(subSection.getName());
            if (structure.isPresent()) {
                return structure.get().execute(subSection, error, scope, args);
            }

            return this.getData(subSection, error, key, scope, args);
        }
        Optional<ConfigStructure> structure = this.get(key);
        if (structure.isPresent()) {
            return structure.get().execute(section, error, scope, args);
        }

        Optional<ConfigSection> configSection = section.getConfigSection(key);
        if (configSection.isPresent()) {
            return this.executeChildrenRecursive(configSection.get(), error, scope, args);
        }

        return section.getObject(key);
    }

    public Object getData(ConfigSection section, String key, Scope scope, Object... args) {
        return this.getData(section, error -> {
            if (errorLogger != null) errorLogger.severe(error);
        }, key, scope, args);
    }

    public Object execute(ConfigSection section, Consumer<String> error, Scope scope, Object... args) {
        if (section == null) return null;
        ConfigStructure structure = structureHashMap.get(section.getName());
        if (structure == null) return null;
        return structure.execute(section, error, scope, args);
    }

    public Object execute(ConfigSection section, Scope scope, Object... args) {
        return this.execute(section, error -> {
            if (errorLogger != null) errorLogger.severe(error);
        }, scope, args);
    }

    public List<Object> executeChildren(ConfigSection section, Consumer<String> error, Scope scope, Object... args) {
        if (section == null) return null;
        List<Object> obs = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            Optional<ConfigSection> opt = section.getConfigSection(key);

            if (opt.isEmpty()) continue;

            obs.add(execute(opt.get(), error, scope, args));
        }
        return obs;
    }

    public List<Object> executeChildren(ConfigSection section, Scope scope, Object... args) {
        return this.executeChildren(section, error -> {
            if (errorLogger != null) errorLogger.severe(error);
        }, scope, args);
    }

    public List<Object> executeChildrenRecursive(ConfigSection section, Consumer<String> error, Scope scope, Object... args) {
        if (section == null) return null;
        List<Object> obs = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            if (!section.isConfigSection(key)) continue;

            Optional<ConfigSection> opt = section.getConfigSection(key);

            if (opt.isEmpty()) continue;

            ConfigSection sec = opt.get();

            if (!knows(sec.getName())) {
                Object result = execute(sec, error, scope, args);
                if (result == null) {
                    executeChildrenRecursive(sec, error, scope, args);
                } else {
                    obs.add(result);
                }
                continue;
            }

            obs.add(execute(sec, error, scope, args));
        }
        return obs;
    }

    public List<Object> executeChildrenRecursive(ConfigSection section, Scope scope, Object... args) {
        return this.executeChildrenRecursive(section, error -> {
            if (errorLogger != null) errorLogger.severe(error);
        }, scope, args);
    }
}
