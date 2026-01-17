package com.azk.pulse.modules.performance;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.MessageUtil;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class HistoryCommand implements PulseSubcommand {
    private final JavaPlugin plugin;
    private final PerformanceHistory history;

    public HistoryCommand(JavaPlugin plugin, PerformanceHistory history) {
        this.plugin = plugin;
        this.history = history;
    }

    @Override
    public String name() {
        return "history";
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
        MessageUtil.sendTitleKey(sender, plugin, "history.title");
        if (args.length >= 1 && args[0].equalsIgnoreCase("export")) {
            Duration range = args.length >= 2 ? parseRange(args[1]) : Duration.ofHours(24);
            List<PerformanceHistory.HistorySample> samples = history.getSamplesSince(range);
            if (samples.isEmpty()) {
                MessageUtil.sendWarningKey(sender, plugin, "history.export.no-data");
                return true;
            }
            String name = "history-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now()) + ".csv";
            File file = new File(plugin.getDataFolder(), name);
            try {
                history.exportCsv(file, samples);
                MessageUtil.sendSuccessKey(sender, plugin, "history.export.success",
                        java.util.Map.of("path", file.getAbsolutePath()));
            } catch (IOException ex) {
                MessageUtil.sendErrorKey(sender, plugin, "history.export.failed",
                        java.util.Map.of("error", ex.getMessage()));
            }
            return true;
        }

        Duration range = args.length >= 1 ? parseRange(args[0]) : Duration.ofHours(24);
        List<PerformanceHistory.HistorySample> samples = history.getSamplesSince(range);
        if (samples.isEmpty()) {
            MessageUtil.sendWarningKey(sender, plugin, "history.no-data");
            return true;
        }

        double minTps = Double.MAX_VALUE;
        double maxTps = 0.0;
        double totalTps = 0.0;
        int tpsCount = 0;
        double totalMspt = 0.0;
        int msptCount = 0;
        double totalRam = 0.0;
        int ramCount = 0;
        int totalPlayers = 0;
        int counted = 0;

        for (PerformanceHistory.HistorySample sample : samples) {
            double tps = sample.getTps1();
            if (tps > 0) {
                minTps = Math.min(minTps, tps);
                maxTps = Math.max(maxTps, tps);
                totalTps += tps;
                tpsCount++;
            }
            if (sample.getMspt() > 0) {
                totalMspt += sample.getMspt();
                msptCount++;
            }
            if (sample.getMaxMemory() > 0) {
                totalRam += (sample.getUsedMemory() / (double) sample.getMaxMemory()) * 100.0;
                ramCount++;
            }
            totalPlayers += sample.getPlayers();
            counted++;
        }

        if (counted == 0) {
            MessageUtil.sendWarningKey(sender, plugin, "history.no-usable-data");
            return true;
        }

        double avgTps = tpsCount > 0 ? totalTps / tpsCount : 0.0;
        double avgMspt = msptCount > 0 ? totalMspt / msptCount : 0.0;
        double avgRam = ramCount > 0 ? totalRam / ramCount : 0.0;
        double avgPlayers = totalPlayers / (double) counted;
        double minTpsValue = tpsCount > 0 ? minTps : 0.0;
        double maxTpsValue = tpsCount > 0 ? maxTps : 0.0;

        MessageUtil.sendSectionKey(sender, plugin, "history.summary.title",
                java.util.Map.of("range", formatRange(range)));
        MessageUtil.sendKeyValueKey(sender, plugin, "history.tps-summary",
                String.format(Locale.US, "%.2f / %.2f / %.2f", avgTps, minTpsValue, maxTpsValue));
        MessageUtil.sendKeyValueKey(sender, plugin, "history.avg-mspt", String.format(Locale.US, "%.2f ms", avgMspt));
        MessageUtil.sendKeyValueKey(sender, plugin, "history.avg-ram", String.format(Locale.US, "%.1f%%", avgRam));
        MessageUtil.sendKeyValueKey(sender, plugin, "history.avg-players", String.format(Locale.US, "%.1f", avgPlayers));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("24h", "7d", "30d", "export");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("export")) {
            return List.of("24h", "7d", "30d");
        }
        return List.of();
    }

    private Duration parseRange(String input) {
        if (input == null) {
            return Duration.ofHours(24);
        }
        String value = input.toLowerCase(Locale.ROOT).trim();
        if (value.endsWith("h")) {
            return Duration.ofHours(parseInt(value.substring(0, value.length() - 1), 24));
        }
        if (value.endsWith("d")) {
            return Duration.ofDays(parseInt(value.substring(0, value.length() - 1), 1));
        }
        return Duration.ofHours(24);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String formatRange(Duration duration) {
        long hours = duration.toHours();
        if (hours >= 24 && hours % 24 == 0) {
            return (hours / 24) + "d";
        }
        return hours + "h";
    }
}
