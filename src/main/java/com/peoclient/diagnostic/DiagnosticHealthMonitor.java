package com.peoclient.diagnostic;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Kiểm tra sức khỏe các subsystem diagnostic.
 */
public final class DiagnosticHealthMonitor {
    private static final DiagnosticHealthMonitor instance = new DiagnosticHealthMonitor();
    private final ConcurrentLinkedQueue<String> warnings = new ConcurrentLinkedQueue<>();
    private volatile boolean healthy = true;

    private DiagnosticHealthMonitor() {}

    public static DiagnosticHealthMonitor get() { return instance; }

    public void checkHealth() {
        warnings.clear();
        healthy = true;

        if (!DiagnosticConfig.get().isEnabled()) {
            warnings.add("Diagnostic disabled by config");
            healthy = false;
            return;
        }

        // Kiểm tra các subsystem cơ bản
        try {
            DiagnosticRecorder.get().flush(); // test write
        } catch (Exception e) {
            warnings.add("DiagnosticRecorder error: " + e.getMessage());
            healthy = false;
        }

        try {
            DiagnosticTimeline.get().size();
        } catch (Exception e) {
            warnings.add("DiagnosticTimeline error: " + e.getMessage());
            healthy = false;
        }

        try {
            PreKickSnapshot.get().getSnapshot();
        } catch (Exception e) {
            warnings.add("PreKickSnapshot error: " + e.getMessage());
            healthy = false;
        }

        try {
            PreDisconnectSnapshot.get().getAll();
        } catch (Exception e) {
            warnings.add("PreDisconnectSnapshot error: " + e.getMessage());
            healthy = false;
        }

        try {
            LatencyMetrics.get().getLastPing();
        } catch (Exception e) {
            warnings.add("LatencyMetrics error: " + e.getMessage());
            healthy = false;
        }

        try {
            TickMetrics.get().getTotalTicks();
        } catch (Exception e) {
            warnings.add("TickMetrics error: " + e.getMessage());
            healthy = false;
        }

        try {
            BreakEventRecorder.get().getEvents();
        } catch (Exception e) {
            warnings.add("BreakEventRecorder error: " + e.getMessage());
            healthy = false;
        }

        try {
            TargetHistory.get().getHistory();
        } catch (Exception e) {
            warnings.add("TargetHistory error: " + e.getMessage());
            healthy = false;
        }

        try {
            ServerResponseMonitor.get().getResponses();
        } catch (Exception e) {
            warnings.add("ServerResponseMonitor error: " + e.getMessage());
            healthy = false;
        }

        try {
            ClientConnectionMonitor.get().getState();
        } catch (Exception e) {
            warnings.add("ClientConnectionMonitor error: " + e.getMessage());
            healthy = false;
        }

        if (healthy) {
            DiagnosticRecorder.get().record("HealthCheck", "All systems OK");
        } else {
            DiagnosticRecorder.get().record("HealthCheck", "Warnings: " + String.join("; ", warnings));
        }
    }

    public boolean isHealthy() { return healthy; }
    public ConcurrentLinkedQueue<String> getWarnings() { return new ConcurrentLinkedQueue<>(warnings); }
    public void clearWarnings() { warnings.clear(); }
}