package com.azk.pulse.modules.performance;

import com.azk.pulse.commands.PulseSubcommand;
import com.azk.pulse.core.ConfigFiles;
import com.azk.pulse.core.MessageUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class UnloadChunksCommand implements PulseSubcommand {
    private final JavaPlugin plugin;
    private final ConfigFiles configFiles;

    public UnloadChunksCommand(JavaPlugin plugin, ConfigFiles configFiles) {
        this.plugin = plugin;
        this.configFiles = configFiles;
    }

    @Override
    public String name() {
        return "unloadchunks";
    }

    @Override
    public String module() {
        return "performance";
    }

    @Override
    public String permission() {
        return "pulse.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageUtil.sendTitleKey(sender, plugin, "unloadchunks.title");
        String worldName = parseString(args, "world", null);
        World world = null;
        if (worldName != null) {
            world = Bukkit.getWorld(worldName);
        } else if (sender instanceof Player player) {
            world = player.getWorld();
        }

        if (world == null) {
            MessageUtil.sendErrorKey(sender, plugin, "unloadchunks.world-not-found");
            return true;
        }

        int keepSpawnRadius = Math.max(0, configFiles.getMain().getInt("performance.unloadchunks.keep-spawn-radius", 4));
        int playerRadius = Math.max(0, configFiles.getMain().getInt("performance.unloadchunks.player-chunk-radius", 6));

        Set<Long> protectedChunks = new HashSet<>();
        for (Player player : world.getPlayers()) {
            addChunkRadius(protectedChunks, player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(), playerRadius);
        }

        if (world.getKeepSpawnInMemory()) {
            int spawnX = world.getSpawnLocation().getChunk().getX();
            int spawnZ = world.getSpawnLocation().getChunk().getZ();
            addChunkRadius(protectedChunks, spawnX, spawnZ, keepSpawnRadius);
        }

        int unloaded = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.isForceLoaded()) {
                continue;
            }
            long key = chunkKey(chunk.getX(), chunk.getZ());
            if (protectedChunks.contains(key)) {
                continue;
            }
            if (world.unloadChunkRequest(chunk.getX(), chunk.getZ())) {
                unloaded++;
            }
        }

        MessageUtil.sendSuccessKey(sender, plugin, "unloadchunks.success",
                java.util.Map.of("count", Integer.toString(unloaded), "world", world.getName()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> results = new ArrayList<>();
        if (args.length == 1) {
            results.add("world=");
        }
        if (args.length >= 1 && args[args.length - 1].startsWith("world=")) {
            String prefix = args[args.length - 1].substring("world=".length()).toLowerCase(Locale.ROOT);
            for (World world : Bukkit.getWorlds()) {
                String name = world.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    results.add("world=" + name);
                }
            }
        }
        return results;
    }

    private void addChunkRadius(Set<Long> set, int centerX, int centerZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                set.add(chunkKey(centerX + dx, centerZ + dz));
            }
        }
    }

    private long chunkKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private String parseString(String[] args, String key, String fallback) {
        for (String arg : args) {
            if (arg.toLowerCase(Locale.ROOT).startsWith(key + "=")) {
                return arg.substring(key.length() + 1);
            }
        }
        return fallback;
    }
}
