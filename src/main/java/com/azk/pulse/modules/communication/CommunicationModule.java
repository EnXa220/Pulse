package com.azk.pulse.modules.communication;

import com.azk.pulse.PulsePlugin;
import com.azk.pulse.commands.CommandRegistry;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.PulseModule;
import org.bukkit.event.HandlerList;

public class CommunicationModule implements PulseModule {
    private final PulsePlugin plugin;
    private final ConfigFiles configFiles;
    private final CommandRegistry registry;
    private CommunicationService service;
    private CommunicationListener listener;
    private boolean enabled;

    public CommunicationModule(PulsePlugin plugin, ConfigFiles configFiles, CommandRegistry registry) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "communication";
    }

    @Override
    public void enable() {
        service = new CommunicationService(plugin, configFiles);
        service.start();
        listener = new CommunicationListener(plugin, service);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (service != null) {
            service.stop();
            service = null;
        }
        registry.unregisterByModule(getName());
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
