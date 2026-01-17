package com.azk.pulse.core;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Lang {
    private final JavaPlugin plugin;
    private FileConfiguration fallbackConfig;
    private FileConfiguration currentConfig;
    private String language;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        language = plugin.getConfig().getString("general.language", "en");
        fallbackConfig = loadConfig("en");
        if (language == null || language.isBlank() || language.equalsIgnoreCase("en")) {
            currentConfig = fallbackConfig;
        } else {
            currentConfig = loadConfig(language);
        }
    }

    public String get(String key) {
        return get(key, key);
    }

    public String get(String key, String fallback) {
        String value = null;
        if (currentConfig != null) {
            value = currentConfig.getString(key);
        }
        if ((value == null || value.isBlank()) && fallbackConfig != null) {
            value = fallbackConfig.getString(key);
        }
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    public List<String> getList(String key) {
        List<String> values = null;
        if (currentConfig != null) {
            values = currentConfig.getStringList(key);
        }
        if ((values == null || values.isEmpty()) && fallbackConfig != null) {
            values = fallbackConfig.getStringList(key);
        }
        if (values == null) {
            return Collections.emptyList();
        }
        return values;
    }

    public String getLanguage() {
        return language;
    }

    private FileConfiguration loadConfig(String code) {
        String filename = "lang/" + code.toLowerCase() + ".yml";
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) {
            plugin.saveResource(filename, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
