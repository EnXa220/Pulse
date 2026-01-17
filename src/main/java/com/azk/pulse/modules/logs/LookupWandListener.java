package com.azk.pulse.modules.logs;

import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class LookupWandListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigFiles configFiles;
    private final LookupSelectionManager selectionManager;
    private final LookupService lookupService;
    private final NamespacedKey wandKey;

    public LookupWandListener(JavaPlugin plugin, ConfigFiles configFiles, LookupSelectionManager selectionManager,
                              LookupService lookupService) {
        this.plugin = plugin;
        this.configFiles = configFiles;
        this.selectionManager = selectionManager;
        this.lookupService = lookupService;
        this.wandKey = new NamespacedKey(plugin, "lookup_wand");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        if (!isWand(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("pulse.mod")) {
            MessageUtil.sendErrorKey(player, plugin, "general.no-permission");
            return;
        }

        Action action = event.getAction();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (action == Action.LEFT_CLICK_BLOCK) {
            selectionManager.getSelection(player).setPos1(block.getLocation());
            MessageUtil.sendSuccessKey(player, plugin, "lookup.wand.pos1",
                    java.util.Map.of("pos", format(block.getLocation())));
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                sendBlockLookup(player, block.getLocation());
            } else {
                selectionManager.getSelection(player).setPos2(block.getLocation());
                MessageUtil.sendSuccessKey(player, plugin, "lookup.wand.pos2",
                        java.util.Map.of("pos", format(block.getLocation())));
            }
        }
    }

    public ItemStack createWand() {
        Material material = getMaterial("logs.wand-material", Material.STICK);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(MessageUtil.tr(plugin, "lookup.wand.name")));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(wandKey, PersistentDataType.BYTE);
    }

    private void sendBlockLookup(CommandSender sender, Location location) {
        int defaultLimit = configFiles.getMain().getInt("logs.lookup-default-limit", 10);
        int maxLimit = configFiles.getMain().getInt("logs.lookup-max-limit", 200);
        int limit = maxLimit > 0 ? Math.min(defaultLimit, maxLimit) : defaultLimit;
        LogRepository.LookupQuery query = new LogRepository.LookupQuery(
                null,
                null,
                location.getWorld() != null ? location.getWorld().getName() : null,
                null,
                null,
                null,
                null,
                location.getBlockX(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockY(),
                location.getBlockZ(),
                location.getBlockZ(),
                null,
                null,
                null,
                limit,
                0
        );
        String title = "Block logs: " + format(location);
        lookupService.sendLookup(sender, query, title);
    }

    private String format(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private Material getMaterial(String path, Material fallback) {
        String raw = configFiles.getMain().getString(path);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
        return material != null ? material : fallback;
    }
}
