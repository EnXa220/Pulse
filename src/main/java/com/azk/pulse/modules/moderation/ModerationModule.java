package com.azk.pulse.modules.moderation;

import com.azk.pulse.PulsePlugin;
import com.azk.pulse.commands.CommandRegistry;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.PulseModule;

public class ModerationModule implements PulseModule {
    private final PulsePlugin plugin;
    private final ConfigFiles configFiles;
    private final CommandRegistry registry;
    private final ReportService reportService;
    private boolean enabled;

    public ModerationModule(PulsePlugin plugin, ConfigFiles configFiles, CommandRegistry registry) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.registry = registry;
        this.reportService = new ReportService(plugin);
    }

    @Override
    public String getName() {
        return "moderation";
    }

    @Override
    public void enable() {
        reportService.load();
        registry.register(new ReportsCommand(plugin, reportService));
    }

    @Override
    public void disable() {
        reportService.save();
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

    public ReportService getReportService() {
        return reportService;
    }
}
