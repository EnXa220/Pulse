package com.azk.pulse.modules.performance;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class KillEntitiesCommand implements PulseSubcommand {
    private final JavaPlugin plugin;

    public KillEntitiesCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "killentities";
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
        MessageUtil.sendTitleKey(sender, plugin, "killentities.title");
        if (!(sender instanceof Player player)) {
            MessageUtil.sendErrorKey(sender, plugin, "general.only-players");
            return true;
        }

        double radius = parseDouble(args, "radius", 20.0);
        String typeName = parseString(args, "type", "");
        EntityType type = null;
        if (!typeName.isBlank()) {
            type = EntityType.fromName(typeName.toLowerCase(Locale.ROOT));
            if (type == null) {
                MessageUtil.sendErrorKey(sender, plugin, "killentities.unknown-type",
                        java.util.Map.of("type", typeName));
                return true;
            }
        }

        int removed = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player) {
                continue;
            }
            if (type != null && entity.getType() != type) {
                continue;
            }
            entity.remove();
            removed++;
        }

        MessageUtil.sendSuccessKey(sender, plugin, "killentities.removed",
                java.util.Map.of("count", Integer.toString(removed)));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> results = new ArrayList<>();
        if (args.length == 1) {
            results.add("radius=");
            results.add("type=");
        }
        if (args.length >= 1 && args[args.length - 1].startsWith("type=")) {
            String prefix = args[args.length - 1].substring("type=".length()).toLowerCase(Locale.ROOT);
            for (EntityType type : EntityType.values()) {
                String name = type.name().toLowerCase(Locale.ROOT);
                if (name.startsWith(prefix)) {
                    results.add("type=" + name);
                }
            }
        }
        return results;
    }

    private double parseDouble(String[] args, String key, double fallback) {
        String raw = parseString(args, key, null);
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String parseString(String[] args, String key, String fallback) {
        for (String arg : args) {
            if (arg.toLowerCase(Locale.ROOT).startsWith(key + "=")) {
                return arg.substring(key.length() + 1);
            }
        }
        return fallback;
    }
}
