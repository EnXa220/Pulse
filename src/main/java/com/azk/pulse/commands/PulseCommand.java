package com.azk.pulse.commands;

import com.azk.pulse.PulsePlugin;
import com.azk.pulse.core.MessageUtil;
import com.azk.pulse.core.ModuleManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class PulseCommand implements CommandExecutor, TabCompleter {
    private final PulsePlugin plugin;
    private final ModuleManager moduleManager;
    private final CommandRegistry registry;

    public PulseCommand(PulsePlugin plugin, ModuleManager moduleManager, CommandRegistry registry) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            if (!sender.hasPermission("pulse.admin")) {
                MessageUtil.sendErrorKey(sender, plugin, "general.no-permission");
                return true;
            }
            plugin.reloadAll();
            MessageUtil.sendTitleKey(sender, plugin, "commands.reload.title");
            MessageUtil.sendSuccessKey(sender, plugin, "commands.reload.success");
            return true;
        }

        PulseSubcommand subcommand = registry.get(sub);
        if (subcommand == null) {
            sendHelp(sender);
            return true;
        }

        if (!moduleManager.isModuleEnabled(subcommand.module())) {
            MessageUtil.sendWarningKey(sender, plugin, "general.module-disabled",
                    java.util.Map.of("module", subcommand.module()));
            return true;
        }

        String permission = subcommand.permission();
        if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
            MessageUtil.sendErrorKey(sender, plugin, "general.no-permission");
            return true;
        }

        String[] forwarded = new String[Math.max(0, args.length - 1)];
        if (forwarded.length > 0) {
            System.arraycopy(args, 1, forwarded, 0, forwarded.length);
        }

        return subcommand.execute(sender, forwarded);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return registry.getAll().stream()
                    .filter(sub -> moduleManager.isModuleEnabled(sub.module()))
                    .filter(sub -> sub.permission() == null || sub.permission().isBlank() || sender.hasPermission(sub.permission()))
                    .map(PulseSubcommand::name)
                    .sorted()
                    .filter(name -> name.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length > 1) {
            PulseSubcommand subcommand = registry.get(args[0].toLowerCase(Locale.ROOT));
            if (subcommand != null) {
                return subcommand.tabComplete(sender, slice(args));
            }
        }

        return new ArrayList<>();
    }

    private void sendHelp(CommandSender sender) {
        List<PulseSubcommand> subs = registry.getAll().stream()
                .filter(sub -> moduleManager.isModuleEnabled(sub.module()))
                .filter(sub -> sub.permission() == null || sub.permission().isBlank() || sender.hasPermission(sub.permission()))
                .sorted(Comparator.comparing(PulseSubcommand::name))
                .collect(Collectors.toList());

        MessageUtil.sendSectionKey(sender, plugin, "commands.help.title");
        for (PulseSubcommand sub : subs) {
            MessageUtil.sendKey(sender, plugin, "commands.help.item", java.util.Map.of("command", sub.name()));
        }

        if (sender.hasPermission("pulse.admin")) {
            MessageUtil.sendKey(sender, plugin, "commands.help.item", java.util.Map.of("command", "reload"));
        }
    }

    private String[] slice(String[] args) {
        String[] sliced = new String[Math.max(0, args.length - 1)];
        if (sliced.length > 0) {
            System.arraycopy(args, 1, sliced, 0, sliced.length);
        }
        return sliced;
    }
}
