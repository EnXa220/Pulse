package com.azk.pulse.gui;

import com.azk.pulse.PulsePlugin;
import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.MessageUtil;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuiCommand implements PulseSubcommand {
    private final PulsePlugin plugin;
    private final GuiManager guiManager;

    public GuiCommand(PulsePlugin plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public String name() {
        return "gui";
    }

    @Override
    public String module() {
        return "core";
    }

    @Override
    public String permission() {
        return "pulse.gui";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendErrorKey(sender, plugin, "general.only-players");
            return true;
        }
        guiManager.openMain(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
