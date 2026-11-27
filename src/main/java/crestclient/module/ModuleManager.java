package crestclient.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {}

    public static ModuleManager getInstance() { return INSTANCE; }

    public void register(Module module) { modules.add(module); }

    public List<Module> modules() { return Collections.unmodifiableList(modules); }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) {
            if (type.isInstance(m)) return type.cast(m);
        }
        return null;
    }
}
