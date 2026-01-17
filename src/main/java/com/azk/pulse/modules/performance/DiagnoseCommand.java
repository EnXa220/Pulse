package com.azk.pulse.modules.performance;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class DiagnoseCommand implements PulseSubcommand {
    private final JavaPlugin plugin;
    private final PerformanceMetrics metrics;
    private final PerformanceHistory history;
    private final ConfigFiles configFiles;

    public DiagnoseCommand(JavaPlugin plugin, PerformanceMetrics metrics, PerformanceHistory history, ConfigFiles configFiles) {
        this.plugin = plugin;
        this.metrics = metrics;
        this.history = history;
        this.configFiles = configFiles;
    }

    @Override
    public String name() {
        return "diagnose";
    }

    @Override
    public String module() {
        return "performance";
    }

    @Override
    public String permission() {
        return "pulse.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageUtil.sendTitleKey(sender, plugin, "diagnose.title");
        int chunkLimit = configFiles.getMain().getInt("performance.diagnose.max-chunks-scan",
                configFiles.getMain().getInt("performance.lag.max-chunks-scan", 2000));
        int blockSamples = configFiles.getMain().getInt("performance.diagnose.block-samples-per-chunk", 256);
        DiagnosticScanner scanner = new DiagnosticScanner(chunkLimit, blockSamples);
        DiagnosticScanner.DiagnosticSnapshot snapshot = scanner.scan();
        DiagnosticAnalyzer analyzer = new DiagnosticAnalyzer(plugin, metrics, history, configFiles);
        DiagnosticReport report = analyzer.analyze(snapshot);

        MessageUtil.sendSectionKey(sender, plugin, "diagnose.causes.title");
        if (report.getIssues().isEmpty()) {
            MessageUtil.sendKey(sender, plugin, "diagnose.causes.none");
        } else {
            for (String issue : report.getIssues()) {
                MessageUtil.sendKey(sender, plugin, "general.list-item", java.util.Map.of("item", issue));
            }
        }

        if (!report.getPatterns().isEmpty()) {
            MessageUtil.sendSectionKey(sender, plugin, "diagnose.patterns.title");
            for (String pattern : report.getPatterns()) {
                MessageUtil.sendKey(sender, plugin, "general.list-item", java.util.Map.of("item", pattern));
            }
        }

        MessageUtil.sendSectionKey(sender, plugin, "diagnose.recommendations.title");
        for (String tip : report.getRecommendations()) {
            MessageUtil.sendKey(sender, plugin, "general.list-item", java.util.Map.of("item", tip));
        }

        HealthEvaluator.HealthResult health = report.getHealth();
        if (health != null) {
            String level = localizeHealthLevel(health.getLevel());
            MessageUtil.sendKeyValueKey(sender, plugin, "diagnose.health-score",
                    health.getScore() + "/100 (" + level + ")");
            if (!health.getReasons().isEmpty()) {
                MessageUtil.sendKeyValueKey(sender, plugin, "diagnose.health-factors",
                        String.join(", ", localizeHealthReasons(health.getReasons())));
            }
        }

        MessageUtil.sendKeyValueKey(sender, plugin, "diagnose.risk",
                MessageUtil.tr(plugin, "diagnose.risk." + report.getRisk().toLowerCase(Locale.ROOT), report.getRisk()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    private String localizeHealthLevel(String level) {
        if (level == null || level.isBlank()) {
            return "";
        }
        return MessageUtil.tr(plugin, "health.level." + level.toLowerCase(Locale.ROOT), level);
    }

    private List<String> localizeHealthReasons(List<String> reasons) {
        return reasons.stream()
                .map(reason -> MessageUtil.tr(plugin, "health.reason." + reason.toLowerCase(Locale.ROOT), reason))
                .toList();
    }
}
