package com.azk.pulse.modules.logs;

import com.azk.pulse.core.MessageUtil;
import com.azk.pulse.storage.DatabaseProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

public class LogRepository {
    private final JavaPlugin plugin;
    private final DatabaseProvider storage;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public LogRepository(JavaPlugin plugin, DatabaseProvider storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void init() {
        storage.init();
        createTables();
    }

    public void shutdown() {
        storage.shutdown();
    }

    public void logBlock(String player, String world, int x, int y, int z, String action, String material) {
        runAsync(() -> {
            String sql = "INSERT INTO block_log(player, world, x, y, z, action, material, time) VALUES(?,?,?,?,?,?,?,?)";
            try (Connection connection = storage.openConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player);
                statement.setString(2, world);
                statement.setInt(3, x);
                statement.setInt(4, y);
                statement.setInt(5, z);
                statement.setString(6, action);
                statement.setString(7, material);
                statement.setLong(8, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to log block event: " + ex.getMessage());
            }
        });
    }

    public void logChest(String player, String world, int x, int y, int z, String action, String containerType) {
        runAsync(() -> {
            String sql = "INSERT INTO chest_log(player, world, x, y, z, action, container, time) VALUES(?,?,?,?,?,?,?,?)";
            try (Connection connection = storage.openConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player);
                statement.setString(2, world);
                statement.setInt(3, x);
                statement.setInt(4, y);
                statement.setInt(5, z);
                statement.setString(6, action);
                statement.setString(7, containerType);
                statement.setLong(8, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to log chest event: " + ex.getMessage());
            }
        });
    }

    public void logCommand(String player, String command) {
        runAsync(() -> {
            String sql = "INSERT INTO command_log(player, command, time) VALUES(?,?,?)";
            try (Connection connection = storage.openConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player);
                statement.setString(2, command);
                statement.setLong(3, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to log command: " + ex.getMessage());
            }
        });
    }

    public void logDeath(String player, String killer, String cause) {
        runAsync(() -> {
            String sql = "INSERT INTO death_log(player, killer, cause, time) VALUES(?,?,?,?)";
            try (Connection connection = storage.openConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player);
                statement.setString(2, killer);
                statement.setString(3, cause);
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to log death: " + ex.getMessage());
            }
        });
    }

    public List<LogEntry> fetchRecent(LookupQuery query) {
        int safeLimit = Math.max(1, Math.min(query.limit(), 200));
        List<LogEntry> entries = new ArrayList<>();
        boolean hasActionFilter = query.action() != null && !query.action().isBlank();
        boolean hasMaterialFilter = query.material() != null && !query.material().isBlank();

        if (query.type() == null || query.type() == LogType.BLOCKS) {
            entries.addAll(fetchBlocks(query, safeLimit));
        }
        if (query.type() == null || query.type() == LogType.CHESTS) {
            entries.addAll(fetchChests(query, safeLimit));
        }
        if (query.type() == LogType.COMMANDS || (query.type() == null && !hasActionFilter && !hasMaterialFilter)) {
            entries.addAll(fetchCommands(query, safeLimit));
        }
        if (query.type() == LogType.DEATHS || (query.type() == null && !hasActionFilter && !hasMaterialFilter)) {
            entries.addAll(fetchDeaths(query, safeLimit));
        }

        if (query.type() == null) {
            entries.sort(Comparator.comparingLong(LogEntry::timestamp).reversed());
            if (entries.size() > safeLimit) {
                return new ArrayList<>(entries.subList(0, safeLimit));
            }
        }

        return entries;
    }

