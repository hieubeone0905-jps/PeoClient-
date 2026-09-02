package com.peoclient.diagnostic;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gom các event diagnostic thành timeline.
 */
public final class DiagnosticTimeline {
    private static final DiagnosticTimeline instance = new DiagnosticTimeline();
    private final ConcurrentLinkedQueue<DiagnosticEvent> events = new ConcurrentLinkedQueue<>();
    private final AtomicInteger maxSize = new AtomicInteger(10000);

    private DiagnosticTimeline() {}

    public static DiagnosticTimeline get() { return instance; }

    public void record(DiagnosticEvent event) {
        if (!DiagnosticConfig.get().isEnabled()) return;
        events.add(event);
        while (events.size() > maxSize.get()) {
            events.poll();
        }
    }

    public void record(DiagnosticEvent.Category category, String eventType, String message) {
        record(new DiagnosticEvent.Builder()
                .category(category)
                .eventType(eventType)
                .message(message)
                .build());
    }

    public ConcurrentLinkedQueue<DiagnosticEvent> getRecentEvents() {
        return new ConcurrentLinkedQueue<>(events);
    }

    public ConcurrentLinkedQueue<DiagnosticEvent> getEventsSince(long timestamp) {
        ConcurrentLinkedQueue<DiagnosticEvent> result = new ConcurrentLinkedQueue<>();
        for (DiagnosticEvent e : events) {
            if (e.getTimestamp() >= timestamp) result.add(e);
        }
        return result;
    }

    public ConcurrentLinkedQueue<DiagnosticEvent> getLastSeconds(int seconds) {
        long cutoff = System.currentTimeMillis() - seconds * 1000L;
        return getEventsSince(cutoff);
    }

    public void clear() { events.clear(); }
    public int size() { return events.size(); }
}