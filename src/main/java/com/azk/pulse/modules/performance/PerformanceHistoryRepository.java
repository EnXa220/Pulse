package com.azk.pulse.modules.performance;

import com.azk.pulse.storage.DatabaseProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public class PerformanceHistoryRepository {
    private final JavaPlugin plugin;
    private final DatabaseProvider storage;

    public PerformanceHistoryRepository(JavaPlugin plugin, DatabaseProvider storage) {
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

    public void saveSampleAsync(PerformanceHistory.HistorySample sample) {
        if (sample == null) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveSample(sample));
    }

    public List<PerformanceHistory.HistorySample> loadSince(Instant cutoff) {
        List<PerformanceHistory.HistorySample> samples = new ArrayList<>();
        String sql = "SELECT time, tps1, tps5, tps15, mspt, used_memory, max_memory, players "
                + "FROM perf_history WHERE time >= ? ORDER BY time ASC";
        try (Connection connection = storage.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cutoff.toEpochMilli());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Instant timestamp = Instant.ofEpochMilli(resultSet.getLong("time"));
                    double tps1 = resultSet.getDouble("tps1");
                    double tps5 = resultSet.getDouble("tps5");
                    double tps15 = resultSet.getDouble("tps15");
                    double mspt = resultSet.getDouble("mspt");
                    long used = resultSet.getLong("used_memory");
                    long max = resultSet.getLong("max_memory");
                    int players = resultSet.getInt("players");
                    samples.add(new PerformanceHistory.HistorySample(timestamp, tps1, tps5, tps15, mspt, used, max, players));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to load performance history: " + ex.getMessage());
        }
        return samples;
    }

    public void deleteOlderThan(Instant cutoff) {
        String sql = "DELETE FROM perf_history WHERE time < ?";
        try (Connection connection = storage.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cutoff.toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to prune performance history: " + ex.getMessage());
        }
    }

    private void saveSample(PerformanceHistory.HistorySample sample) {
        String sql = "INSERT INTO perf_history(time, tps1, tps5, tps15, mspt, used_memory, max_memory, players) "
                + "VALUES(?,?,?,?,?,?,?,?)";
        try (Connection connection = storage.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sample.getTimestamp().toEpochMilli());
            statement.setDouble(2, sample.getTps1());
            statement.setDouble(3, sample.getTps5());
            statement.setDouble(4, sample.getTps15());
            statement.setDouble(5, sample.getMspt());
            statement.setLong(6, sample.getUsedMemory());
            statement.setLong(7, sample.getMaxMemory());
            statement.setInt(8, sample.getPlayers());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to save performance history: " + ex.getMessage());
        }
    }

    private void createTables() {
        try (Connection connection = storage.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS perf_history ("
                            + "time BIGINT NOT NULL,"
                            + "tps1 DOUBLE,"
                            + "tps5 DOUBLE,"
                            + "tps15 DOUBLE,"
                            + "mspt DOUBLE,"
                            + "used_memory BIGINT,"
                            + "max_memory BIGINT,"
                            + "players INTEGER)"
            );
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_perf_time ON perf_history(time)");
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to initialize perf history table: " + ex.getMessage());
        }
    }
}
