package com.azk.pulse.modules.logs;

import com.azk.pulse.core.ConfigFiles;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.java.JavaPlugin;

public class LogListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigFiles configFiles;
    private final LogRepository repository;

    public LogListener(JavaPlugin plugin, ConfigFiles configFiles, LogRepository repository) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.repository = repository;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEnabled("logs.log-blocks")) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        repository.logBlock(player.getName(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                "BREAK", block.getType().name());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isEnabled("logs.log-blocks")) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        repository.logBlock(player.getName(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                "PLACE", block.getType().name());
    }

    @EventHandler
    public void onChestOpen(PlayerInteractEvent event) {
        if (!isEnabled("logs.log-chests")) {
            return;
        }
        if (event.isCancelled() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (!(block.getState() instanceof Container)) {
            return;
        }
        Player player = event.getPlayer();
        repository.logChest(player.getName(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                "OPEN", block.getType().name());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isEnabled("logs.log-commands")) {
            return;
        }
        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        String command = message.startsWith("/") ? message.substring(1) : message;
        repository.logCommand(event.getPlayer().getName(), command);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!isEnabled("logs.log-deaths")) {
            return;
        }
        Player player = event.getEntity();
        String killer = player.getKiller() != null ? player.getKiller().getName() : "NONE";
        String cause = player.getLastDamageCause() != null
                ? player.getLastDamageCause().getCause().name()
                : "UNKNOWN";
        repository.logDeath(player.getName(), killer, cause);
    }

    private boolean isEnabled(String path) {
        return configFiles.getMain().getBoolean("logs.enabled", true)
                && configFiles.getMain().getBoolean(path, true);
    }
}
