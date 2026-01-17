package com.azk.pulse.commands;

import java.util.List;
import org.bukkit.command.CommandSender;

public interface PulseSubcommand {
    String name();

    String module();

    String permission();

    boolean execute(CommandSender sender, String[] args);

    List<String> tabComplete(CommandSender sender, String[] args);
}
