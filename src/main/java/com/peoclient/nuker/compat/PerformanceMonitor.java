package com.peoclient.nuker.compat;

import com.peoclient.diagnostic.NukerTimingMetrics;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Read-only Nuker timing monitor. Never throttles or modifies Nuker settings. */
public final class PerformanceMonitor {
    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();
    private static final int MAX_SAMPLES = 1000;
    private final ConcurrentLinkedQueue<Long> breakDurations = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> attemptIntervals = new ConcurrentLinkedQueue<>();
    private volatile long lastAttemptTime;

    private PerformanceMonitor() {}
    public static PerformanceMonitor get() { return INSTANCE; }

    public void recordBreakDuration(long ms) {
        if (ms < 0) return;
        breakDurations.add(ms);
        trim(breakDurations);
    }

    public void recordAttemptInterval() {
        long now = System.currentTimeMillis();
        long previous = lastAttemptTime;
        lastAttemptTime = now;
        if (previous <= 0) return;
        long diff = now - previous;
        if (diff >= 0) {
            attemptIntervals.add(diff);
            trim(attemptIntervals);
        }
    }

    private static void trim(ConcurrentLinkedQueue<Long> q) {
        while (q.size() > MAX_SAMPLES) q.poll();
    }

    public double getAverageBreakDuration() { return average(breakDurations); }
    public double getAverageAttemptInterval() { return average(attemptIntervals); }
    public ConcurrentLinkedQueue<Long> getBreakDurations() { return new ConcurrentLinkedQueue<>(breakDurations); }
    public ConcurrentLinkedQueue<Long> getAttemptIntervals() { return new ConcurrentLinkedQueue<>(attemptIntervals); }

    private static double average(ConcurrentLinkedQueue<Long> q) {
        if (q.isEmpty()) return 0.0;
        long sum = 0;
        for (long value : q) sum += value;
        return (double) sum / q.size();
    }

    public void clear() {
        breakDurations.clear();
        attemptIntervals.clear();
        lastAttemptTime = 0L;
    }
}
