package com.azk.pulse.core;

import com.azk.pulse.commands.CommandRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ModuleManager {
    private final JavaPlugin plugin;
    private final ConfigFiles configFiles;
    private final CommandRegistry registry;
    private final Map<String, PulseModule> modules = new LinkedHashMap<>();

    public ModuleManager(JavaPlugin plugin, ConfigFiles configFiles, CommandRegistry registry) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.registry = registry;
    }

    public void register(PulseModule module) {
        modules.put(module.getName().toLowerCase(), module);
    }

    public void loadModules() {
        FileConfiguration config = configFiles.getModules();
        for (PulseModule module : modules.values()) {
            String key = "modules." + module.getName().toLowerCase();
            boolean shouldEnable = config.getBoolean(key, true);
            if (shouldEnable && !module.isEnabled()) {
                module.enable();
                module.setEnabled(true);
                plugin.getLogger().info("Enabled module: " + module.getName());
            } else if (!shouldEnable && module.isEnabled()) {
                module.disable();
                module.setEnabled(false);
                registry.unregisterByModule(module.getName());
                plugin.getLogger().info("Disabled module: " + module.getName());
            }
        }
    }

    public void reloadModules() {
        for (PulseModule module : modules.values()) {
            if (module.isEnabled()) {
                module.disable();
                module.setEnabled(false);
                registry.unregisterByModule(module.getName());
            }
        }
        loadModules();
    }

    public boolean isModuleEnabled(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.equalsIgnoreCase("core")) {
            return true;
        }
        return Optional.ofNullable(modules.get(name.toLowerCase()))
                .map(PulseModule::isEnabled)
                .orElse(false);
    }

    public PulseModule getModule(String name) {
        return modules.get(name.toLowerCase());
    }

    public void shutdown() {
        for (PulseModule module : modules.values()) {
            if (module.isEnabled()) {
                module.disable();
                module.setEnabled(false);
            }
        }
    }
}
