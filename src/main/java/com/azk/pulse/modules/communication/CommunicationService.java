package com.azk.pulse.modules.communication;

import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import com.azk.pulse.modules.performance.PerformanceMetrics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

public class CommunicationService {
    private final JavaPlugin plugin;
    private final ConfigFiles configFiles;
    private final PerformanceMetrics metrics;
    private final Set<Integer> triggeredThresholds = new HashSet<>();
    private BukkitTask messageTask;
    private BukkitTask tpsTask;
    private int messageIndex = 0;
    private long lastTpsAlert = 0L;

    public CommunicationService(JavaPlugin plugin, ConfigFiles configFiles) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.metrics = new PerformanceMetrics(plugin);
    }

    public void start() {
        if (!isEnabled()) {
            return;
        }
        startMessageTask();
        startTpsTask();
        sendRestartMessage();
        handlePlayerCountChange();
    }

    public void stop() {
        if (messageTask != null) {
            messageTask.cancel();
            messageTask = null;
        }
        if (tpsTask != null) {
            tpsTask.cancel();
            tpsTask = null;
        }
        triggeredThresholds.clear();
    }

    public void handlePlayerCountChange() {
        if (!isEnabled()) {
            return;
        }
        int players = Bukkit.getOnlinePlayers().size();
        List<PlayerThreshold> thresholds = getThresholds();
        for (PlayerThreshold threshold : thresholds) {
            if (players >= threshold.min()) {
                if (!triggeredThresholds.contains(threshold.min())) {
                    broadcast(threshold.message());
                    triggeredThresholds.add(threshold.min());
                }
            } else {
                triggeredThresholds.remove(threshold.min());
            }
        }
    }

    private void startMessageTask() {
        List<String> messages = configFiles.getMain().getStringList("communication.messages");
        if (messages.isEmpty()) {
            return;
        }
        int intervalSeconds = Math.max(30, configFiles.getMain().getInt("communication.interval-seconds", 300));
        messageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isEnabled()) {
                return;
            }
            String message = messages.get(messageIndex % messages.size());
            messageIndex++;
            broadcast(message);
        }, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    private void startTpsTask() {
        boolean enabled = configFiles.getMain().getBoolean("communication.tps-alert.enabled", false);
        if (!enabled) {
            return;
        }
        int intervalSeconds = Math.max(10, configFiles.getMain().getInt("communication.tps-alert.check-interval-seconds", 30));
        double threshold = configFiles.getMain().getDouble("communication.tps-alert.threshold", 18.0);
        long cooldownSeconds = Math.max(30, configFiles.getMain().getLong("communication.tps-alert.cooldown-seconds", 300));
        String message = configFiles.getMain().getString("communication.tps-alert.message", "Low TPS: %tps%");

        tpsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isEnabled()) {
                return;
            }
            double tps = metrics.getTps()[0];
            if (tps <= 0) {
                return;
            }
            if (tps <= threshold) {
                long now = System.currentTimeMillis();
                if (now - lastTpsAlert >= cooldownSeconds * 1000L) {
                    lastTpsAlert = now;
                    broadcast(replaceTokens(message, tps));
                }
            }
        }, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    private void sendRestartMessage() {
        String message = configFiles.getMain().getString("communication.restart-message", "");
        if (message == null || message.isBlank()) {
            return;
        }
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            broadcast(message);
        }
    }

    private void broadcast(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Bukkit.broadcastMessage(MessageUtil.color(message));
    }

    private String replaceTokens(String message, double tps) {
        return message.replace("%tps%", String.format(Locale.US, "%.2f", tps));
    }

    private boolean isEnabled() {
        return configFiles.getMain().getBoolean("communication.enabled", true);
    }

    private List<PlayerThreshold> getThresholds() {
        List<PlayerThreshold> thresholds = new ArrayList<>();
        List<Map<?, ?>> entries = configFiles.getMain().getMapList("communication.player-thresholds");
        for (Map<?, ?> entry : entries) {
            Object minValue = entry.get("min");
            Object messageValue = entry.get("message");
            int min = parseInt(minValue, -1);
            String message = messageValue != null ? String.valueOf(messageValue) : "";
            if (min > 0 && !message.isBlank()) {
                thresholds.add(new PlayerThreshold(min, message));
            }
        }
        return thresholds;
    }

    private int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private record PlayerThreshold(int min, String message) {
    }
}
