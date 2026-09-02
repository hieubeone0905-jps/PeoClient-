package com.peoclient.diagnostic;

import net.minecraft.class_2338;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class TargetHistory {
    private static final TargetHistory instance = new TargetHistory();
    private final ConcurrentLinkedQueue<TargetRecord> history = new ConcurrentLinkedQueue<>();
    private final Map<class_2338, Integer> targetCounts = new HashMap<>();
    private static final int MAX_HISTORY = 200;

    private TargetHistory() {}

    public static TargetHistory get() { return instance; }

    public void recordTarget(class_2338 pos, double distance, int queueOrder) {
        if (!DiagnosticConfig.get().isRecordTargetHistory()) return;
        history.add(new TargetRecord(System.currentTimeMillis(), pos, distance, queueOrder));
        targetCounts.put(pos, targetCounts.getOrDefault(pos, 0) + 1);
        while (history.size() > MAX_HISTORY) {
            TargetRecord old = history.poll();
            if (old != null) {
                targetCounts.computeIfPresent(old.pos, (k, v) -> v > 1 ? v-1 : null);
            }
        }
    }

    public void clear() { history.clear(); targetCounts.clear(); }

    public ConcurrentLinkedQueue<TargetRecord> getHistory() { return new ConcurrentLinkedQueue<>(history); }
    public int getTargetCount(class_2338 pos) { return targetCounts.getOrDefault(pos, 0); }

    public static class TargetRecord {
        public final long timestamp;
        public final class_2338 pos;
        public final double distance;
        public final int queueOrder;
        public TargetRecord(long ts, class_2338 p, double d, int q) {
            this.timestamp = ts; this.pos = p; this.distance = d; this.queueOrder = q;
        }
    }
}