package com.azk.pulse.modules.performance;

import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

public class DiagnosticAnalyzer {
    private final JavaPlugin plugin;
    private final PerformanceMetrics metrics;
    private final PerformanceHistory history;
    private final ConfigFiles configFiles;

    public DiagnosticAnalyzer(JavaPlugin plugin, PerformanceMetrics metrics, PerformanceHistory history, ConfigFiles configFiles) {
        this.plugin = plugin;
        this.metrics = metrics;
        this.history = history;
        this.configFiles = configFiles;
    }

    public DiagnosticReport analyze(DiagnosticScanner.DiagnosticSnapshot snapshot) {
        DiagnosticReport report = new DiagnosticReport();
        HealthEvaluator.HealthResult health = HealthEvaluator.evaluate(metrics,
                metrics.scanChunks(configFiles.getMain().getInt("performance.lag.max-chunks-scan", 2000)),
                configFiles);
        report.setHealth(health);

        double tps = metrics.getTps()[0];
        double mspt = metrics.getAverageTickTime();
        double ramPercent = metrics.getMemoryUsagePercent();

        double warnTps = configFiles.getAlerts().getDouble("alerts.tps.warning", 18.0);
        double critTps = configFiles.getAlerts().getDouble("alerts.tps.critical", 15.0);
        double warnMspt = configFiles.getAlerts().getDouble("alerts.mspt.warning", 45.0);
        double critMspt = configFiles.getAlerts().getDouble("alerts.mspt.critical", 55.0);
        double warnRam = configFiles.getAlerts().getDouble("alerts.ram.warning-percent", 85.0);
        double critRam = configFiles.getAlerts().getDouble("alerts.ram.critical-percent", 95.0);

        if (tps > 0 && tps < warnTps) {
            report.getIssues().add(tr("diagnose.issue.low-tps",
                    Map.of("tps", String.format(Locale.US, "%.2f", tps))));
            report.getRecommendations().add(tr("diagnose.recommend.view-distance"));
            report.getRecommendations().add(tr("diagnose.recommend.check-configs"));
            if (tps < critTps) {
                report.getRecommendations().add(tr("diagnose.recommend.timings"));
            }
        }

        if (mspt > 0 && mspt > warnMspt) {
            report.getIssues().add(tr("diagnose.issue.high-mspt",
                    Map.of("mspt", String.format(Locale.US, "%.2f", mspt))));
            report.getRecommendations().add(tr("diagnose.recommend.reduce-redstone"));
            report.getRecommendations().add(tr("diagnose.recommend.entity-activation"));
            if (mspt > critMspt) {
                report.getRecommendations().add(tr("diagnose.recommend.lower-spawns"));
            }
        }

        if (ramPercent > 0 && ramPercent > warnRam) {
            report.getIssues().add(tr("diagnose.issue.high-ram",
                    Map.of("ram", String.format(Locale.US, "%.1f", ramPercent))));
            report.getRecommendations().add(tr("diagnose.recommend.restart"));
            if (ramPercent > critRam) {
                report.getRecommendations().add(tr("diagnose.recommend.memory-leak"));
            }
        }

        if (snapshot.getEntitiesPerChunk() > configFiles.getAlerts().getInt("alerts.entities-per-chunk.warning", 150)) {
            report.getIssues().add(tr("diagnose.issue.entities-per-chunk",
                    Map.of("entities", String.format(Locale.US, "%.1f", snapshot.getEntitiesPerChunk()))));
            report.getRecommendations().add(tr("diagnose.recommend.limit-farms"));
            report.getRecommendations().add(tr("diagnose.recommend.per-player-spawns"));
        }

        if (snapshot.getHoppersPerChunk() > 4.0) {
            report.getIssues().add(tr("diagnose.issue.hoppers"));
            report.getRecommendations().add(tr("diagnose.recommend.hopper-cooldown"));
            report.getRecommendations().add(tr("diagnose.recommend.hopper-disable-move"));
        }

        if (snapshot.getSpawnersPerChunk() > 0.5) {
            report.getIssues().add(tr("diagnose.issue.spawners"));
            report.getRecommendations().add(tr("diagnose.recommend.spawner-limits"));
        }

        if (snapshot.getTileEntitiesPerChunk() > 12.0) {
            report.getIssues().add(tr("diagnose.issue.tile-entities"));
            report.getRecommendations().add(tr("diagnose.recommend.reduce-containers"));
        }

        if (snapshot.getRedstoneRatio() > 0.15) {
            report.getIssues().add(tr("diagnose.issue.redstone"));
            report.getRecommendations().add(tr("diagnose.recommend.fast-redstone"));
            report.getRecommendations().add(tr("diagnose.recommend.eigencraft"));
        }

        if (snapshot.getForcedChunks() > 20) {
            report.getIssues().add(tr("diagnose.issue.forced-chunks",
                    Map.of("count", Integer.toString(snapshot.getForcedChunks()))));
            report.getRecommendations().add(tr("diagnose.recommend.review-plugins"));
            report.getRecommendations().add(tr("diagnose.recommend.reduce-chunk-tickets"));
        }

        if (snapshot.getChunkTickets() > 100) {
            report.getIssues().add(tr("diagnose.issue.chunk-tickets",
                    Map.of("count", Integer.toString(snapshot.getChunkTickets()))));
            report.getRecommendations().add(tr("diagnose.recommend.disable-chunk-tickets"));
        }

        Map<EntityType, Integer> entityCounts = snapshot.getEntityCounts();
        List<Map.Entry<EntityType, Integer>> topEntities = entityCounts.entrySet().stream()
                .filter(entry -> entry.getKey() != EntityType.PLAYER)
                .sorted(Map.Entry.<EntityType, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        for (Map.Entry<EntityType, Integer> entry : topEntities) {
            if (entry.getValue() >= 200) {
                report.getPatterns().add(tr("diagnose.pattern.possible-farm",
                        Map.of("type", entry.getKey().name(), "count", entry.getValue().toString())));
            }
        }

        PluginLoadAnalyzer pluginLoadAnalyzer = new PluginLoadAnalyzer();
        List<PluginLoadAnalyzer.PluginLoad> pluginLoads = pluginLoadAnalyzer.getTopByScheduledTasks(3);
        if (!pluginLoads.isEmpty()) {
            PluginLoadAnalyzer.PluginLoad top = pluginLoads.get(0);
            if (top.taskCount() >= 10) {
                report.getPatterns().add(tr("diagnose.pattern.scheduler-load",
                        Map.of("plugin", top.name(), "count", Integer.toString(top.taskCount()))));
                report.getRecommendations().add(tr("diagnose.recommend.profiling"));
            }
        }
        if (Bukkit.getPluginManager().getPlugin("spark") != null) {
            report.getRecommendations().add(tr("diagnose.recommend.spark"));
        }

        report.setRisk(calculateRisk(health, history));
        if (report.getHealth().getScore() < 40) {
            report.getRecommendations().add(tr("diagnose.recommend.freeze-risk"));
        }

        if (report.getRecommendations().isEmpty()) {
            report.getRecommendations().add(tr("diagnose.recommend.none"));
        }

        return report;
    }

    private String calculateRisk(HealthEvaluator.HealthResult health, PerformanceHistory history) {
        if (health.getScore() < 40) {
            return "HIGH";
        }
        List<PerformanceHistory.HistorySample> samples = history.getLatestSamples(6);
        if (samples.size() >= 2) {
            double first = samples.get(0).getTps1();
            double last = samples.get(samples.size() - 1).getTps1();
            if (first > 0 && last > 0 && last - first < -1.0) {
                return "ELEVATED";
            }
        }
        return "LOW";
    }

    private String tr(String key) {
        return MessageUtil.tr(plugin, key);
    }

    private String tr(String key, Map<String, String> placeholders) {
        return MessageUtil.tr(plugin, key, placeholders);
    }
}
