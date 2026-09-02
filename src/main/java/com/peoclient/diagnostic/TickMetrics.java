package com.peoclient.diagnostic;

import net.minecraft.class_310;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class TickMetrics {
    private static final TickMetrics instance = new TickMetrics();
    private final ConcurrentLinkedQueue<Long> tickDurations = new ConcurrentLinkedQueue<>();
    private volatile long totalTicks = 0;
    private volatile long totalNanos = 0;
    private volatile long minNanos = Long.MAX_VALUE;
    private volatile long maxNanos = 0;
    private volatile long lastTickTime = 0;
    private static final int MAX_SAMPLES = 1000;

    private TickMetrics() {}

    public static TickMetrics get() { return instance; }

    public void recordTickStart() {
        lastTickTime = System.nanoTime();
    }

    public void recordTickEnd() {
        if (lastTickTime == 0) return;
        long now = System.nanoTime();
        long duration = now - lastTickTime;
        totalTicks++;
        totalNanos += duration;
        if (duration < minNanos) minNanos = duration;
        if (duration > maxNanos) maxNanos = duration;
        tickDurations.add(duration);
        while (tickDurations.size() > MAX_SAMPLES) tickDurations.poll();
        lastTickTime = 0;
    }

    public double getAverageTickMs() {
        if (totalTicks == 0) return 0;
        return (totalNanos / (double)totalTicks) / 1_000_000.0;
    }

    public double getMinTickMs() { return minNanos == Long.MAX_VALUE ? 0 : minNanos / 1_000_000.0; }
    public double getMaxTickMs() { return maxNanos / 1_000_000.0; }
    public long getTotalTicks() { return totalTicks; }
    public ConcurrentLinkedQueue<Long> getRecentTickDurations() { return new ConcurrentLinkedQueue<>(tickDurations); }
    public void reset() { totalTicks = 0; totalNanos = 0; minNanos = Long.MAX_VALUE; maxNanos = 0; tickDurations.clear(); }
}