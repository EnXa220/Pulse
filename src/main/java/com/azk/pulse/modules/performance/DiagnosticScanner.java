package com.azk.pulse.modules.performance;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class DiagnosticScanner {
    private final int maxChunks;
    private final int blockSamplesPerChunk;

    public DiagnosticScanner(int maxChunks, int blockSamplesPerChunk) {
        this.maxChunks = Math.max(0, maxChunks);
        this.blockSamplesPerChunk = Math.max(0, blockSamplesPerChunk);
    }

    public DiagnosticSnapshot scan() {
        int scannedChunks = 0;
        int totalEntities = 0;
        int totalTileEntities = 0;
        int hopperCount = 0;
        int spawnerCount = 0;
        int containerCount = 0;
        int redstoneSamples = 0;
        int redstoneHits = 0;
        int forcedChunks = 0;
        int chunkTickets = 0;

        Map<EntityType, Integer> entityCounts = new EnumMap<>(EntityType.class);
        Random random = new Random();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (maxChunks > 0 && scannedChunks >= maxChunks) {
                    break;
                }
                scannedChunks++;
                if (chunk.isForceLoaded()) {
                    forcedChunks++;
                }

                Entity[] entities = chunk.getEntities();
                totalEntities += entities.length;
                for (Entity entity : entities) {
                    entityCounts.merge(entity.getType(), 1, Integer::sum);
                }

                BlockState[] states = chunk.getTileEntities();
                totalTileEntities += states.length;
                for (BlockState state : states) {
                    if (state instanceof Hopper) {
                        hopperCount++;
                    }
                    if (state instanceof CreatureSpawner) {
                        spawnerCount++;
                    }
                    if (state instanceof Container) {
                        containerCount++;
                    }
                }

                if (blockSamplesPerChunk > 0) {
                    int minY = world.getMinHeight();
                    int maxY = world.getMaxHeight();
                    for (int i = 0; i < blockSamplesPerChunk; i++) {
                        int x = (chunk.getX() << 4) + random.nextInt(16);
                        int z = (chunk.getZ() << 4) + random.nextInt(16);
                        int y = minY + random.nextInt(Math.max(1, maxY - minY));
                        Block block = world.getBlockAt(x, y, z);
                        if (block == null) {
                            continue;
                        }
                        redstoneSamples++;
                        if (isRedstone(block.getType())) {
                            redstoneHits++;
                        }
                    }
                }
            }
            chunkTickets += countChunkTickets(world);
            if (maxChunks > 0 && scannedChunks >= maxChunks) {
                break;
            }
        }

        return new DiagnosticSnapshot(scannedChunks, totalEntities, totalTileEntities, hopperCount,
                spawnerCount, containerCount, redstoneSamples, redstoneHits, forcedChunks, chunkTickets, entityCounts);
    }

    private boolean isRedstone(Material material) {
        return material == Material.REDSTONE_WIRE
                || material == Material.REPEATER
                || material == Material.COMPARATOR
                || material == Material.OBSERVER
                || material == Material.PISTON
                || material == Material.STICKY_PISTON
                || material == Material.DISPENSER
                || material == Material.DROPPER
                || material == Material.HOPPER;
    }

    private int countChunkTickets(World world) {
        try {
            java.lang.reflect.Method method = world.getClass().getMethod("getChunkTickets");
            Object value = method.invoke(world);
            if (value instanceof java.util.Collection<?> collection) {
                return collection.size();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    public static class DiagnosticSnapshot {
        private final int scannedChunks;
        private final int totalEntities;
        private final int totalTileEntities;
        private final int hopperCount;
        private final int spawnerCount;
        private final int containerCount;
        private final int redstoneSamples;
        private final int redstoneHits;
        private final int forcedChunks;
        private final int chunkTickets;
        private final Map<EntityType, Integer> entityCounts;

        public DiagnosticSnapshot(int scannedChunks, int totalEntities, int totalTileEntities,
                                  int hopperCount, int spawnerCount, int containerCount,
                                  int redstoneSamples, int redstoneHits,
                                  int forcedChunks, int chunkTickets,
                                  Map<EntityType, Integer> entityCounts) {
            this.scannedChunks = scannedChunks;
            this.totalEntities = totalEntities;
            this.totalTileEntities = totalTileEntities;
            this.hopperCount = hopperCount;
            this.spawnerCount = spawnerCount;
            this.containerCount = containerCount;
            this.redstoneSamples = redstoneSamples;
            this.redstoneHits = redstoneHits;
            this.forcedChunks = forcedChunks;
            this.chunkTickets = chunkTickets;
            this.entityCounts = new HashMap<>(entityCounts);
        }

        public int getScannedChunks() {
            return scannedChunks;
        }

        public int getTotalEntities() {
            return totalEntities;
        }

        public int getTotalTileEntities() {
            return totalTileEntities;
        }

        public int getHopperCount() {
            return hopperCount;
        }

        public int getSpawnerCount() {
            return spawnerCount;
        }

        public int getContainerCount() {
            return containerCount;
        }

        public int getRedstoneSamples() {
            return redstoneSamples;
        }

        public int getRedstoneHits() {
            return redstoneHits;
        }

        public int getForcedChunks() {
            return forcedChunks;
        }

        public int getChunkTickets() {
            return chunkTickets;
        }

        public Map<EntityType, Integer> getEntityCounts() {
            return new HashMap<>(entityCounts);
        }

        public double getRedstoneRatio() {
            if (redstoneSamples <= 0) {
                return 0.0;
            }
            return redstoneHits / (double) redstoneSamples;
        }

        public double getEntitiesPerChunk() {
            if (scannedChunks <= 0) {
                return 0.0;
            }
            return totalEntities / (double) scannedChunks;
        }

        public double getHoppersPerChunk() {
            if (scannedChunks <= 0) {
                return 0.0;
            }
            return hopperCount / (double) scannedChunks;
        }

        public double getSpawnersPerChunk() {
            if (scannedChunks <= 0) {
                return 0.0;
            }
            return spawnerCount / (double) scannedChunks;
        }

        public double getTileEntitiesPerChunk() {
            if (scannedChunks <= 0) {
                return 0.0;
            }
            return totalTileEntities / (double) scannedChunks;
        }
    }
}
