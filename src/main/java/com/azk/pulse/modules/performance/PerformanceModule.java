package com.azk.pulse.modules.performance;

import com.azk.pulse.PulsePlugin;
import com.azk.pulse.commands.CommandRegistry;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.PulseModule;
import com.azk.pulse.storage.StorageFactory;
import java.time.Instant;
import java.time.Duration;
import org.bukkit.scheduler.BukkitTask;

public class PerformanceModule implements PulseModule {
    private final PulsePlugin plugin;
    private final ConfigFiles configFiles;
    private final CommandRegistry registry;
    private final PerformanceMetrics metrics;
    private final PerformanceHistory history;
    private AlertService alertService;
    private PerformanceHistoryRepository historyRepository;
    private ClearLagService clearLagService;
    private BukkitTask historyTask;
    private boolean enabled;

    public PerformanceModule(PulsePlugin plugin, ConfigFiles configFiles, CommandRegistry registry) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.registry = registry;
        this.metrics = new PerformanceMetrics(plugin);
        this.history = new PerformanceHistory();
    }

    @Override
    public String getName() {
        return "performance";
    }

    @Override
    public void enable() {
        registry.register(new StatusCommand(plugin, metrics));
        registry.register(new LagCommand(plugin, metrics, configFiles));
        registry.register(new DiagnoseCommand(plugin, metrics, history, configFiles));
        registry.register(new HistoryCommand(plugin, history));
        clearLagService = new ClearLagService(plugin, configFiles);
        registry.register(new ClearLagCommand(plugin, configFiles, clearLagService));
        registry.register(new KillEntitiesCommand(plugin));
        registry.register(new UnloadChunksCommand(plugin, configFiles));
        registry.register(new HealthCommand(plugin, metrics, configFiles));
        registry.register(new AlertTestCommand(plugin, configFiles));

        startHistoryTask();
        startAlerts();
        clearLagService.start();
    }

    @Override
    public void disable() {
        stopHistoryTask();
        stopAlerts();
        if (clearLagService != null) {
            clearLagService.stop();
            clearLagService = null;
        }
        if (historyRepository != null) {
            historyRepository.shutdown();
            historyRepository = null;
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

    private void startHistoryTask() {
        stopHistoryTask();
        int interval = Math.max(10, configFiles.getMain().getInt("performance.history.sample-interval-seconds", 60));
        int retentionHours = Math.max(1, configFiles.getMain().getInt("performance.history.retention-hours", 720));
        history.setRetention(Duration.ofHours(retentionHours));
        history.clear();

        setupHistoryRepository();
        loadHistoryFromDatabase();

        historyTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            PerformanceHistory.HistorySample sample = metrics.captureSample();
            history.addSample(sample);
            if (historyRepository != null) {
                historyRepository.saveSampleAsync(sample);
            }
        }, 20L, interval * 20L);
    }

    private void stopHistoryTask() {
        if (historyTask != null) {
            historyTask.cancel();
            historyTask = null;
        }
    }

    private void startAlerts() {
        stopAlerts();
        alertService = new AlertService(plugin, configFiles, metrics);
        alertService.start();
    }

    private void stopAlerts() {
        if (alertService != null) {
            alertService.stop();
            alertService = null;
        }
    }

    private void setupHistoryRepository() {
        boolean persist = configFiles.getMain().getBoolean("performance.history.persist", true);
        if (!persist) {
            historyRepository = null;
            return;
        }
        historyRepository = new PerformanceHistoryRepository(plugin, StorageFactory.create(plugin, configFiles.getMain()));
        historyRepository.init();
    }

    private void loadHistoryFromDatabase() {
        if (historyRepository == null) {
            return;
        }
        Duration retention = history.getRetention();
        Instant cutoff = Instant.now().minus(retention);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            java.util.List<PerformanceHistory.HistorySample> samples = historyRepository.loadSince(cutoff);
            historyRepository.deleteOlderThan(cutoff);
            if (samples.isEmpty()) {
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (PerformanceHistory.HistorySample sample : samples) {
                    history.addSample(sample);
                }
            });
        });
    }

    public PerformanceHistory getHistory() {
        return history;
    }

    public PerformanceMetrics getMetrics() {
        return metrics;
    }
}