    private List<LogEntry> fetchBlocks(LookupQuery query, int limit) {
        List<LogEntry> entries = new ArrayList<>();
        QueryStatement statementData = buildLocationQuery(
                "SELECT player, action, material, world, x, y, z, time FROM block_log",
                query, limit, true, "action", "material");

        try (Connection connection = storage.openConnection();
             PreparedStatement statement = connection.prepareStatement(statementData.sql())) {
            statementData.apply(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String player = resultSet.getString("player");
                    String action = resultSet.getString("action");
                    String material = resultSet.getString("material");
                    String world = resultSet.getString("world");
                    int x = resultSet.getInt("x");
                    int y = resultSet.getInt("y");
                    int z = resultSet.getInt("z");
                    long time = resultSet.getLong("time");
                    String typeLabel = MessageUtil.tr(plugin, "lookup.log.block.type", "BLOCK");
                    String actionLabel = MessageUtil.tr(plugin,
                            "lookup.action." + action.toLowerCase(Locale.ROOT), action);
                    String message = MessageUtil.tr(plugin, "lookup.log.block.line", java.util.Map.of(
                            "time", formatTime(time),
                            "type", typeLabel,
                            "player", player,
                            "action", actionLabel,
                            "material", material,
                            "world", world,
                            "x", Integer.toString(x),
                            "y", Integer.toString(y),
                            "z", Integer.toString(z)
                    ));
                    entries.add(new LogEntry(time, typeLabel, message, world, x, y, z));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to fetch block logs: " + ex.getMessage());
        }
        return entries;
    }

    private List<LogEntry> fetchChests(LookupQuery query, int limit) {
        List<LogEntry> entries = new ArrayList<>();
        QueryStatement statementData = buildLocationQuery(
                "SELECT player, action, container, world, x, y, z, time FROM chest_log",
                query, limit, true, "action", "container");

        try (Connection connection = storage.openConnection();
             PreparedStatement statement = connection.prepareStatement(statementData.sql())) {
            statementData.apply(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String player = resultSet.getString("player");
                    String action = resultSet.getString("action");
                    String container = resultSet.getString("container");
                    String world = resultSet.getString("world");
                    int x = resultSet.getInt("x");
                    int y = resultSet.getInt("y");
                    int z = resultSet.getInt("z");
                    long time = resultSet.getLong("time");
                    String typeLabel = MessageUtil.tr(plugin, "lookup.log.chest.type", "CHEST");
                    String actionLabel = MessageUtil.tr(plugin,
                            "lookup.action." + action.toLowerCase(Locale.ROOT), action);
                    String message = MessageUtil.tr(plugin, "lookup.log.chest.line", java.util.Map.of(
                            "time", formatTime(time),
                            "type", typeLabel,
                            "player", player,
                            "action", actionLabel,
                            "container", container,
                            "world", world,
                            "x", Integer.toString(x),
                            "y", Integer.toString(y),
                            "z", Integer.toString(z)
                    ));
                    entries.add(new LogEntry(time, typeLabel, message, world, x, y, z));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to fetch chest logs: " + ex.getMessage());
        }
        return entries;
    }

    private List<LogEntry> fetchCommands(LookupQuery query, int limit) {
        List<LogEntry> entries = new ArrayList<>();
        QueryStatement statementData = buildSimpleQuery(
                "SELECT player, command, time FROM command_log", query, limit);
        try (Connection connection = storage.openConnection();
             PreparedStatement statement = connection.prepareStatement(statementData.sql())) {
            statementData.apply(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String player = resultSet.getString("player");
                    String command = resultSet.getString("command");
                    long time = resultSet.getLong("time");
                    String typeLabel = MessageUtil.tr(plugin, "lookup.log.command.type", "COMMAND");
                    String message = MessageUtil.tr(plugin, "lookup.log.command.line", java.util.Map.of(
                            "time", formatTime(time),
                            "type", typeLabel,
                            "player", player,
                            "command", command
                    ));
                    entries.add(new LogEntry(time, typeLabel, message, null, null, null, null));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to fetch command logs: " + ex.getMessage());
        }
        return entries;
    }

    private List<LogEntry> fetchDeaths(LookupQuery query, int limit) {
        List<LogEntry> entries = new ArrayList<>();
        QueryStatement statementData = buildSimpleQuery(
                "SELECT player, killer, cause, time FROM death_log", query, limit);
        try (Connection connection = storage.openConnection();
             PreparedStatement statement = connection.prepareStatement(statementData.sql())) {
            statementData.apply(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String player = resultSet.getString("player");
                    String killer = resultSet.getString("killer");
                    String cause = resultSet.getString("cause");
                    long time = resultSet.getLong("time");
                    String typeLabel = MessageUtil.tr(plugin, "lookup.log.death.type", "DEATH");
                    String message = MessageUtil.tr(plugin, "lookup.log.death.line", java.util.Map.of(
                            "time", formatTime(time),
                            "type", typeLabel,
                            "player", player,
                            "cause", cause,
                            "killer", killer
                    ));
                    entries.add(new LogEntry(time, typeLabel, message, null, null, null, null));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to fetch death logs: " + ex.getMessage());
        }
        return entries;
    }

    private QueryStatement buildLocationQuery(String baseSql, LookupQuery query, int limit, boolean includeLocation,
                                              String actionColumn, String materialColumn) {
        StringBuilder sql = new StringBuilder(baseSql + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query.player() != null) {
            sql.append(" AND LOWER(player) = LOWER(?)");
            params.add(query.player());
        }
        if (query.since() != null) {
            sql.append(" AND time >= ?");
            params.add(query.since().toEpochMilli());
        }
        if (includeLocation && query.world() != null) {
            sql.append(" AND world = ?");
            params.add(query.world());
        }
        if (actionColumn != null && query.action() != null && !query.action().isBlank()) {
            sql.append(" AND LOWER(").append(actionColumn).append(") = LOWER(?)");
            params.add(query.action());
        }
        if (materialColumn != null && query.material() != null && !query.material().isBlank()) {
            sql.append(" AND LOWER(").append(materialColumn).append(") = LOWER(?)");
            params.add(query.material());
        }
        boolean hasBox = includeLocation
                && query.minX() != null && query.maxX() != null
                && query.minZ() != null && query.maxZ() != null;
        if (hasBox) {
            sql.append(" AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?");
            params.add(query.minX());
            params.add(query.maxX());
            params.add(query.minZ());
            params.add(query.maxZ());
            if (query.minY() != null && query.maxY() != null) {
                sql.append(" AND y BETWEEN ? AND ?");
                params.add(query.minY());
                params.add(query.maxY());
            }
        } else if (includeLocation && query.radius() != null && query.radius() > 0 && query.x() != null && query.z() != null) {
            int radius = query.radius();
            sql.append(" AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?");
            params.add(query.x() - radius);
            params.add(query.x() + radius);
            params.add(query.z() - radius);
            params.add(query.z() + radius);
            if (query.y() != null) {
                sql.append(" AND y = ?");
                params.add(query.y());
            }
        } else if (includeLocation && query.x() != null && query.z() != null) {
            sql.append(" AND x = ? AND z = ?");
            params.add(query.x());
            params.add(query.z());
            if (query.y() != null) {
                sql.append(" AND y = ?");
                params.add(query.y());
            }
        } else if (includeLocation && query.y() != null) {
            sql.append(" AND y = ?");
            params.add(query.y());
        }

        int offset = Math.max(0, query.offset());
        sql.append(" ORDER BY time DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return new QueryStatement(sql.toString(), params);
    }

    private QueryStatement buildSimpleQuery(String baseSql, LookupQuery query, int limit) {
        StringBuilder sql = new StringBuilder(baseSql + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query.player() != null) {
            sql.append(" AND LOWER(player) = LOWER(?)");
            params.add(query.player());
        }
        if (query.since() != null) {
            sql.append(" AND time >= ?");
            params.add(query.since().toEpochMilli());
        }

        int offset = Math.max(0, query.offset());
        sql.append(" ORDER BY time DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return new QueryStatement(sql.toString(), params);
    }

    private void createTables() {
        try (Connection connection = storage.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS block_log ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "player TEXT NOT NULL,"
                            + "world TEXT NOT NULL,"
                            + "x INTEGER, y INTEGER, z INTEGER,"
                            + "action TEXT, material TEXT,"
                            + "time BIGINT)"
            );
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_block_player_time ON block_log(player, time)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_block_world_time ON block_log(world, time)");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS chest_log ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "player TEXT NOT NULL,"
                            + "world TEXT NOT NULL,"
                            + "x INTEGER, y INTEGER, z INTEGER,"
                            + "action TEXT, container TEXT,"
                            + "time BIGINT)"
            );
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chest_player_time ON chest_log(player, time)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chest_world_time ON chest_log(world, time)");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS command_log ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "player TEXT NOT NULL,"
                            + "command TEXT,"
                            + "time BIGINT)"
            );
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_command_player_time ON command_log(player, time)");

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS death_log ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "player TEXT NOT NULL,"
                            + "killer TEXT,"
                            + "cause TEXT,"
                            + "time BIGINT)"
            );
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_death_player_time ON death_log(player, time)");
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to initialize log tables: " + ex.getMessage());
        }
    }

    private void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    private String formatTime(long millis) {
        return formatter.format(Instant.ofEpochMilli(millis));
    }

    public enum LogType {
        BLOCKS,
        CHESTS,
        COMMANDS,
        DEATHS;

        public static LogType fromString(String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            String value = input.trim().toUpperCase(Locale.ROOT);
            for (LogType type : values()) {
                if (type.name().equals(value)) {
                    return type;
                }
            }
            return null;
        }
    }

    public record LogEntry(long timestamp, String type, String message, String world, Integer x, Integer y, Integer z) {
    }

    public record LookupQuery(String player, LogType type, String world, Integer x, Integer y, Integer z,
                              Integer radius, Integer minX, Integer maxX, Integer minY, Integer maxY,
                              Integer minZ, Integer maxZ, String action, String material, Instant since,
                              int limit, int offset) {
    }

    private record QueryStatement(String sql, List<Object> params) {
        private void apply(PreparedStatement statement) throws SQLException {
            int index = 1;
            for (Object value : params) {
                statement.setObject(index++, value);
            }
        }
    }
}
