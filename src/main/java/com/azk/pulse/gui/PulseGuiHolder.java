package com.azk.pulse.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PulseGuiHolder implements InventoryHolder {
    private final PulseGuiType type;

    public PulseGuiHolder(PulseGuiType type) {
        this.type = type;
    }

    public PulseGuiType getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
