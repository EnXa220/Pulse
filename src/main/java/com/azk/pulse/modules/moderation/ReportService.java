package com.azk.pulse.modules.moderation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ReportService {
    private final JavaPlugin plugin;
    private final File file;
    private final List<ReportEntry> reports = new ArrayList<>();

    public ReportService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reports.yml");
    }

    public void load() {
        reports.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> entries = config.getMapList("reports");
        for (Map<?, ?> entry : entries) {
            String id = String.valueOf(entry.get("id"));
            String reporter = String.valueOf(entry.get("reporter"));
            String target = String.valueOf(entry.get("target"));
            String reason = String.valueOf(entry.get("reason"));
            long timestamp = toLong(entry.get("timestamp"));
            reports.add(new ReportEntry(id, reporter, target, reason, timestamp));
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> entries = new ArrayList<>();
        for (ReportEntry report : reports) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", report.id());
            entry.put("reporter", report.reporter());
            entry.put("target", report.target());
            entry.put("reason", report.reason());
            entry.put("timestamp", report.timestamp());
            entries.add(entry);
        }
        config.set("reports", entries);
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save reports: " + ex.getMessage());
        }
    }

    public void addReport(ReportEntry entry) {
        reports.add(entry);
        save();
    }

    public List<ReportEntry> getReports() {
        return new ArrayList<>(reports);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public record ReportEntry(String id, String reporter, String target, String reason, long timestamp) {
    }
}
