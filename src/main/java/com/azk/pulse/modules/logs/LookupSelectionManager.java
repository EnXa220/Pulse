package com.azk.pulse.modules.logs;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LookupSelectionManager {
    private final Map<UUID, LookupSelection> selections = new ConcurrentHashMap<>();

    public LookupSelection getSelection(Player player) {
        return selections.computeIfAbsent(player.getUniqueId(), id -> new LookupSelection());
    }

    public void clear(Player player) {
        selections.remove(player.getUniqueId());
    }

    public SelectionRegion getRegion(Player player) {
        LookupSelection selection = selections.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            return null;
        }
        Location pos1 = selection.getPos1();
        Location pos2 = selection.getPos2();
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
            return null;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            return null;
        }
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        return new SelectionRegion(pos1.getWorld().getName(), minX, maxX, minY, maxY, minZ, maxZ);
    }

    public record SelectionRegion(String world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        public long volume() {
            return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }
    }
}
