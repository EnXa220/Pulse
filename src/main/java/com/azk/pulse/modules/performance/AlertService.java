package com.azk.pulse.modules.performance;

import com.azk.pulse.PulsePlugin;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.DiscordWebhook;
import com.azk.pulse.core.MessageUtil;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class AlertService {
    private final PulsePlugin plugin;
    private final ConfigFiles configFiles;
    private final PerformanceMetrics metrics;
    private final Map<String, AlertState> states = new HashMap<>();
    private BukkitTask task;

    public AlertService(PulsePlugin plugin, ConfigFiles configFiles, PerformanceMetrics metrics) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.metrics = metrics;
    }

    public void start() {
        boolean enabled = configFiles.getAlerts().getBoolean("alerts.enabled", true);
        if (!enabled) {
            return;
        }
        int interval = Math.max(10, configFiles.getAlerts().getInt("alerts.check-interval-seconds", 30));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAlerts, 20L, interval * 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void checkAlerts() {
        double[] tps = metrics.getTps();
        double tps1 = tps[0];
        double mspt = metrics.getAverageTickTime();
        double ramPercent = metrics.getMemoryUsagePercent();
        int chunkLimit = configFiles.getMain().getInt("performance.lag.max-chunks-scan", 2000);
        PerformanceMetrics.ChunkScanResult scan = metrics.scanChunks(chunkLimit);

        double warnTps = configFiles.getAlerts().getDouble("alerts.tps.warning", 18.0);
        double critTps = configFiles.getAlerts().getDouble("alerts.tps.critical", 15.0);
        double warnMspt = configFiles.getAlerts().getDouble("alerts.mspt.warning", 45.0);
        double critMspt = configFiles.getAlerts().getDouble("alerts.mspt.critical", 55.0);
        double warnRam = configFiles.getAlerts().getDouble("alerts.ram.warning-percent", 85.0);
        double critRam = configFiles.getAlerts().getDouble("alerts.ram.critical-percent", 95.0);
        int warnEntities = configFiles.getAlerts().getInt("alerts.entities-per-chunk.warning", 150);
        int critEntities = configFiles.getAlerts().getInt("alerts.entities-per-chunk.critical", 250);

        if (tps1 > 0 && isAlertEnabled("tps")) {
            AlertLevel level = levelForBelow(tps1, warnTps, critTps);
            String template = getTemplate("tps", "&eTPS low: %value% (warn < %warn%, crit < %crit%)");
            String message = applyTemplate(template,
                    String.format("%.2f", tps1),
                    String.format("%.2f", warnTps),
                    String.format("%.2f", critTps));
            sendIfNeeded("tps", level, message, getCooldownSeconds("tps"));
        }

        if (mspt > 0 && isAlertEnabled("mspt")) {
            AlertLevel level = levelForAbove(mspt, warnMspt, critMspt);
            String template = getTemplate("mspt", "&eTick time high: %value% ms (warn > %warn%, crit > %crit%)");
            String message = applyTemplate(template,
                    String.format("%.2f", mspt),
                    String.format("%.2f", warnMspt),
                    String.format("%.2f", critMspt));
            sendIfNeeded("mspt", level, message, getCooldownSeconds("mspt"));
        }

        if (ramPercent > 0 && isAlertEnabled("ram")) {
            AlertLevel level = levelForAbove(ramPercent, warnRam, critRam);
            String template = getTemplate("ram", "&eRAM usage high: %value%%% (warn > %warn%%, crit > %crit%%)");
            String message = applyTemplate(template,
                    String.format("%.1f", ramPercent),
                    String.format("%.1f", warnRam),
                    String.format("%.1f", critRam));
            sendIfNeeded("ram", level, message, getCooldownSeconds("ram"));
        }

        if (scan.getScannedChunks() > 0 && isAlertEnabled("entities-per-chunk")) {
            double avgEntities = scan.getAverageEntitiesPerChunk();
            AlertLevel level = levelForAbove(avgEntities, warnEntities, critEntities);
            String template = getTemplate("entities-per-chunk",
                    "&eEntities per chunk high: %value% (warn > %warn%, crit > %crit%)");
            String message = applyTemplate(template,
                    String.format("%.1f", avgEntities),
                    Integer.toString(warnEntities),
                    Integer.toString(critEntities));
            sendIfNeeded("entities-per-chunk", level, message, getCooldownSeconds("entities-per-chunk"));
        }
    }

    private void sendIfNeeded(String key, AlertLevel level, String message, long cooldownSeconds) {
        AlertState state = states.computeIfAbsent(key, ignored -> new AlertState());
        if (level == AlertLevel.OK) {
            state.lastLevel = level;
            return;
        }

        cooldownSeconds = Math.max(30, cooldownSeconds);
        long now = System.currentTimeMillis();
        boolean shouldSend = level != state.lastLevel || (now - state.lastSentAt) >= (cooldownSeconds * 1000L);

        if (!shouldSend) {
            return;
        }

        state.lastLevel = level;
        state.lastSentAt = now;

        String permission = configFiles.getAlerts().getString("alerts.notify-permission", "pulse.view");
        String coloredMessage = colorFor(level) + message;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                MessageUtil.send(player, plugin, coloredMessage);
            }
        }
        plugin.getLogger().warning(message);

        if (configFiles.getAlerts().getBoolean("alerts.discord.enabled", false)) {
            String webhook = configFiles.getAlerts().getString("alerts.discord.webhook-url", "");
            DiscordWebhook.send(plugin, webhook, "[" + level.name() + "] " + MessageUtil.stripColor(message));
        }
    }

    private AlertLevel levelForBelow(double value, double warn, double crit) {
        if (value <= crit) {
            return AlertLevel.CRITICAL;
        }
        if (value <= warn) {
            return AlertLevel.WARNING;
        }
        return AlertLevel.OK;
    }

    private AlertLevel levelForAbove(double value, double warn, double crit) {
        if (value >= crit) {
            return AlertLevel.CRITICAL;
        }
        if (value >= warn) {
            return AlertLevel.WARNING;
        }
        return AlertLevel.OK;
    }

    private enum AlertLevel {
        OK,
        WARNING,
        CRITICAL
    }

    private String colorFor(AlertLevel level) {
        return switch (level) {
            case WARNING -> "&e";
            case CRITICAL -> "&c";
            default -> "&7";
        };
    }

    private static final class AlertState {
        private AlertLevel lastLevel = AlertLevel.OK;
        private long lastSentAt = 0L;
    }

    private boolean isAlertEnabled(String key) {
        return configFiles.getAlerts().getBoolean("alerts." + key + ".enabled", true);
    }

    private long getCooldownSeconds(String key) {
        if (configFiles.getAlerts().contains("alerts." + key + ".cooldown-seconds")) {
            return configFiles.getAlerts().getLong("alerts." + key + ".cooldown-seconds");
        }
        return configFiles.getAlerts().getLong("alerts.cooldown-seconds", 120);
    }

    private String getTemplate(String key, String fallback) {
        String path = "alerts." + key + ".message";
        String template = configFiles.getAlerts().getString(path);
        if (template == null || template.isBlank()) {
            return fallback;
        }
        return template;
    }

    private String applyTemplate(String template, String value, String warn, String crit) {
        return template
                .replace("%value%", value)
                .replace("%warn%", warn)
                .replace("%crit%", crit);
    }
}
