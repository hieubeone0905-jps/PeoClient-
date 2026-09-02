package com.peoclient.diagnostic;

import net.minecraft.class_310;
import net.minecraft.class_2338;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

/**
 * Theo dõi phiên Nuker (ON → OFF hoặc disconnect).
 * Không làm chậm Nuker.
 */
public final class NukerSessionRecorder {
    private static final NukerSessionRecorder instance = new NukerSessionRecorder();
    private volatile Session currentSession = null;
    private final ConcurrentLinkedQueue<Session> history = new ConcurrentLinkedQueue<>();
    private static final int MAX_HISTORY = 10;

    private NukerSessionRecorder() {}

    public static NukerSessionRecorder get() { return instance; }

    public void startSession() {
        if (currentSession != null) return;
        class_310 mc = class_310.method_1551();
        String account = mc.method_1548() != null ? mc.method_1548().method_1676() : "UNKNOWN";
        String server = DiagnosticUtil.serverAddress(mc);
        currentSession = new Session(account, server);
        DiagnosticRecorder.get().record("NukerSession", "Started");
    }

    public void endSession() {
        if (currentSession == null) return;
        currentSession.endTime = System.currentTimeMillis();
        history.add(currentSession);
        while (history.size() > MAX_HISTORY) history.poll();
        DiagnosticRecorder.get().record("NukerSession", "Ended. Attempts=" + currentSession.attempts +
                " Success=" + currentSession.successes + " Fail=" + currentSession.failures);
        currentSession = null;
    }

    public void recordTarget(class_2338 pos) {
        if (currentSession == null) return;
        currentSession.targetCount++;
        if (currentSession.lastTarget != null && !currentSession.lastTarget.equals(pos)) {
            currentSession.targetChanges++;
        }
        currentSession.lastTarget = pos;
    }

    public void recordAttempt() {
        if (currentSession == null) return;
        currentSession.attempts++;
        long now = System.currentTimeMillis();
        if (currentSession.lastAttemptTime > 0) {
            long diff = now - currentSession.lastAttemptTime;
            currentSession.attemptIntervals.add(diff);
        }
        currentSession.lastAttemptTime = now;
    }

    public void recordSuccess(long durationMs) {
        if (currentSession == null) return;
        currentSession.successes++;
        currentSession.breakDurations.add(durationMs);
    }

    public void recordFailure() {
        if (currentSession == null) return;
        currentSession.failures++;
    }

    public void recordRecovery() {
        if (currentSession == null) return;
        currentSession.recoveries++;
    }

    public void recordStateTransition(String from, String to) {
        if (currentSession == null) return;
        currentSession.stateTransitions++;
    }

    public Session getCurrentSession() { return currentSession; }

    public boolean isActive() { return currentSession != null; }

    public SessionSnapshot snapshot() {
        if (currentSession == null) return null;
        return currentSession.snapshot();
    }

    public void reset() {
        currentSession = null;
        history.clear();
    }

    public static class Session {
        public final String sessionId;
        public final String account;
        public final String server;
        public final long startTime;
        public long endTime;
        public int attempts, successes, failures, recoveries;
        public int targetCount, targetChanges, stateTransitions, stagnantCount;
        public class_2338 lastTarget;
        public long lastAttemptTime;
        public final ConcurrentLinkedQueue<Long> breakDurations = new ConcurrentLinkedQueue<>();
        public final ConcurrentLinkedQueue<Long> attemptIntervals = new ConcurrentLinkedQueue<>();

        public Session(String account, String server) {
            this.sessionId = UUID.randomUUID().toString();
            this.account = account;
            this.server = server;
            this.startTime = System.currentTimeMillis();
            this.endTime = -1;
        }

        public SessionSnapshot snapshot() {
            return new SessionSnapshot(
                    sessionId, account, server, startTime, endTime,
                    attempts, successes, failures, recoveries,
                    targetCount, targetChanges, stateTransitions, stagnantCount,
                    lastTarget,
                    breakDurations, attemptIntervals
            );
        }
    }

    public static class SessionSnapshot {
        public final String sessionId, account, server;
        public final long startTime, endTime;
        public final int attempts, successes, failures, recoveries;
        public final int targetCount, targetChanges, stateTransitions, stagnantCount;
        public final class_2338 lastTarget;
        public final ConcurrentLinkedQueue<Long> breakDurations;
        public final ConcurrentLinkedQueue<Long> attemptIntervals;

        public SessionSnapshot(String id, String acc, String svr, long start, long end,
                               int att, int succ, int fail, int rec,
                               int tCount, int tChanges, int sTrans, int stag,
                               class_2338 last, ConcurrentLinkedQueue<Long> bd, ConcurrentLinkedQueue<Long> ai) {
            this.sessionId = id; this.account = acc; this.server = svr;
            this.startTime = start; this.endTime = end;
            this.attempts = att; this.successes = succ; this.failures = fail; this.recoveries = rec;
            this.targetCount = tCount; this.targetChanges = tChanges;
            this.stateTransitions = sTrans; this.stagnantCount = stag;
            this.lastTarget = last;
            this.breakDurations = new ConcurrentLinkedQueue<>(bd);
            this.attemptIntervals = new ConcurrentLinkedQueue<>(ai);
        }
    }
}