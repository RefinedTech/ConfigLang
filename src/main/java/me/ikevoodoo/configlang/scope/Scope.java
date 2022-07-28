package me.ikevoodoo.configlang.scope;

import me.ikevoodoo.configlang.ConfigLang;
import me.ikevoodoo.configlang.ConfigSection;
import me.ikevoodoo.configlang.variables.VariableStorage;

public class Scope {

    private final VariableStorage variables = new VariableStorage();

    private final String name;

    public Scope(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public VariableStorage variables() {
        return variables;
    }

    public Scope childScope(String name) {
        Scope scope = new Scope(name);
        scope.variables().copy(this.variables());
        return scope;
    }

    public void execute(ConfigLang lang, ConfigSection section) {
        lang.execute(section, this);
    }

}
