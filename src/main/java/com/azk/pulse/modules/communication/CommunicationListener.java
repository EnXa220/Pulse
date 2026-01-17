package com.azk.pulse.modules.communication;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CommunicationListener implements Listener {
    private final JavaPlugin plugin;
    private final CommunicationService service;

    public CommunicationListener(JavaPlugin plugin, CommunicationService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        service.handlePlayerCountChange();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, service::handlePlayerCountChange, 1L);
    }
}
