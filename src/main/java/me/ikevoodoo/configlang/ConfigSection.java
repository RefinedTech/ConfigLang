package me.ikevoodoo.configlang;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unused")
public interface ConfigSection {

    public String getName();

    public String getPath();

    public boolean isConfigSection(String key);

    public ConfigSection getConfigSection(String key);

    public Set<String> getKeys(boolean deep);

    public boolean contains(String key);

    public <T> Optional<T> get(String key);

    public void set(String key, Object value);

}
