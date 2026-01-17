package com.azk.pulse.modules.performance;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.DiscordWebhook;
import com.azk.pulse.core.MessageUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AlertTestCommand implements PulseSubcommand {
    private final JavaPlugin plugin;
    private final ConfigFiles configFiles;

    public AlertTestCommand(JavaPlugin plugin, ConfigFiles configFiles) {
        this.plugin = plugin;
        this.configFiles = configFiles;
    }

    @Override
    public String name() {
        return "alerttest";
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
        MessageUtil.sendTitleKey(sender, plugin, "alerts.test.title");
        String message = MessageUtil.tr(plugin, "alerts.test.message");
        String permission = configFiles.getAlerts().getString("alerts.notify-permission", "pulse.view");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                MessageUtil.send(player, plugin, message);
            }
        }
        plugin.getLogger().warning("[Pulse] Test alert triggered by " + sender.getName());

        if (configFiles.getAlerts().getBoolean("alerts.discord.enabled", false)) {
            String webhook = configFiles.getAlerts().getString("alerts.discord.webhook-url", "");
            DiscordWebhook.send(plugin, webhook, MessageUtil.stripColor(MessageUtil.tr(plugin, "alerts.test.discord")));
        }

        MessageUtil.sendSuccessKey(sender, plugin, "alerts.test.sent");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
