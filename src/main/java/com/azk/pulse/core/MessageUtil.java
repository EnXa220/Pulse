package com.azk.pulse.core;

import com.azk.pulse.PulsePlugin;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static void send(CommandSender sender, JavaPlugin plugin, String message) {
        String prefix = plugin.getConfig().getString("general.prefix", "");
        if (prefix == null || prefix.isBlank()) {
            sender.sendMessage(color(message));
            return;
        }
        sender.sendMessage(color(prefix + " " + message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    public static void sendTitle(CommandSender sender, JavaPlugin plugin, String title) {
        String line = "&8&l---- &b" + title + " &8&l----";
        String prefix = plugin.getConfig().getString("general.prefix", "");
        if (prefix != null && !prefix.isBlank()) {
            line = prefix + " " + line;
        }
        sender.sendMessage(color(line));
    }

    public static void sendSection(CommandSender sender, JavaPlugin plugin, String title) {
        send(sender, plugin, "&9" + title + ":");
    }

    public static void sendKeyValue(CommandSender sender, JavaPlugin plugin, String key, String value) {
        send(sender, plugin, "&7" + key + ": &f" + value);
    }

    public static void sendSuccess(CommandSender sender, JavaPlugin plugin, String message) {
        send(sender, plugin, "&a" + message);
    }

    public static void sendWarning(CommandSender sender, JavaPlugin plugin, String message) {
        send(sender, plugin, "&e" + message);
    }

    public static void sendError(CommandSender sender, JavaPlugin plugin, String message) {
        send(sender, plugin, "&c" + message);
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(color(message));
    }

    public static void sendKey(CommandSender sender, JavaPlugin plugin, String key) {
        send(sender, plugin, tr(plugin, key));
    }

    public static void sendKey(CommandSender sender, JavaPlugin plugin, String key, Map<String, String> placeholders) {
        send(sender, plugin, tr(plugin, key, placeholders));
    }

    public static void sendTitleKey(CommandSender sender, JavaPlugin plugin, String key) {
        sendTitle(sender, plugin, tr(plugin, key));
    }

    public static void sendTitleKey(CommandSender sender, JavaPlugin plugin, String key, Map<String, String> placeholders) {
        sendTitle(sender, plugin, tr(plugin, key, placeholders));
    }

    public static void sendSectionKey(CommandSender sender, JavaPlugin plugin, String key) {
        sendSection(sender, plugin, tr(plugin, key));
    }

    public static void sendSectionKey(CommandSender sender, JavaPlugin plugin, String key, Map<String, String> placeholders) {
        sendSection(sender, plugin, tr(plugin, key, placeholders));
    }

    public static void sendKeyValueKey(CommandSender sender, JavaPlugin plugin, String key, String value) {
        sendKeyValue(sender, plugin, tr(plugin, key), value);
    }

    public static void sendSuccessKey(CommandSender sender, JavaPlugin plugin, String key) {
        sendSuccess(sender, plugin, tr(plugin, key));
    }

    public static void sendSuccessKey(CommandSender sender, JavaPlugin plugin, String key, Map<String, String> placeholders) {
        sendSuccess(sender, plugin, tr(plugin, key, placeholders));
    }

    public static void sendWarningKey(CommandSender sender, JavaPlugin plugin, String key) {
        sendWarning(sender, plugin, tr(plugin, key));
    }

    public static void sendWarningKey(CommandSender sender, JavaPlugin plugin, String key, Map<String, String> placeholders) {
        sendWarning(sender, plugin, tr(plugin, key, placeholders));
    }

    public static void sendErrorKey(CommandSender sender, JavaPlugin plugin, String key) {
        sendError(sender, plugin, tr(plugin, key));
    }

    public static void sendErrorKey(CommandSender sender, JavaPlugin plugin, String key, Map<String, String> placeholders) {
        sendError(sender, plugin, tr(plugin, key, placeholders));
    }

    public static String tr(JavaPlugin plugin, String key) {
        return tr(plugin, key, key, null);
    }

    public static String tr(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        return tr(plugin, key, key, placeholders);
    }

    public static String tr(JavaPlugin plugin, String key, String fallback) {
        return tr(plugin, key, fallback, null);
    }

    public static String tr(JavaPlugin plugin, String key, String fallback, Map<String, String> placeholders) {
        Lang lang = null;
        if (plugin instanceof PulsePlugin pulsePlugin) {
            lang = pulsePlugin.getLang();
        }
        String value = lang != null ? lang.get(key, fallback) : fallback;
        if (placeholders == null || placeholders.isEmpty()) {
            return value;
        }
        return applyPlaceholders(value, placeholders);
    }

    private static String applyPlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String token = "%" + entry.getKey() + "%";
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace(token, value);
        }
        return result;
    }
}
