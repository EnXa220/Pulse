package com.azk.pulse;

import com.azk.pulse.commands.CommandRegistry;
import com.azk.pulse.commands.PulseCommand;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.Lang;
import com.azk.pulse.core.ModuleManager;
import com.azk.pulse.gui.GuiCommand;
import com.azk.pulse.gui.GuiListener;
import com.azk.pulse.gui.GuiManager;
import com.azk.pulse.modules.communication.CommunicationModule;
import com.azk.pulse.modules.logs.LogsModule;
import com.azk.pulse.modules.moderation.ModerationModule;
import com.azk.pulse.modules.moderation.ReportCommand;
import com.azk.pulse.modules.performance.PerformanceModule;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PulsePlugin extends JavaPlugin {
    private ConfigFiles configFiles;
    private ModuleManager moduleManager;
    private CommandRegistry commandRegistry;
    private Lang lang;

    private PerformanceModule performanceModule;
    private LogsModule logsModule;
    private CommunicationModule communicationModule;
    private ModerationModule moderationModule;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        configFiles = new ConfigFiles(this);
        configFiles.loadAll();
        lang = new Lang(this);
        lang.load();

        commandRegistry = new CommandRegistry();
        moduleManager = new ModuleManager(this, configFiles, commandRegistry);

        performanceModule = new PerformanceModule(this, configFiles, commandRegistry);
        logsModule = new LogsModule(this, configFiles, commandRegistry);
        communicationModule = new CommunicationModule(this, configFiles, commandRegistry);
        moderationModule = new ModerationModule(this, configFiles, commandRegistry);

        moduleManager.register(performanceModule);
        moduleManager.register(logsModule);
        moduleManager.register(communicationModule);
        moduleManager.register(moderationModule);
        moduleManager.loadModules();

        guiManager = new GuiManager(this, moduleManager, configFiles, performanceModule, moderationModule);
        getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);
        commandRegistry.register(new GuiCommand(this, guiManager));

        PluginCommand pulseCommand = getCommand("pulse");
        if (pulseCommand != null) {
            PulseCommand executor = new PulseCommand(this, moduleManager, commandRegistry);
            pulseCommand.setExecutor(executor);
            pulseCommand.setTabCompleter(executor);
        }

        PluginCommand reportCommand = getCommand("report");
        if (reportCommand != null) {
            ReportCommand executor = new ReportCommand(this, moduleManager, moderationModule);
            reportCommand.setExecutor(executor);
            reportCommand.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.shutdown();
        }
    }

    public void reloadAll() {
        configFiles.reloadAll();
        if (lang != null) {
            lang.load();
        }
        moduleManager.reloadModules();
    }

    public ConfigFiles getConfigFiles() {
        return configFiles;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public Lang getLang() {
        return lang;
    }
}
