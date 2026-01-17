package com.azk.pulse.modules.logs;

import com.azk.pulse.core.MessageUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LookupService {
    private final JavaPlugin plugin;
    private final LogRepository repository;

    public LookupService(JavaPlugin plugin, LogRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void sendLookup(CommandSender sender, LogRepository.LookupQuery query, String title) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LogRepository.LogEntry> entries = repository.fetchRecent(query);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                MessageUtil.sendTitle(sender, plugin, title);
                if (entries.isEmpty()) {
                    MessageUtil.sendWarningKey(sender, plugin, "lookup.none");
                    return;
                }
                if (sender instanceof Player player) {
                    for (LogRepository.LogEntry entry : entries) {
                        sendInteractive(player, entry);
                    }
                    return;
                }
                for (LogRepository.LogEntry entry : entries) {
                    MessageUtil.sendRaw(sender, colorize(entry));
                }
            });
        });
    }

    public void exportLookup(CommandSender sender, LogRepository.LookupQuery query, String title) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<LogRepository.LogEntry> entries = repository.fetchRecent(query);
            if (entries.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        MessageUtil.sendWarningKey(sender, plugin, "lookup.export.none"));
                return;
            }

            String filename = "lookup-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .format(LocalDateTime.now()) + ".csv";
            File exportDir = new File(plugin.getDataFolder(), "exports");
            File file = new File(exportDir, filename);
            try {
                Files.createDirectories(exportDir.toPath());
                writeCsv(file, entries);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtil.sendTitle(sender, plugin, title);
                    MessageUtil.sendSuccessKey(sender, plugin, "lookup.export.success",
                            java.util.Map.of("path", file.getAbsolutePath()));
                });
            } catch (IOException ex) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        MessageUtil.sendErrorKey(sender, plugin, "lookup.export.failed",
                                java.util.Map.of("error", ex.getMessage())));
            }
        });
    }

    private String colorize(LogRepository.LogEntry entry) {
        String raw = entry.message();
        int split = raw.indexOf("] ");
        String time = raw;
        String rest = "";
        if (split > 0) {
            time = raw.substring(0, split + 1);
            rest = raw.substring(split + 2);
        }
        String type = entry.type();
        String withoutType = rest;
        if (rest.startsWith(type + " ")) {
            withoutType = rest.substring(type.length() + 1);
        }
        return "&7" + time + " &b" + type + " &f" + withoutType;
    }

    private void sendInteractive(Player player, LogRepository.LogEntry entry) {
        String baseText = MessageUtil.color(colorize(entry));
        BaseComponent[] base = TextComponent.fromLegacyText(baseText);
        TextComponent message = new TextComponent();
        message.setExtra(List.of());
        for (BaseComponent component : base) {
            message.addExtra(component);
        }

        if (entry.world() != null && entry.x() != null && entry.y() != null && entry.z() != null) {
            String tpCommand = "/pulse lookup tp " + entry.world() + " " + entry.x() + " " + entry.y() + " " + entry.z();
            TextComponent tp = new TextComponent(MessageUtil.color(" &b[" + MessageUtil.tr(plugin, "lookup.action.tp") + "]"));
            tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand));
            tp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(MessageUtil.tr(plugin, "lookup.action.tp-hover")).create()));

            String scanCommand = "/pulse lookup * world=" + entry.world()
                    + " x=" + entry.x() + " y=" + entry.y() + " z=" + entry.z() + " radius=3";
            TextComponent scan = new TextComponent(MessageUtil.color(" &7[" + MessageUtil.tr(plugin, "lookup.action.scan") + "]"));
            scan.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, scanCommand));
            scan.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(MessageUtil.tr(plugin, "lookup.action.scan-hover")).create()));

            message.addExtra(tp);
            message.addExtra(scan);
        }

        player.spigot().sendMessage(message);
    }

    private void writeCsv(File file, List<LogRepository.LogEntry> entries) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("timestamp,type,message,world,x,y,z").append(System.lineSeparator());
        for (LogRepository.LogEntry entry : entries) {
            builder.append(csv(entry.timestamp()))
                    .append(',').append(csv(entry.type()))
                    .append(',').append(csv(entry.message()))
                    .append(',').append(csv(entry.world()))
                    .append(',').append(csv(entry.x()))
                    .append(',').append(csv(entry.y()))
                    .append(',').append(csv(entry.z()))
                    .append(System.lineSeparator());
        }
        Files.writeString(file.toPath(), builder.toString(), StandardCharsets.UTF_8);
    }

    private String csv(Object value) {
        String text = value == null ? "" : value.toString();
        text = text.replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
