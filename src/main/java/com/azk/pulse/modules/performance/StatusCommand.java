package com.azk.pulse.modules.performance;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.MessageUtil;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class StatusCommand implements PulseSubcommand {
    private final JavaPlugin plugin;
    private final PerformanceMetrics metrics;

    public StatusCommand(JavaPlugin plugin, PerformanceMetrics metrics) {
        this.plugin = plugin;
        this.metrics = metrics;
    }

    @Override
    public String name() {
        return "status";
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
        MessageUtil.sendTitleKey(sender, plugin, "status.title");
        double[] tps = metrics.getTps();
        double mspt = metrics.getAverageTickTime();
        double cpu = metrics.getProcessCpuLoad();
        long used = metrics.getUsedMemory();
        long max = metrics.getMaxMemory();
        int players = metrics.getOnlinePlayers();
        int chunks = metrics.getLoadedChunks();
        int entities = metrics.getTotalEntities();

        String notAvailable = MessageUtil.tr(plugin, "general.not-available");
        String tpsLine = tps[0] < 0
                ? notAvailable
                : String.format(Locale.US, "%.2f / %.2f / %.2f", tps[0], tps[1], tps[2]);
        String msptLine = mspt < 0 ? notAvailable : String.format(Locale.US, "%.2f ms", mspt);
        String cpuLine = cpu < 0 ? notAvailable : String.format(Locale.US, "%.1f%%", cpu);

        MessageUtil.sendKeyValueKey(sender, plugin, "status.tps", tpsLine);
        MessageUtil.sendKeyValueKey(sender, plugin, "status.ram", formatMb(used) + " / " + formatMb(max));
        MessageUtil.sendKeyValueKey(sender, plugin, "status.cpu", cpuLine);
        MessageUtil.sendKeyValueKey(sender, plugin, "status.players", Integer.toString(players));
        MessageUtil.sendKeyValueKey(sender, plugin, "status.loaded-chunks", Integer.toString(chunks));
        MessageUtil.sendKeyValueKey(sender, plugin, "status.entities", Integer.toString(entities));
        MessageUtil.sendKeyValueKey(sender, plugin, "status.mspt", msptLine);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    private String formatMb(long bytes) {
        return String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
