package com.peoclient.diagnostic;

import net.minecraft.class_2338;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class BreakEventRecorder {
    private static final BreakEventRecorder instance = new BreakEventRecorder();
    private final ConcurrentLinkedQueue<BreakEvent> events = new ConcurrentLinkedQueue<>();
    private static final int MAX_EVENTS = 500;

    private BreakEventRecorder() {}

    public static BreakEventRecorder get() { return instance; }

    public void recordStart(class_2338 pos, double range) {
        if (!DiagnosticConfig.get().isRecordBreakEvents()) return;
        events.add(new BreakEvent(System.currentTimeMillis(), pos, "START", range, null));
        trim();
    }

    public void recordSuccess(class_2338 pos, long durationMs) {
        if (!DiagnosticConfig.get().isRecordBreakEvents()) return;
        events.add(new BreakEvent(System.currentTimeMillis(), pos, "SUCCESS", 0, durationMs));
        trim();
    }

    public void recordFailure(class_2338 pos, String reason) {
        if (!DiagnosticConfig.get().isRecordBreakEvents()) return;
        events.add(new BreakEvent(System.currentTimeMillis(), pos, "FAILURE", 0, null, reason));
        trim();
    }

    public void recordRecovery(class_2338 pos, String reason) {
        if (!DiagnosticConfig.get().isRecordBreakEvents()) return;
        events.add(new BreakEvent(System.currentTimeMillis(), pos, "RECOVERY", 0, null, reason));
        trim();
    }

    private void trim() { while (events.size() > MAX_EVENTS) events.poll(); }

    public ConcurrentLinkedQueue<BreakEvent> getEvents() { return new ConcurrentLinkedQueue<>(events); }
    public void clear() { events.clear(); }

    public static class BreakEvent {
        public final long timestamp;
        public final class_2338 pos;
        public final String type;
        public final double range;
        public final Long durationMs;
        public final String reason;
        public BreakEvent(long ts, class_2338 p, String t, double r, Long d) { this(ts, p, t, r, d, null); }
        public BreakEvent(long ts, class_2338 p, String t, double r, Long d, String reason) {
            this.timestamp = ts; this.pos = p; this.type = t; this.range = r; this.durationMs = d; this.reason = reason;
        }
    }
}