package dev.refinedtech.configlang;

import dev.refinedtech.configlang.scope.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * ConfigStructure represents a structure of a config file.
 * It also contains a list of required children.
 * If the structure is not valid, it will return an error message when attempting to execute.
 * It can be run with the execute method.
 * */
@SuppressWarnings("unused")
public abstract class ConfigStructure {

    private final String key;
    private final List<ConfigStructure> required = new ArrayList<>();
    private final boolean childrenRequired;

    public ConfigStructure(String key, ConfigStructure... children) {
        this(key, children.length > 0, children);
    }

    public ConfigStructure(String key, boolean childrenRequired, ConfigStructure... children) {
        this.key = key;
        this.childrenRequired = childrenRequired;
        Collections.addAll(required, children);
    }

    public static ConfigStructure keyStructure(String key) {
        return new ConfigStructure(key) {
            @Override
            protected Object run(ConfigSection section, Scope scope, Object... args) {
                return null;
            }
        };
    }

    public final Object execute(ConfigSection section, Consumer<String> error, Scope scope, Object... args) {
        String err = this.matches(section);
        if (err != null) {
            if (error != null) error.accept(err);
            return null;
        }

        return this.run(section, scope, args);
    }

    public boolean returnsData() {
        return false;
    }

    protected abstract Object run(ConfigSection section, Scope scope, Object... args);

    public final String matches(ConfigSection configuration) {
        // If the configuration's name is not the same as this key, return false
        if (!configuration.getName().equalsIgnoreCase(this.key)) {
            return String.format("The configuration %s is not of the same name as %s", configuration.getPath(), key());
        }

        for (ConfigStructure required : this.required) {
            if (required.childrenRequired() && !configuration.isConfigSection(required.key())) {
                return String.format("The key %s.%s should be a configuration section, however it %s!",
                        configuration.getPath(),
                        required.key(),
                        configuration.contains(required.key())
                                ? "is not a configuration section"
                                : "does not exist");
            }

            // If the ConfigStructure has required sub-structures
            if (!required.children().isEmpty()) {
                if (!configuration.isConfigSection(required.key())) {
                    return String.format("The key %s.%s required child elements but none found!", configuration.getPath(), required.key());
                }

                // Get the ConfigSection from the current ConfigSection for that ConfigStructure
                Optional<ConfigSection> opt = configuration.getConfigSection(required.key());

                ConfigSection config = opt.orElse(null);

                // If it doesn't exist return false
                if (config == null) {
                    return String.format("Could not fetch configuration %s.%s", configuration.getPath(), required.key());
                }

                // If it exists, and the ConfigStructure does not match that ConfigurationSection return false
                String err = required.matches(config);
                if (err != null) {
                    return err;
                }

                continue;
            }

            // The config structure has no children, so we check if the ConfigurationSection contains the ConfigStructure's key
            // If it doesn't, return false
            if (!configuration.contains(required.key())) {
                return String.format("The configuration %s does not contain the required key %s", configuration.getPath(), required.key());
            }
        }

        // The structure of the ConfigurationSection is correct
        return null;
    }

    public final String key() {
        return key;
    }

    public final List<ConfigStructure> children() {
        return required;
    }

    public final boolean childrenRequired() {
        return childrenRequired;
    }
}
