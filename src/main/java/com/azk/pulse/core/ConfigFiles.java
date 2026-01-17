package com.azk.pulse.core;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigFiles {
    private final JavaPlugin plugin;
    private FileConfiguration modulesConfig;
    private FileConfiguration alertsConfig;
    private File modulesFile;
    private File alertsFile;

    public ConfigFiles(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        modulesConfig = loadConfig("modules.yml");
        alertsConfig = loadConfig("alerts.yml");
    }

    public void reloadAll() {
        plugin.reloadConfig();
        modulesConfig = loadConfig("modules.yml");
        alertsConfig = loadConfig("alerts.yml");
    }

    public FileConfiguration getMain() {
        return plugin.getConfig();
    }

    public FileConfiguration getModules() {
        return modulesConfig;
    }

    public FileConfiguration getAlerts() {
        return alertsConfig;
    }

    public void saveModules() {
        if (modulesConfig == null || modulesFile == null) {
            return;
        }
        try {
            modulesConfig.save(modulesFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save modules.yml: " + ex.getMessage());
        }
    }

    public void saveAlerts() {
        if (alertsConfig == null || alertsFile == null) {
            return;
        }
        try {
            alertsConfig.save(alertsFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save alerts.yml: " + ex.getMessage());
        }
    }

    private FileConfiguration loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        if (name.equals("modules.yml")) {
            modulesFile = file;
        } else if (name.equals("alerts.yml")) {
            alertsFile = file;
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
