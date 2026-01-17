package com.azk.pulse.modules.performance;

import com.azk.pulse.PulsePlugin;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitTask;

public class ClearLagService {
    private final PulsePlugin plugin;
    private final ConfigFiles configFiles;
    private BukkitTask scheduleTask;
    private boolean schedulePending;

    public ClearLagService(PulsePlugin plugin, ConfigFiles configFiles) {
        this.plugin = plugin;
        this.configFiles = configFiles;
    }

    public void start() {
        stop();
        boolean enabled = configFiles.getMain().getBoolean("performance.clearlag.schedule.enabled", false);
        if (!enabled) {
            return;
        }
        int interval = Math.max(60, configFiles.getMain().getInt("performance.clearlag.schedule.interval-seconds", 900));
        scheduleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::runScheduledClear,
                interval * 20L, interval * 20L);
    }

    public void stop() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
            scheduleTask = null;
        }
        schedulePending = false;
    }

    public ClearLagResult preview() {
        return scan(false);
    }

    public ClearLagResult clearNow() {
        return scan(true);
    }

    private void runScheduledClear() {
        if (schedulePending) {
            return;
        }
        schedulePending = true;
        int warnSeconds = Math.max(0, configFiles.getMain().getInt("performance.clearlag.schedule.warn-seconds", 30));
        boolean broadcast = configFiles.getMain().getBoolean("performance.clearlag.schedule.broadcast", true);
        if (broadcast && warnSeconds > 0) {
            String template = configFiles.getMain().getString("performance.clearlag.schedule.warn-message",
                    "&eClearLag running in %seconds%s.");
            String message = template.replace("%seconds%", Integer.toString(warnSeconds));
            broadcast(message);
        }
        if (warnSeconds <= 0) {
            executeScheduledClear(broadcast);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> executeScheduledClear(broadcast), warnSeconds * 20L);
    }

    private void executeScheduledClear(boolean broadcast) {
        ClearLagResult result = clearNow();
        if (broadcast) {
            String template = configFiles.getMain().getString("performance.clearlag.schedule.done-message",
                    "&aClearLag removed %count% entities.");
            String message = template.replace("%count%", Integer.toString(result.total()));
            broadcast(message);
        }
        schedulePending = false;
    }

    private void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            MessageUtil.send(player, plugin, message);
        }
        plugin.getLogger().info(MessageUtil.stripColor(message));
    }

    private ClearLagResult scan(boolean remove) {
        Set<EntityType> removeTypes = parseEntityTypes(
                configFiles.getMain().getStringList("performance.clearlag.remove-types"));
        Set<EntityType> whitelistTypes = parseEntityTypes(
                configFiles.getMain().getStringList("performance.clearlag.whitelist-types"));
        Set<String> whitelistWorlds = new HashSet<>();
        for (String worldName : configFiles.getMain().getStringList("performance.clearlag.whitelist-worlds")) {
            if (worldName != null && !worldName.isBlank()) {
                whitelistWorlds.add(worldName.toLowerCase(Locale.ROOT));
            }
        }

        boolean excludeNamed = configFiles.getMain().getBoolean("performance.clearlag.exclude-named", true);
        boolean excludeTamed = configFiles.getMain().getBoolean("performance.clearlag.exclude-tamed", true);

        Map<EntityType, Integer> counts = new EnumMap<>(EntityType.class);
        int total = 0;
        for (World world : Bukkit.getWorlds()) {
            if (whitelistWorlds.contains(world.getName().toLowerCase(Locale.ROOT))) {
                continue;
            }
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) {
                    continue;
                }
                EntityType type = entity.getType();
                if (!removeTypes.isEmpty() && !removeTypes.contains(type)) {
                    continue;
                }
                if (whitelistTypes.contains(type)) {
                    continue;
                }
                if (excludeNamed && entity.getCustomName() != null) {
                    continue;
                }
                if (excludeTamed && entity instanceof Tameable tameable && tameable.isTamed()) {
                    continue;
                }
                counts.merge(type, 1, Integer::sum);
                total++;
                if (remove) {
                    entity.remove();
                }
            }
        }

        return new ClearLagResult(total, counts);
    }

    private Set<EntityType> parseEntityTypes(List<String> rawTypes) {
        Set<EntityType> result = new HashSet<>();
        if (rawTypes == null) {
            return result;
        }
        for (String name : rawTypes) {
            if (name == null || name.isBlank()) {
                continue;
            }
            EntityType type = EntityType.fromName(name.toLowerCase(Locale.ROOT));
            if (type == null) {
                try {
                    type = EntityType.valueOf(name.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    type = null;
                }
            }
            if (type != null) {
                result.add(type);
            }
        }
        return result;
    }

    public record ClearLagResult(int total, Map<EntityType, Integer> counts) {
    }
}
