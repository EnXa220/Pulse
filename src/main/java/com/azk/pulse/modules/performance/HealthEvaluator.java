package com.azk.pulse.modules.performance;

import com.azk.pulse.core.ConfigFiles;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public final class HealthEvaluator {
    private HealthEvaluator() {
    }

    public static HealthResult evaluate(PerformanceMetrics metrics, PerformanceMetrics.ChunkScanResult scan, ConfigFiles configFiles) {
        FileConfiguration alerts = configFiles.getAlerts();
        double tps = metrics.getTps()[0];
        double mspt = metrics.getAverageTickTime();
        double ramPercent = metrics.getMemoryUsagePercent();
        double avgEntities = scan.getAverageEntitiesPerChunk();

        double warnTps = alerts.getDouble("alerts.tps.warning", 18.0);
        double critTps = alerts.getDouble("alerts.tps.critical", 15.0);
        double warnMspt = alerts.getDouble("alerts.mspt.warning", 45.0);
        double critMspt = alerts.getDouble("alerts.mspt.critical", 55.0);
        double warnRam = alerts.getDouble("alerts.ram.warning-percent", 85.0);
        double critRam = alerts.getDouble("alerts.ram.critical-percent", 95.0);
        int warnEntities = alerts.getInt("alerts.entities-per-chunk.warning", 150);
        int critEntities = alerts.getInt("alerts.entities-per-chunk.critical", 250);

        int score = 100;
        List<String> reasons = new ArrayList<>();

        if (tps > 0 && tps < warnTps) {
            score -= tps < critTps ? 30 : 15;
            reasons.add("low-tps");
        }

        if (mspt > 0 && mspt > warnMspt) {
            score -= mspt > critMspt ? 25 : 12;
            reasons.add("high-mspt");
        }

        if (ramPercent > 0 && ramPercent > warnRam) {
            score -= ramPercent > critRam ? 20 : 10;
            reasons.add("high-ram");
        }

        if (avgEntities > 0 && avgEntities > warnEntities) {
            score -= avgEntities > critEntities ? 20 : 10;
            reasons.add("dense-chunks");
        }

        score = Math.max(0, Math.min(100, score));
        String level;
        if (score >= 80) {
            level = "GOOD";
        } else if (score >= 60) {
            level = "FAIR";
        } else if (score >= 40) {
            level = "POOR";
        } else {
            level = "CRITICAL";
        }

        return new HealthResult(score, level, reasons);
    }

    public static final class HealthResult {
        private final int score;
        private final String level;
        private final List<String> reasons;

        public HealthResult(int score, String level, List<String> reasons) {
            this.score = score;
            this.level = level;
            this.reasons = reasons;
        }

        public int getScore() {
            return score;
        }

        public String getLevel() {
            return level;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }
}
