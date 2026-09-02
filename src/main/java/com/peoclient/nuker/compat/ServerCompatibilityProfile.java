package com.peoclient.nuker.compat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Per-server, client-side compatibility statistics only. */
public final class ServerCompatibilityProfile {
    private static final ServerCompatibilityProfile INSTANCE = new ServerCompatibilityProfile();
    private final ConcurrentHashMap<String, ServerStats> stats = new ConcurrentHashMap<>();

    private ServerCompatibilityProfile() {}
    public static ServerCompatibilityProfile get() { return INSTANCE; }

    public void recordBreakAttempt(String server, boolean success, long durationMs) {
        if (server == null || server.isBlank()) return;
        ServerStats s = stats.computeIfAbsent(server, ServerStats::new);
        s.totalAttempts.incrementAndGet();
        if (success) {
            s.successfulAttempts.incrementAndGet();
            if (durationMs >= 0) s.totalBreakDuration.addAndGet(durationMs);
        } else {
            s.failedAttempts.incrementAndGet();
        }
    }

    public void recordRecovery(String server) {
        if (server == null || server.isBlank()) return;
        stats.computeIfAbsent(server, ServerStats::new).recoveries.incrementAndGet();
    }

    public void recordPing(String server, int ping) {
        if (server == null || server.isBlank() || ping < 0) return;
        ServerStats s = stats.computeIfAbsent(server, ServerStats::new);
        s.pingSum.addAndGet(ping);
        s.pingCount.incrementAndGet();
        s.maxPing.accumulateAndGet(ping, Math::max);
        s.minPing.updateAndGet(old -> old == Integer.MAX_VALUE ? ping : Math.min(old, ping));
    }

    public ServerStats getStats(String server) { return stats.get(server); }
    public void clear() { stats.clear(); }

    public static final class ServerStats {
        public final String server;
        public final AtomicInteger totalAttempts = new AtomicInteger();
        public final AtomicInteger successfulAttempts = new AtomicInteger();
        public final AtomicInteger failedAttempts = new AtomicInteger();
        public final AtomicInteger recoveries = new AtomicInteger();
        public final AtomicLong totalBreakDuration = new AtomicLong();
        public final AtomicInteger pingSum = new AtomicInteger();
        public final AtomicInteger pingCount = new AtomicInteger();
        public final AtomicInteger minPing = new AtomicInteger(Integer.MAX_VALUE);
        public final AtomicInteger maxPing = new AtomicInteger();

        private ServerStats(String server) { this.server = server; }
        public double getSuccessRate() {
            int total = totalAttempts.get();
            return total == 0 ? 0.0 : (double) successfulAttempts.get() / total;
        }
        public double getAverageBreakDuration() {
            int total = successfulAttempts.get();
            return total == 0 ? 0.0 : (double) totalBreakDuration.get() / total;
        }
        public int getAveragePing() {
            int count = pingCount.get();
            return count == 0 ? -1 : pingSum.get() / count;
        }
        public int getMinPing() { return minPing.get() == Integer.MAX_VALUE ? -1 : minPing.get(); }
        public int getMaxPing() { return maxPing.get(); }
    }
}
