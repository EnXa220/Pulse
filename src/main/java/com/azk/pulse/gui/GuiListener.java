package com.azk.pulse.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {
    private final GuiManager guiManager;

    public GuiListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PulseGuiHolder guiHolder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack stack = event.getCurrentItem();
        if (stack == null) {
            return;
        }

        String action = guiManager.getAction(stack);
        if (action != null) {
            guiManager.handleAction(player, action);
            return;
        }

        if (guiHolder.getType() == PulseGuiType.MODULES) {
            String module = guiManager.getModule(stack);
            if (module != null) {
                guiManager.toggleModule(player, module);
                guiManager.openModules(player);
            }
        }
    }

    
}
