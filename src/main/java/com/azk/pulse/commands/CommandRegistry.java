package com.azk.pulse.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    private final Map<String, PulseSubcommand> subcommands = new HashMap<>();

    public void register(PulseSubcommand subcommand) {
        subcommands.put(subcommand.name().toLowerCase(), subcommand);
    }

    public PulseSubcommand get(String name) {
        return subcommands.get(name.toLowerCase());
    }

    public Collection<PulseSubcommand> getAll() {
        return subcommands.values();
    }

    public void unregisterByModule(String moduleName) {
        String lower = moduleName.toLowerCase();
        subcommands.entrySet().removeIf(entry -> entry.getValue().module().equalsIgnoreCase(lower));
    }
}
