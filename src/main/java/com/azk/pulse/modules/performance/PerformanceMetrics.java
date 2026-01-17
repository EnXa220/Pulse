package com.azk.pulse.modules.performance;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class PerformanceMetrics {
    public PerformanceMetrics(JavaPlugin plugin) {
    }

    public double[] getTps() {
        double[] tps = readTps("getTPS");
        if (tps != null) {
            return tps;
        }
        tps = readTps("getRecentTps");
        if (tps != null) {
            return tps;
        }
        tps = readSpigotTps();
        if (tps != null) {
            return tps;
        }
        tps = readTpsField();
        if (tps != null) {
            return tps;
        }
        return new double[] { -1, -1, -1 };
    }

    private double[] readTps(String methodName) {
        try {
            Object value = Bukkit.getServer().getClass().getMethod(methodName).invoke(Bukkit.getServer());
            if (value instanceof double[] tps) {
                if (tps.length >= 3) {
                    return new double[] { tps[0], tps[1], tps[2] };
                }
                return tps;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private double[] readSpigotTps() {
        try {
            Object server = Bukkit.getServer();
            Object spigot = server.getClass().getMethod("spigot").invoke(server);
            Object value = spigot.getClass().getMethod("getTPS").invoke(spigot);
            if (value instanceof double[] tps) {
                if (tps.length >= 3) {
                    return new double[] { tps[0], tps[1], tps[2] };
                }
                return tps;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private double[] readTpsField() {
        try {
            Object server = Bukkit.getServer();
            Object minecraftServer = server.getClass().getMethod("getServer").invoke(server);
            java.lang.reflect.Field field = minecraftServer.getClass().getDeclaredField("recentTps");
            field.setAccessible(true);
            Object value = field.get(minecraftServer);
            if (value instanceof double[] tps) {
                if (tps.length >= 3) {
                    return new double[] { tps[0], tps[1], tps[2] };
                }
                return tps;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public double getAverageTickTime() {
        try {
            Object value = Bukkit.getServer().getClass().getMethod("getAverageTickTime").invoke(Bukkit.getServer());
            if (value instanceof Double mspt) {
                return mspt;
            }
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Exception ignored) {
        }
        return -1.0;
    }

    public double getProcessCpuLoad() {
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            double load = sunBean.getProcessCpuLoad();
            if (load >= 0) {
                return load * 100.0;
            }
        }
        return -1.0;
    }

    public long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public int getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    public int getLoadedChunks() {
        int total = 0;
        for (World world : Bukkit.getWorlds()) {
            total += world.getLoadedChunks().length;
        }
        return total;
    }

    public int getTotalEntities() {
        int total = 0;
        for (World world : Bukkit.getWorlds()) {
            total += world.getEntities().size();
        }
        return total;
    }

    public double getMemoryUsagePercent() {
        long max = getMaxMemory();
        if (max <= 0) {
            return -1.0;
        }
        return (getUsedMemory() / (double) max) * 100.0;
    }

    public PerformanceHistory.HistorySample captureSample() {
        double[] tps = getTps();
        double mspt = getAverageTickTime();
        long used = getUsedMemory();
        long max = getMaxMemory();
        int players = getOnlinePlayers();
        return new PerformanceHistory.HistorySample(Instant.now(), tps[0], tps[1], tps[2], mspt, used, max, players);
    }

    public ChunkScanResult scanChunks(int maxChunks) {
        int scanned = 0;
        int totalEntities = 0;
        int maxEntities = 0;
        String maxChunk = "";
        boolean truncated = false;

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (maxChunks > 0 && scanned >= maxChunks) {
                    truncated = true;
                    break;
                }
                int entities = chunk.getEntities().length;
                scanned++;
                totalEntities += entities;
                if (entities > maxEntities) {
                    maxEntities = entities;
                    maxChunk = world.getName() + ":" + chunk.getX() + "," + chunk.getZ();
                }
            }
            if (truncated) {
                break;
            }
        }

        return new ChunkScanResult(scanned, totalEntities, maxEntities, maxChunk, truncated);
    }

    public List<ChunkEntityInfo> collectChunkEntityInfo(int maxChunks) {
        int scanned = 0;
        boolean truncated = false;
        List<ChunkEntityInfo> entries = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (maxChunks > 0 && scanned >= maxChunks) {
                    truncated = true;
                    break;
                }
                int entities = chunk.getEntities().length;
                entries.add(new ChunkEntityInfo(world.getName(), chunk.getX(), chunk.getZ(), entities));
                scanned++;
            }
            if (truncated) {
                break;
            }
        }

        if (truncated) {
            entries.add(ChunkEntityInfo.truncated());
        }

        return entries;
    }

    public static final class ChunkScanResult {
        private final int scannedChunks;
        private final int totalEntities;
        private final int maxEntities;
        private final String maxChunk;
        private final boolean truncated;

        public ChunkScanResult(int scannedChunks, int totalEntities, int maxEntities, String maxChunk, boolean truncated) {
            this.scannedChunks = scannedChunks;
            this.totalEntities = totalEntities;
            this.maxEntities = maxEntities;
            this.maxChunk = maxChunk;
            this.truncated = truncated;
        }

        public int getScannedChunks() {
            return scannedChunks;
        }

        public int getTotalEntities() {
            return totalEntities;
        }

        public int getMaxEntities() {
            return maxEntities;
        }

        public String getMaxChunk() {
            return maxChunk;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public double getAverageEntitiesPerChunk() {
            if (scannedChunks <= 0) {
                return 0.0;
            }
            return totalEntities / (double) scannedChunks;
        }
    }

    public static final class ChunkEntityInfo {
        private final String world;
        private final int x;
        private final int z;
        private final int entities;
        private final boolean truncatedMarker;

        public ChunkEntityInfo(String world, int x, int z, int entities) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.entities = entities;
            this.truncatedMarker = false;
        }

        private ChunkEntityInfo() {
            this.world = "";
            this.x = 0;
            this.z = 0;
            this.entities = 0;
            this.truncatedMarker = true;
        }

        public static ChunkEntityInfo truncated() {
            return new ChunkEntityInfo();
        }

        public boolean isTruncatedMarker() {
            return truncatedMarker;
        }

        public String getWorld() {
            return world;
        }

        public int getX() {
            return x;
        }

        public int getZ() {
            return z;
        }

        public int getEntities() {
            return entities;
        }
    }
}
