package com.peoclient.diagnostic;

import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Detailed, throttled diagnostics for Nuker render synchronization.
 * Render-only: never changes world state and never sends packets.
 */
public final class NukerRenderDiagnostics {
    private static final AtomicInteger WORLD_CHANGES = new AtomicInteger();
    private static final AtomicInteger SECTION_QUEUES = new AtomicInteger();
    private static final AtomicInteger FLUSHES = new AtomicInteger();
    private static final AtomicInteger HARD_REFRESHES = new AtomicInteger();
    private static final AtomicInteger MISMATCH_CHECKS = new AtomicInteger();

    private static long lastSummaryMs;
    private static long lastHardRefreshMs;

    private NukerRenderDiagnostics() {}

    public static void worldChanged(class_2338 pos, class_2680 oldState, class_2680 newState) {
        if (!DiagnosticConfig.get().isEnabled()) return;
        int n = WORLD_CHANGES.incrementAndGet();
        if (n <= 20 || n % 50 == 0) {
            DiagnosticRecorder.get().record("NukerRender",
                    "WORLD_CHANGE pos=" + pos + " old=" + shortState(oldState)
                            + " new=" + shortState(newState));
        }
    }

    public static void sectionQueued(int count) {
        if (!DiagnosticConfig.get().isEnabled()) return;
        SECTION_QUEUES.addAndGet(Math.max(0, count));
    }

    public static void flushed(int sections) {
        if (!DiagnosticConfig.get().isEnabled()) return;
        FLUSHES.incrementAndGet();
        if (sections > 0) {
            DiagnosticRecorder.get().record("NukerRender",
                    "FLUSH sections=" + sections + " pendingWorldChanges=" + WORLD_CHANGES.get());
        }
    }

    public static boolean shouldHardRefresh() {
        long now = System.currentTimeMillis();
        if (now - lastHardRefreshMs < 1500L) return false;
        lastHardRefreshMs = now;
        return true;
    }

    public static void hardRefresh(class_310 mc) {
        if (!DiagnosticConfig.get().isEnabled()) return;
        HARD_REFRESHES.incrementAndGet();
        DiagnosticRecorder.get().record("NukerRender",
                "HARD_REFRESH renderer.reload() after prolonged Nuker activity");
    }

    public static void recordMismatch(class_2338 pos, class_2680 state) {
        if (!DiagnosticConfig.get().isEnabled()) return;
        int n = MISMATCH_CHECKS.incrementAndGet();
        if (n <= 20 || n % 50 == 0) {
            DiagnosticRecorder.get().record("NukerRender",
                    "STATE_CHECK pos=" + pos + " clientState=" + shortState(state));
        }
    }

    public static void summary() {
        if (!DiagnosticConfig.get().isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastSummaryMs < 5000L) return;
        lastSummaryMs = now;
        DiagnosticRecorder.get().record("NukerRender",
                "SUMMARY worldChanges=" + WORLD_CHANGES.get()
                        + " sectionQueues=" + SECTION_QUEUES.get()
                        + " flushes=" + FLUSHES.get()
                        + " hardRefreshes=" + HARD_REFRESHES.get()
                        + " stateChecks=" + MISMATCH_CHECKS.get());
        DiagnosticRecorder.get().flush();
    }

    private static String shortState(class_2680 state) {
        return state == null ? "null" : state.method_26204().toString();
    }
}
