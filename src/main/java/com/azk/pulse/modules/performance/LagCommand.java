package com.azk.pulse.modules.performance;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

public class LagCommand implements PulseSubcommand {
    private final JavaPlugin plugin;
    private final PerformanceMetrics metrics;
    private final ConfigFiles configFiles;
    private final PluginLoadAnalyzer pluginLoadAnalyzer = new PluginLoadAnalyzer();

    public LagCommand(JavaPlugin plugin, PerformanceMetrics metrics, ConfigFiles configFiles) {
        this.plugin = plugin;
        this.metrics = metrics;
        this.configFiles = configFiles;
    }

    @Override
    public String name() {
        return "lag";
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
        MessageUtil.sendTitleKey(sender, plugin, "lag.title");
        List<WorldStat> worldStats = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            worldStats.add(new WorldStat(world.getName(), world.getEntities().size(), world.getLoadedChunks().length));
        }

        List<WorldStat> topWorlds = worldStats.stream()
                .sorted(Comparator.comparingInt(WorldStat::entities).reversed())
                .limit(5)
                .collect(Collectors.toList());

        int maxChunks = Math.max(0, configFiles.getMain().getInt("performance.lag.max-chunks-scan", 2000));
        List<PerformanceMetrics.ChunkEntityInfo> chunkInfos = metrics.collectChunkEntityInfo(maxChunks);
        boolean truncated = !chunkInfos.isEmpty() && chunkInfos.get(chunkInfos.size() - 1).isTruncatedMarker();
        if (truncated) {
            chunkInfos = chunkInfos.subList(0, chunkInfos.size() - 1);
        }

        List<PerformanceMetrics.ChunkEntityInfo> topChunks = chunkInfos.stream()
                .sorted(Comparator.comparingInt(PerformanceMetrics.ChunkEntityInfo::getEntities).reversed())
                .limit(5)
                .collect(Collectors.toList());

        Map<EntityType, Integer> entityCounts = new HashMap<>();
        Bukkit.getWorlds().forEach(world -> world.getEntities().forEach(entity -> {
            if (entity.getType() == EntityType.PLAYER) {
                return;
            }
            entityCounts.merge(entity.getType(), 1, Integer::sum);
        }));

        List<Map.Entry<EntityType, Integer>> topEntities = entityCounts.entrySet().stream()
                .sorted(Map.Entry.<EntityType, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        MessageUtil.sendSectionKey(sender, plugin, "lag.top-worlds");
        for (WorldStat stat : topWorlds) {
            MessageUtil.sendKey(sender, plugin, "lag.world-line",
                    Map.of("world", stat.name, "entities", Integer.toString(stat.entities), "chunks", Integer.toString(stat.chunks)));
        }

        MessageUtil.sendSectionKey(sender, plugin, "lag.top-chunks");
        for (PerformanceMetrics.ChunkEntityInfo info : topChunks) {
            MessageUtil.sendKey(sender, plugin, "lag.chunk-line",
                    Map.of("world", info.getWorld(),
                            "x", Integer.toString(info.getX()),
                            "z", Integer.toString(info.getZ()),
                            "entities", Integer.toString(info.getEntities())));
        }
        if (truncated) {
            MessageUtil.sendWarningKey(sender, plugin, "lag.chunk-scan-truncated",
                    Map.of("max", Integer.toString(maxChunks)));
        }

        MessageUtil.sendSectionKey(sender, plugin, "lag.top-entities");
        for (Map.Entry<EntityType, Integer> entry : topEntities) {
            MessageUtil.sendKey(sender, plugin, "lag.entity-line",
                    Map.of("type", entry.getKey().name(), "count", entry.getValue().toString()));
        }

        List<PluginLoadAnalyzer.PluginLoad> pluginLoads = pluginLoadAnalyzer.getTopByScheduledTasks(5);
        if (pluginLoads.isEmpty()) {
            MessageUtil.sendWarningKey(sender, plugin, "lag.no-scheduler-data");
        } else {
            MessageUtil.sendSectionKey(sender, plugin, "lag.top-plugins");
            for (PluginLoadAnalyzer.PluginLoad load : pluginLoads) {
                MessageUtil.sendKey(sender, plugin, "lag.plugin-line",
                        Map.of("plugin", load.name(), "count", Integer.toString(load.taskCount())));
            }
        }
        if (Bukkit.getPluginManager().getPlugin("spark") != null) {
            MessageUtil.sendWarningKey(sender, plugin, "lag.spark-detected");
        } else {
            MessageUtil.sendWarningKey(sender, plugin, "lag.profiling-hint");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    private record WorldStat(String name, int entities, int chunks) {
    }
}
