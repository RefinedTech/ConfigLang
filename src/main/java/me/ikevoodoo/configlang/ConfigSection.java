package me.ikevoodoo.configlang;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unused")
public abstract class ConfigSection {

    public abstract String getName();

    public abstract String getPath();

    public abstract boolean isConfigSection(String key);

    public abstract Optional<ConfigSection> getConfigSection(String key);

    public abstract Set<String> getKeys(boolean deep);

    public abstract boolean contains(String key);

    public abstract <T> Optional<T> get(String key);

    public abstract Object getObject(String key);

    public abstract void set(String key, Object value);

    public String treeString(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" (Section)");
        sb.append("\n");
        ++depth;
        for (String key : getKeys(false)) {
            sb.append("  ".repeat(Math.max(0, depth)));
            sb.append(key);
            if (isConfigSection(key)) {
                sb.append(" ↓ ");
                sb.append("\n");
                sb.append("  ".repeat(Math.max(0, depth) + 1));
                sb.append(getConfigSection(key).get().treeString(depth + 1));
            } else {
                sb.append(" = ");
                sb.append(getObject(key));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public final String treeString() {
        return treeString(0);
    }

}
