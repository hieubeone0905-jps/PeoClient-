package com.peoclient.diagnostic;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Collections;

/**
 * Chỉ đo timing, không can thiệp.
 */
public final class NukerTimingMetrics {
    private static final NukerTimingMetrics instance = new NukerTimingMetrics();
    private final ConcurrentLinkedQueue<Long> targetToAttempt = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> attemptToInteraction = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> interactionToBlockUpdate = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> blockUpdateToSuccess = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> successToNextTarget = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> attemptToSuccess = new ConcurrentLinkedQueue<>();

    private NukerTimingMetrics() {}

    public static NukerTimingMetrics get() { return instance; }

    public void recordTargetToAttempt(long ms) { targetToAttempt.add(ms); trim(targetToAttempt); }
    public void recordAttemptToInteraction(long ms) { attemptToInteraction.add(ms); trim(attemptToInteraction); }
    public void recordInteractionToBlockUpdate(long ms) { interactionToBlockUpdate.add(ms); trim(interactionToBlockUpdate); }
    public void recordBlockUpdateToSuccess(long ms) { blockUpdateToSuccess.add(ms); trim(blockUpdateToSuccess); }
    public void recordSuccessToNextTarget(long ms) { successToNextTarget.add(ms); trim(successToNextTarget); }
    public void recordAttemptToSuccess(long ms) { attemptToSuccess.add(ms); trim(attemptToSuccess); }

    private void trim(ConcurrentLinkedQueue<Long> q) {
        while (q.size() > 1000) q.poll();
    }

    public TimingStats getStats(ConcurrentLinkedQueue<Long> q) {
        if (q.isEmpty()) return new TimingStats(0, 0, 0, 0, 0, 0);
        long min = Long.MAX_VALUE, max = 0, sum = 0;
        for (long v : q) {
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        double avg = (double) sum / q.size();
        double median = 0;
        // median approximative
        Long[] arr = q.toArray(new Long[0]);
        if (arr.length > 0) {
            java.util.Arrays.sort(arr);
            median = arr[arr.length / 2];
        }
        return new TimingStats(q.size(), min, max, avg, median, sum);
    }

    public TimingStats getTargetToAttemptStats() { return getStats(targetToAttempt); }
    public TimingStats getAttemptToInteractionStats() { return getStats(attemptToInteraction); }
    public TimingStats getInteractionToBlockUpdateStats() { return getStats(interactionToBlockUpdate); }
    public TimingStats getBlockUpdateToSuccessStats() { return getStats(blockUpdateToSuccess); }
    public TimingStats getSuccessToNextTargetStats() { return getStats(successToNextTarget); }
    public TimingStats getAttemptToSuccessStats() { return getStats(attemptToSuccess); }

    public void clear() {
        targetToAttempt.clear(); attemptToInteraction.clear();
        interactionToBlockUpdate.clear(); blockUpdateToSuccess.clear();
        successToNextTarget.clear(); attemptToSuccess.clear();
    }

    public static class TimingStats {
        public final int count;
        public final long min, max;
        public final double average, median, total;
        public TimingStats(int c, long min, long max, double avg, double med, double tot) {
            this.count = c; this.min = min; this.max = max;
            this.average = avg; this.median = med; this.total = tot;
        }
    }
}