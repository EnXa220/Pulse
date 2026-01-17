package com.azk.pulse.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.bukkit.plugin.java.JavaPlugin;

public class MySQLStorage implements DatabaseProvider {
    private final JavaPlugin plugin;
    private final String jdbcUrl;
    private final Properties properties = new Properties();

    public MySQLStorage(JavaPlugin plugin, String host, int port, String database, String user, String password, boolean useSsl) {
        this.plugin = plugin;
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSsl + "&serverTimezone=UTC";
        properties.setProperty("user", user);
        properties.setProperty("password", password);
    }

    @Override
    public void init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().warning("MySQL JDBC driver not found.");
        }
    }

    @Override
    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, properties);
    }

    @Override
    public void shutdown() {
    }
}
