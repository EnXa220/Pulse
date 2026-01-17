package com.azk.pulse.gui;

import com.azk.pulse.PulsePlugin;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import com.azk.pulse.core.ModuleManager;
import com.azk.pulse.modules.moderation.ModerationModule;
import com.azk.pulse.modules.moderation.ReportService;
import com.azk.pulse.modules.performance.HealthEvaluator;
import com.azk.pulse.modules.performance.PerformanceHistory;
import com.azk.pulse.modules.performance.PerformanceMetrics;
import com.azk.pulse.modules.performance.PerformanceModule;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class GuiManager {
    private final PulsePlugin plugin;
    private final ModuleManager moduleManager;
    private final ConfigFiles configFiles;
    private final PerformanceModule performanceModule;
    private final ModerationModule moderationModule;
    private final NamespacedKey actionKey;
    private final NamespacedKey moduleKey;
    private final PerformanceMetrics metrics;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public GuiManager(PulsePlugin plugin, ModuleManager moduleManager, ConfigFiles configFiles,
                      PerformanceModule performanceModule, ModerationModule moderationModule) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;
        this.configFiles = configFiles;
        this.performanceModule = performanceModule;
        this.moderationModule = moderationModule;
        this.actionKey = new NamespacedKey(plugin, "pulse_gui_action");
        this.moduleKey = new NamespacedKey(plugin, "pulse_gui_module");
        this.metrics = new PerformanceMetrics(plugin);
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(new PulseGuiHolder(PulseGuiType.MAIN), 27, tr("gui.main.title"));
        inventory.setItem(10, actionItem(Material.BEACON, tr("gui.main.diagnostics.name"),
                langList("gui.main.diagnostics.lore"), "diagnostics"));
        inventory.setItem(12, actionItem(Material.BELL, tr("gui.main.alerts.name"),
                langList("gui.main.alerts.lore"), "alerts"));
        inventory.setItem(14, actionItem(Material.BOOK, tr("gui.main.reports.name"),
                langList("gui.main.reports.lore"), "reports"));
        inventory.setItem(16, actionItem(Material.COMPARATOR, tr("gui.main.modules.name"),
                langList("gui.main.modules.lore"), "modules"));
        inventory.setItem(22, actionItem(Material.BARRIER, tr("gui.main.close.name"),
                langList("gui.main.close.lore"), "close"));
        player.openInventory(inventory);
    }

    public void openDiagnostics(Player player) {
        Inventory inventory = Bukkit.createInventory(new PulseGuiHolder(PulseGuiType.DIAGNOSTICS), 27, tr("gui.diagnostics.title"));

        if (!moduleManager.isModuleEnabled("performance")) {
            inventory.setItem(13, infoItem(Material.REDSTONE_BLOCK, tr("gui.diagnostics.disabled.name"),
                    langList("gui.diagnostics.disabled.lore")));
            inventory.setItem(22, actionItem(Material.ARROW, tr("gui.common.back.name"),
                    langList("gui.common.back.lore"), "back"));
            player.openInventory(inventory);
            return;
        }

        double[] tps = metrics.getTps();
        double mspt = metrics.getAverageTickTime();
        long used = metrics.getUsedMemory();
        long max = metrics.getMaxMemory();
        int players = metrics.getOnlinePlayers();
        int chunks = metrics.getLoadedChunks();
        int entities = metrics.getTotalEntities();

        String notAvailable = tr("general.not-available");
        String tpsLine = tps[0] < 0 ? notAvailable : String.format(Locale.US, "1m %.2f / 5m %.2f / 15m %.2f", tps[0], tps[1], tps[2]);
        inventory.setItem(10, infoItem(Material.CLOCK, tr("gui.diagnostics.tps.name"),
                langList("gui.diagnostics.tps.lore", java.util.Map.of("value", tpsLine))));

        String msptLine = mspt < 0 ? notAvailable : String.format(Locale.US, "%.2f ms", mspt);
        inventory.setItem(11, infoItem(Material.COMPARATOR, tr("gui.diagnostics.mspt.name"),
                langList("gui.diagnostics.mspt.lore", java.util.Map.of("value", msptLine))));

        String ramLine = String.format(Locale.US, "%.1f / %.1f MB", used / 1024.0 / 1024.0, max / 1024.0 / 1024.0);
        inventory.setItem(12, infoItem(Material.REDSTONE, tr("gui.diagnostics.ram.name"),
                langList("gui.diagnostics.ram.lore", java.util.Map.of("value", ramLine))));

        inventory.setItem(13, infoItem(Material.SPAWNER, tr("gui.diagnostics.entities.name"),
                langList("gui.diagnostics.entities.lore", java.util.Map.of(
                        "entities", Integer.toString(entities),
                        "chunks", Integer.toString(chunks),
                        "players", Integer.toString(players)
                ))));

        HealthEvaluator.HealthResult health = HealthEvaluator.evaluate(metrics,
                metrics.scanChunks(configFiles.getMain().getInt("performance.lag.max-chunks-scan", 2000)),
                configFiles);
        List<String> healthLore = new ArrayList<>();
        String level = MessageUtil.tr(plugin, "health.level." + health.getLevel().toLowerCase(Locale.ROOT), health.getLevel());
        healthLore.add(tr("gui.diagnostics.health.score", java.util.Map.of("score", health.getScore() + "/100")));
        healthLore.add(tr("gui.diagnostics.health.level", java.util.Map.of("level", level)));
        if (!health.getReasons().isEmpty()) {
            String reasons = health.getReasons().stream()
                    .map(reason -> MessageUtil.tr(plugin, "health.reason." + reason.toLowerCase(Locale.ROOT), reason))
                    .collect(java.util.stream.Collectors.joining(", "));
            healthLore.add(tr("gui.diagnostics.health.factors", java.util.Map.of("factors", reasons)));
        }
        inventory.setItem(14, infoItem(Material.HEART_OF_THE_SEA, tr("gui.diagnostics.health.name"), healthLore));

        PerformanceHistory history = performanceModule.getHistory();
        List<PerformanceHistory.HistorySample> samples = history.getLatestSamples(6);
        if (samples.size() >= 2) {
            double first = samples.get(0).getTps1();
            double last = samples.get(samples.size() - 1).getTps1();
            String trendValue = first > 0 && last > 0
                    ? String.format(Locale.US, "%.2f TPS", (last - first))
                    : tr("general.not-available");
            String trend = tr("gui.diagnostics.trend.lore", java.util.Map.of("value", trendValue));
            inventory.setItem(15, infoItem(Material.PAPER, tr("gui.diagnostics.trend.name"), List.of(trend)));
        }

        inventory.setItem(19, actionItem(Material.BOOK, tr("gui.diagnostics.actions.status.name"),
                langList("gui.diagnostics.actions.status.lore"), "run_status"));
        inventory.setItem(20, actionItem(Material.CLOCK, tr("gui.diagnostics.actions.lag.name"),
                langList("gui.diagnostics.actions.lag.lore"), "run_lag"));
        inventory.setItem(21, actionItem(Material.ANVIL, tr("gui.diagnostics.actions.diagnose.name"),
                langList("gui.diagnostics.actions.diagnose.lore"), "run_diagnose"));
        inventory.setItem(23, actionItem(Material.BARRIER, tr("gui.diagnostics.actions.clearlag-preview.name"),
                langList("gui.diagnostics.actions.clearlag-preview.lore"), "clearlag_preview"));
        inventory.setItem(24, actionItem(Material.CHEST, tr("gui.diagnostics.actions.unload-chunks.name"),
                langList("gui.diagnostics.actions.unload-chunks.lore"), "unload_chunks"));
        inventory.setItem(25, actionItem(Material.TNT, tr("gui.diagnostics.actions.clearlag-confirm.name"),
                langList("gui.diagnostics.actions.clearlag-confirm.lore"), "clearlag_confirm"));
        inventory.setItem(26, actionItem(Material.ARROW, tr("gui.common.back.name"),
                langList("gui.common.back.lore"), "back"));
        player.openInventory(inventory);
    }

    public void openAlerts(Player player) {
        FileConfiguration alerts = configFiles.getAlerts();
        Inventory inventory = Bukkit.createInventory(new PulseGuiHolder(PulseGuiType.ALERTS), 27, tr("gui.alerts.title"));

        boolean enabled = alerts.getBoolean("alerts.enabled", true);
        String enabledLabel = enabled ? tr("general.yes") : tr("general.no");
        inventory.setItem(10, infoItem(enabled ? Material.LIME_DYE : Material.RED_DYE,
                tr("gui.alerts.enabled.name"),
                langList("gui.alerts.enabled.lore", java.util.Map.of("value", enabledLabel))));

        boolean tpsEnabled = alerts.getBoolean("alerts.tps.enabled", true);
        boolean msptEnabled = alerts.getBoolean("alerts.mspt.enabled", true);
        boolean ramEnabled = alerts.getBoolean("alerts.ram.enabled", true);
        boolean entitiesEnabled = alerts.getBoolean("alerts.entities-per-chunk.enabled", true);

        String tpsEnabledLabel = tpsEnabled ? tr("general.yes") : tr("general.no");
        inventory.setItem(12, infoItem(Material.CLOCK, tr("gui.alerts.tps.name"),
                langList("gui.alerts.tps.lore", java.util.Map.of(
                        "enabled", tpsEnabledLabel,
                        "warn", Double.toString(alerts.getDouble("alerts.tps.warning", 18.0)),
                        "crit", Double.toString(alerts.getDouble("alerts.tps.critical", 15.0)),
                        "cooldown", Long.toString(alerts.getLong("alerts.tps.cooldown-seconds", 120)))
                )));

        String msptEnabledLabel = msptEnabled ? tr("general.yes") : tr("general.no");
        inventory.setItem(13, infoItem(Material.COMPARATOR, tr("gui.alerts.mspt.name"),
                langList("gui.alerts.mspt.lore", java.util.Map.of(
                        "enabled", msptEnabledLabel,
                        "warn", Double.toString(alerts.getDouble("alerts.mspt.warning", 45.0)),
                        "crit", Double.toString(alerts.getDouble("alerts.mspt.critical", 55.0)),
                        "cooldown", Long.toString(alerts.getLong("alerts.mspt.cooldown-seconds", 120)))
                )));

        String ramEnabledLabel = ramEnabled ? tr("general.yes") : tr("general.no");
        inventory.setItem(14, infoItem(Material.REDSTONE, tr("gui.alerts.ram.name"),
                langList("gui.alerts.ram.lore", java.util.Map.of(
                        "enabled", ramEnabledLabel,
                        "warn", Double.toString(alerts.getDouble("alerts.ram.warning-percent", 85.0)),
                        "crit", Double.toString(alerts.getDouble("alerts.ram.critical-percent", 95.0)),
                        "cooldown", Long.toString(alerts.getLong("alerts.ram.cooldown-seconds", 180)))
                )));

        String entitiesEnabledLabel = entitiesEnabled ? tr("general.yes") : tr("general.no");
        inventory.setItem(15, infoItem(Material.SPAWNER, tr("gui.alerts.entities.name"),
                langList("gui.alerts.entities.lore", java.util.Map.of(
                        "enabled", entitiesEnabledLabel,
                        "warn", Integer.toString(alerts.getInt("alerts.entities-per-chunk.warning", 150)),
                        "crit", Integer.toString(alerts.getInt("alerts.entities-per-chunk.critical", 250)),
                        "cooldown", Long.toString(alerts.getLong("alerts.entities-per-chunk.cooldown-seconds", 180)))
                )));

        inventory.setItem(19, actionItem(Material.LEVER, tr("gui.alerts.actions.toggle-alerts.name"),
                langList("gui.alerts.actions.toggle-alerts.lore"), "toggle_alerts"));
        inventory.setItem(20, actionItem(tpsEnabled ? Material.LIME_DYE : Material.RED_DYE,
                tr("gui.alerts.actions.toggle-tps.name"),
                langList("gui.alerts.actions.toggle-tps.lore"), "toggle_tps_alert"));
        inventory.setItem(21, actionItem(msptEnabled ? Material.LIME_DYE : Material.RED_DYE,
                tr("gui.alerts.actions.toggle-mspt.name"),
                langList("gui.alerts.actions.toggle-mspt.lore"), "toggle_mspt_alert"));
        inventory.setItem(22, actionItem(ramEnabled ? Material.LIME_DYE : Material.RED_DYE,
                tr("gui.alerts.actions.toggle-ram.name"),
                langList("gui.alerts.actions.toggle-ram.lore"), "toggle_ram_alert"));
        inventory.setItem(23, actionItem(entitiesEnabled ? Material.LIME_DYE : Material.RED_DYE,
                tr("gui.alerts.actions.toggle-entities.name"),
                langList("gui.alerts.actions.toggle-entities.lore"), "toggle_entities_alert"));
        inventory.setItem(24, actionItem(Material.FIREWORK_ROCKET, tr("gui.alerts.actions.test.name"),
                langList("gui.alerts.actions.test.lore"), "alert_test"));
        inventory.setItem(26, actionItem(Material.ARROW, tr("gui.common.back.name"),
                langList("gui.common.back.lore"), "back"));

        player.openInventory(inventory);
    }

    public void openReports(Player player) {
        Inventory inventory = Bukkit.createInventory(new PulseGuiHolder(PulseGuiType.REPORTS), 54, tr("gui.reports.title"));
        if (!moduleManager.isModuleEnabled("moderation")) {
            inventory.setItem(22, infoItem(Material.REDSTONE_BLOCK, tr("gui.reports.disabled.name"),
                    langList("gui.reports.disabled.lore")));
            inventory.setItem(49, actionItem(Material.ARROW, tr("gui.common.back.name"),
                    langList("gui.common.back.lore"), "back"));
            player.openInventory(inventory);
            return;
        }
        ReportService reportService = moderationModule.getReportService();
        List<ReportService.ReportEntry> reports = reportService.getReports().stream()
                .sorted((a, b) -> Long.compare(b.timestamp(), a.timestamp()))
                .limit(28)
                .collect(Collectors.toList());

        int index = 0;
        for (ReportService.ReportEntry report : reports) {
            String time = formatter.format(Instant.ofEpochMilli(report.timestamp()));
            List<String> lore = langList("gui.reports.item.lore", java.util.Map.of(
                    "reporter", report.reporter(),
                    "target", report.target(),
                    "reason", report.reason(),
                    "time", time
            ));
            inventory.setItem(index, infoItem(Material.WRITABLE_BOOK, tr("gui.reports.item.name"), lore));
            index++;
        }

        inventory.setItem(49, actionItem(Material.ARROW, tr("gui.common.back.name"),
                langList("gui.common.back.lore"), "back"));
        player.openInventory(inventory);
    }

    public void openModules(Player player) {
        Inventory inventory = Bukkit.createInventory(new PulseGuiHolder(PulseGuiType.MODULES), 27, tr("gui.modules.title"));
        addModuleToggle(inventory, 10, "performance");
        addModuleToggle(inventory, 12, "logs");
        addModuleToggle(inventory, 14, "communication");
        addModuleToggle(inventory, 16, "moderation");
        inventory.setItem(22, actionItem(Material.REPEATER, tr("gui.modules.reload.name"),
                langList("gui.modules.reload.lore"), "reload"));
        inventory.setItem(26, actionItem(Material.ARROW, tr("gui.common.back.name"),
                langList("gui.common.back.lore"), "back"));
        player.openInventory(inventory);
    }

    public void handleAction(Player player, String action) {
        if (action == null) {
            return;
        }
        switch (action) {
            case "diagnostics" -> openDiagnostics(player);
            case "alerts" -> openAlerts(player);
            case "reports" -> openReports(player);
            case "modules" -> openModules(player);
            case "close" -> player.closeInventory();
            case "back" -> openMain(player);
            case "toggle_alerts" -> toggleAlerts(player);
            case "toggle_tps_alert" -> toggleAlert(player, "tps", tr("gui.alerts.tps.short"));
            case "toggle_mspt_alert" -> toggleAlert(player, "mspt", tr("gui.alerts.mspt.short"));
            case "toggle_ram_alert" -> toggleAlert(player, "ram", tr("gui.alerts.ram.short"));
            case "toggle_entities_alert" -> toggleAlert(player, "entities-per-chunk", tr("gui.alerts.entities.short"));
            case "alert_test" -> player.performCommand("pulse alerttest");
            case "run_status" -> player.performCommand("pulse status");
            case "run_lag" -> player.performCommand("pulse lag");
            case "run_diagnose" -> player.performCommand("pulse diagnose");
            case "clearlag_preview" -> player.performCommand("pulse clearlag preview");
            case "clearlag_confirm" -> player.performCommand("pulse clearlag confirm");
            case "unload_chunks" -> player.performCommand("pulse unloadchunks");
            case "reload" -> player.performCommand("pulse reload");
            default -> {
            }
        }
    }

    public void toggleModule(CommandSender sender, String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            return;
        }
        if (!sender.hasPermission("pulse.admin")) {
            MessageUtil.sendErrorKey(sender, plugin, "general.no-permission");
            return;
        }
        boolean enabled = moduleManager.isModuleEnabled(moduleName);
        FileConfiguration modulesConfig = configFiles.getModules();
        modulesConfig.set("modules." + moduleName.toLowerCase(Locale.ROOT), !enabled);
        configFiles.saveModules();
        plugin.reloadAll();
        String status = !enabled ? tr("general.enabled") : tr("general.disabled");
        String moduleLabel = tr("module." + moduleName, capitalize(moduleName));
        MessageUtil.sendSuccessKey(sender, plugin, "gui.modules.toggle.success",
                java.util.Map.of("module", moduleLabel, "status", status));
    }

    public String getAction(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    public String getModule(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer().get(moduleKey, PersistentDataType.STRING);
    }

    private void toggleAlerts(Player player) {
        if (!player.hasPermission("pulse.admin")) {
            MessageUtil.sendErrorKey(player, plugin, "general.no-permission");
            return;
        }
        FileConfiguration alerts = configFiles.getAlerts();
        boolean enabled = alerts.getBoolean("alerts.enabled", true);
        alerts.set("alerts.enabled", !enabled);
        configFiles.saveAlerts();
        plugin.reloadAll();
        String status = !enabled ? tr("general.enabled") : tr("general.disabled");
        MessageUtil.sendSuccessKey(player, plugin, "gui.alerts.toggle.success",
                java.util.Map.of("status", status));
        openAlerts(player);
    }

    private void toggleAlert(Player player, String key, String label) {
        if (!player.hasPermission("pulse.admin")) {
            MessageUtil.sendErrorKey(player, plugin, "general.no-permission");
            return;
        }
        FileConfiguration alerts = configFiles.getAlerts();
        String path = "alerts." + key + ".enabled";
        boolean enabled = alerts.getBoolean(path, true);
        alerts.set(path, !enabled);
        configFiles.saveAlerts();
        plugin.reloadAll();
        String status = !enabled ? tr("general.enabled") : tr("general.disabled");
        MessageUtil.sendSuccessKey(player, plugin, "gui.alerts.toggle-type.success",
                java.util.Map.of("type", label, "status", status));
        openAlerts(player);
    }

    private void addModuleToggle(Inventory inventory, int slot, String moduleName) {
        boolean enabled = moduleManager.isModuleEnabled(moduleName);
        Material material = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String status = enabled ? tr("general.enabled") : tr("general.disabled");
        List<String> lore = langList("gui.modules.toggle.lore", java.util.Map.of("status", status));
        String moduleLabel = tr("module." + moduleName, capitalize(moduleName));
        ItemStack item = infoItem(material, moduleLabel, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(moduleKey, PersistentDataType.STRING, moduleName);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private ItemStack actionItem(Material material, String name, List<String> lore, String action) {
        ItemStack item = infoItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack infoItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color("&b" + name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream().map(line -> MessageUtil.color("&7" + line)).collect(Collectors.toList()));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private String tr(String key) {
        return MessageUtil.tr(plugin, key);
    }

    private String tr(String key, String fallback) {
        return MessageUtil.tr(plugin, key, fallback);
    }

    private String tr(String key, java.util.Map<String, String> placeholders) {
        return MessageUtil.tr(plugin, key, placeholders);
    }

    private List<String> langList(String key) {
        if (plugin.getLang() == null) {
            return List.of();
        }
        return plugin.getLang().getList(key);
    }

    private List<String> langList(String key, java.util.Map<String, String> placeholders) {
        List<String> lines = langList(key);
        if (lines.isEmpty() || placeholders == null || placeholders.isEmpty()) {
            return lines;
        }
        List<String> replaced = new ArrayList<>();
        for (String line : lines) {
            String value = line;
            for (java.util.Map.Entry<String, String> entry : placeholders.entrySet()) {
                String token = "%" + entry.getKey() + "%";
                value = value.replace(token, entry.getValue() == null ? "" : entry.getValue());
            }
            replaced.add(value);
        }
        return replaced;
    }
}
