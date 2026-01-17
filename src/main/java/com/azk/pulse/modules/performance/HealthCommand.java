package com.azk.pulse.modules.performance;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class HealthCommand implements PulseSubcommand {
    private final JavaPlugin plugin;
    private final PerformanceMetrics metrics;
    private final ConfigFiles configFiles;

    public HealthCommand(JavaPlugin plugin, PerformanceMetrics metrics, ConfigFiles configFiles) {
        this.plugin = plugin;
        this.metrics = metrics;
        this.configFiles = configFiles;
    }

    @Override
    public String name() {
        return "health";
    }

    @Override
    public String module() {
        return "performance";
    }

    @Override
    public String permission() {
        return "pulse.view";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageUtil.sendTitleKey(sender, plugin, "health.title");
        int chunkLimit = configFiles.getMain().getInt("performance.lag.max-chunks-scan", 2000);
        PerformanceMetrics.ChunkScanResult scan = metrics.scanChunks(chunkLimit);
        HealthEvaluator.HealthResult health = HealthEvaluator.evaluate(metrics, scan, configFiles);
        String level = MessageUtil.tr(plugin, "health.level." + health.getLevel().toLowerCase(Locale.ROOT), health.getLevel());
        MessageUtil.sendKeyValueKey(sender, plugin, "health.label",
                health.getScore() + "/100 (" + level + ")");
        if (!health.getReasons().isEmpty()) {
            String factors = health.getReasons().stream()
                    .map(reason -> MessageUtil.tr(plugin, "health.reason." + reason.toLowerCase(Locale.ROOT), reason))
                    .collect(java.util.stream.Collectors.joining(", "));
            MessageUtil.sendKeyValueKey(sender, plugin, "health.factors", factors);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
