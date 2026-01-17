package com.azk.pulse.modules.performance;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ClearLagCommand implements PulseSubcommand {
    private final JavaPlugin plugin;
    private final ConfigFiles configFiles;
    private final ClearLagService clearLagService;
    private final Map<String, Long> lastPreview = new ConcurrentHashMap<>();

    public ClearLagCommand(JavaPlugin plugin, ConfigFiles configFiles, ClearLagService clearLagService) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.clearLagService = clearLagService;
    }

    @Override
    public String name() {
        return "clearlag";
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
        MessageUtil.sendTitleKey(sender, plugin, "clearlag.title");
        if (args.length == 0) {
            MessageUtil.sendWarningKey(sender, plugin, "clearlag.usage");
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        switch (mode) {
            case "preview" -> handlePreview(sender);
            case "confirm" -> handleConfirm(sender);
            default -> MessageUtil.sendWarningKey(sender, plugin, "clearlag.usage");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("preview", "confirm");
        }
        return List.of();
    }

    private void handlePreview(CommandSender sender) {
        ClearLagService.ClearLagResult result = clearLagService.preview();
        lastPreview.put(getKey(sender), System.currentTimeMillis());
        sendResult(sender, result, MessageUtil.tr(plugin, "clearlag.preview.label"));
        MessageUtil.sendKey(sender, plugin, "clearlag.confirm-hint");
    }

    private void handleConfirm(CommandSender sender) {
        long timeout = Math.max(5, configFiles.getMain().getLong("performance.clearlag.confirm-timeout-seconds", 30));
        Long last = lastPreview.get(getKey(sender));
        if (last == null || (System.currentTimeMillis() - last) > (timeout * 1000L)) {
            MessageUtil.sendWarningKey(sender, plugin, "clearlag.preview-expired");
            return;
        }
        ClearLagService.ClearLagResult result = clearLagService.clearNow();
        sendResult(sender, result, MessageUtil.tr(plugin, "clearlag.removed.label"));
    }

    private void sendResult(CommandSender sender, ClearLagService.ClearLagResult result, String label) {
        MessageUtil.sendSectionKey(sender, plugin, "clearlag.section", java.util.Map.of("label", label));
        MessageUtil.sendKeyValueKey(sender, plugin, "clearlag.entities-affected", Integer.toString(result.total()));
        if (result.total() == 0) {
            return;
        }
        List<Map.Entry<EntityType, Integer>> entries = new ArrayList<>(result.counts().entrySet());
        entries.sort(Map.Entry.<EntityType, Integer>comparingByValue().reversed());
        for (Map.Entry<EntityType, Integer> entry : entries) {
            MessageUtil.sendKey(sender, plugin, "clearlag.entity-line",
                    java.util.Map.of("type", entry.getKey().name(), "count", entry.getValue().toString()));
        }
    }

    private String getKey(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId().toString();
        }
        return "CONSOLE";
    }
}
