package com.azk.pulse.modules.performance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class PerformanceHistory {
    private final Deque<HistorySample> samples = new ArrayDeque<>();
    private Duration retention = Duration.ofHours(24);

    public void setRetention(Duration retention) {
        if (retention != null && !retention.isNegative() && !retention.isZero()) {
            this.retention = retention;
        }
    }

    public void addSample(HistorySample sample) {
        if (sample == null) {
            return;
        }
        samples.addLast(sample);
        prune();
    }

    public void clear() {
        samples.clear();
    }

    public Duration getRetention() {
        return retention;
    }

    public List<HistorySample> getSamplesSince(Duration duration) {
        Instant cutoff = Instant.now().minus(duration);
        List<HistorySample> result = new ArrayList<>();
        for (HistorySample sample : samples) {
            if (!sample.timestamp.isBefore(cutoff)) {
                result.add(sample);
            }
        }
        return result;
    }

    public List<HistorySample> getLatestSamples(int limit) {
        int safeLimit = Math.max(0, limit);
        if (safeLimit == 0 || samples.isEmpty()) {
            return new ArrayList<>();
        }
        List<HistorySample> result = new ArrayList<>(safeLimit);
        int count = 0;
        java.util.Iterator<HistorySample> iterator = samples.descendingIterator();
        while (iterator.hasNext() && count < safeLimit) {
            HistorySample sample = iterator.next();
            result.add(0, sample);
            count++;
        }
        return result;
    }

    public void exportCsv(File file) throws IOException {
        exportCsv(file, new ArrayList<>(samples));
    }

    public void exportCsv(File file, List<HistorySample> data) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("timestamp,tps_1m,tps_5m,tps_15m,mspt,mem_used,mem_max,players");
            writer.newLine();
            for (HistorySample sample : data) {
                writer.write(formatter.format(sample.timestamp));
                writer.write(",");
                writer.write(Double.toString(sample.tps1));
                writer.write(",");
                writer.write(Double.toString(sample.tps5));
                writer.write(",");
                writer.write(Double.toString(sample.tps15));
                writer.write(",");
                writer.write(Double.toString(sample.mspt));
                writer.write(",");
                writer.write(Long.toString(sample.usedMemory));
                writer.write(",");
                writer.write(Long.toString(sample.maxMemory));
                writer.write(",");
                writer.write(Integer.toString(sample.players));
                writer.newLine();
            }
        }
    }

    private void prune() {
        Instant cutoff = Instant.now().minus(retention);
        while (!samples.isEmpty() && samples.peekFirst().timestamp.isBefore(cutoff)) {
            samples.pollFirst();
        }
    }

    public static final class HistorySample {
        private final Instant timestamp;
        private final double tps1;
        private final double tps5;
        private final double tps15;
        private final double mspt;
        private final long usedMemory;
        private final long maxMemory;
        private final int players;

        public HistorySample(Instant timestamp, double tps1, double tps5, double tps15, double mspt,
                             long usedMemory, long maxMemory, int players) {
            this.timestamp = timestamp;
            this.tps1 = tps1;
            this.tps5 = tps5;
            this.tps15 = tps15;
            this.mspt = mspt;
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.players = players;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public double getTps1() {
            return tps1;
        }

        public double getTps5() {
            return tps5;
        }

        public double getTps15() {
            return tps15;
        }

        public double getMspt() {
            return mspt;
        }

        public long getUsedMemory() {
            return usedMemory;
        }

        public long getMaxMemory() {
            return maxMemory;
        }

        public int getPlayers() {
            return players;
        }
    }
}
