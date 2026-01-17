package com.azk.pulse.modules.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class PluginLoadAnalyzer {
    public List<PluginLoad> getTopByScheduledTasks(int limit) {
        Map<Plugin, Integer> counts = new HashMap<>();
        for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
            counts.merge(task.getOwner(), 1, Integer::sum);
        }
        List<PluginLoad> result = new ArrayList<>();
        for (Map.Entry<Plugin, Integer> entry : counts.entrySet()) {
            result.add(new PluginLoad(entry.getKey().getName(), entry.getValue()));
        }
        result.sort(Comparator.comparingInt(PluginLoad::taskCount).reversed());
        if (result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    public record PluginLoad(String name, int taskCount) {
    }
}
