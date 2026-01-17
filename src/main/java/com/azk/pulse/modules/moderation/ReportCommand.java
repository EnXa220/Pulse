package com.azk.pulse.modules.moderation;

import com.azk.pulse.core.MessageUtil;
import com.azk.pulse.core.ModuleManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ReportCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;
    private final ModerationModule moderationModule;

    public ReportCommand(JavaPlugin plugin, ModuleManager moduleManager, ModerationModule moderationModule) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;
        this.moderationModule = moderationModule;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!moduleManager.isModuleEnabled("moderation")) {
            MessageUtil.sendWarningKey(sender, plugin, "moderation.disabled");
            return true;
        }

        MessageUtil.sendTitleKey(sender, plugin, "moderation.report.title");
        if (sender instanceof Player player && !player.hasPermission("pulse.report")) {
            MessageUtil.sendErrorKey(sender, plugin, "moderation.report.no-permission");
            return true;
        }

        if (args.length < 2) {
            MessageUtil.sendWarningKey(sender, plugin, "moderation.report.usage");
            return true;
        }

        String target = args[0];
        String reason = join(args, 1);
        String reporter = sender.getName();
        ReportService.ReportEntry entry = new ReportService.ReportEntry(
                UUID.randomUUID().toString(), reporter, target, reason, System.currentTimeMillis());
        moderationModule.getReportService().addReport(entry);

        MessageUtil.sendSuccessKey(sender, plugin, "moderation.report.submitted");

        String staffMessage = MessageUtil.tr(plugin, "moderation.report.staff-message",
                java.util.Map.of("reporter", reporter, "target", target, "reason", reason));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("pulse.mod")) {
                MessageUtil.send(player, plugin, "&c" + staffMessage);
            }
        }
        plugin.getLogger().info(staffMessage);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> results = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    results.add(name);
                }
            }
        }
        return results;
    }

    private String join(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
