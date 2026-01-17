package com.azk.pulse.modules.logs;

import com.azk.pulse.PulsePlugin;
import com.azk.pulse.commands.CommandRegistry;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.PulseModule;
import com.azk.pulse.storage.StorageFactory;
import org.bukkit.event.HandlerList;

public class LogsModule implements PulseModule {
    private final PulsePlugin plugin;
    private final ConfigFiles configFiles;
    private final CommandRegistry registry;
    private LogRepository repository;
    private LogListener listener;
    private LookupWandListener wandListener;
    private LookupSelectionManager selectionManager;
    private LookupService lookupService;
    private boolean enabled;

    public LogsModule(PulsePlugin plugin, ConfigFiles configFiles, CommandRegistry registry) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "logs";
    }

    @Override
    public void enable() {
        repository = new LogRepository(plugin, StorageFactory.create(plugin, configFiles.getMain()));
        repository.init();
        listener = new LogListener(plugin, configFiles, repository);
        selectionManager = new LookupSelectionManager();
        lookupService = new LookupService(plugin, repository);
        wandListener = new LookupWandListener(plugin, configFiles, selectionManager, lookupService);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getServer().getPluginManager().registerEvents(wandListener, plugin);
        registry.register(new LookupCommand(plugin, configFiles, selectionManager, lookupService, wandListener));
    }

    @Override
    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (wandListener != null) {
            HandlerList.unregisterAll(wandListener);
            wandListener = null;
        }
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
        lookupService = null;
        selectionManager = null;
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
