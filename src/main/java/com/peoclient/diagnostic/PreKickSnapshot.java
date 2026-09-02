package com.peoclient.diagnostic;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class PreKickSnapshot {
    private static final PreKickSnapshot instance = new PreKickSnapshot();
    private final ConcurrentLinkedQueue<SnapshotEntry> ringBuffer = new ConcurrentLinkedQueue<>();
    private static final int MAX_ENTRIES = 600; // 30 seconds * 20 ticks

    private PreKickSnapshot() {}

    public static PreKickSnapshot get() { return instance; }

    public void record(String event) {
        ringBuffer.add(new SnapshotEntry(System.currentTimeMillis(), event));
        while (ringBuffer.size() > MAX_ENTRIES) ringBuffer.poll();
    }

    public void clear() { ringBuffer.clear(); }

    public ConcurrentLinkedQueue<SnapshotEntry> getSnapshot() {
        return new ConcurrentLinkedQueue<>(ringBuffer);
    }

    public static class SnapshotEntry {
        public final long timestamp;
        public final String event;
        public SnapshotEntry(long ts, String event) { this.timestamp = ts; this.event = event; }
    }
}