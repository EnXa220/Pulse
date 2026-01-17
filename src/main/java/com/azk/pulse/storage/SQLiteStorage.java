package com.azk.pulse.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.bukkit.plugin.java.JavaPlugin;

public class SQLiteStorage implements DatabaseProvider {
    private final JavaPlugin plugin;
    private final String fileName;
    private File databaseFile;

    public SQLiteStorage(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    @Override
    public void init() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        databaseFile = new File(plugin.getDataFolder(), fileName);
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().warning("SQLite JDBC driver not found.");
        }
    }

    @Override
    public Connection openConnection() throws SQLException {
        if (databaseFile == null) {
            init();
        }
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    @Override
    public void shutdown() {
    }
}
