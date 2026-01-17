package com.azk.pulse.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class StorageFactory {
    private StorageFactory() {
    }

    public static DatabaseProvider create(JavaPlugin plugin, FileConfiguration config) {
        String type = config.getString("storage.type", "sqlite");
        if (type != null && type.equalsIgnoreCase("mysql")) {
            String host = config.getString("storage.mysql.host", "localhost");
            int port = config.getInt("storage.mysql.port", 3306);
            String database = config.getString("storage.mysql.database", "pulse");
            String user = config.getString("storage.mysql.user", "root");
            String password = config.getString("storage.mysql.password", "");
            boolean useSsl = config.getBoolean("storage.mysql.use-ssl", false);
            return new MySQLStorage(plugin, host, port, database, user, password, useSsl);
        }
        return new SQLiteStorage(plugin, "pulse.db");
    }
}
