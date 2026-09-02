package com.peoclient.diagnostic;

/** Immutable snapshot of AccountSessionMetrics. */
public final class AccountSessionSnapshot {
    private final String account;
    private final String server;
    private final long sessionStart;
    private final long sessionEnd;
    private final long totalTicks;
    private final int breakAttempts, successes, failures, recoveries;
    private final int lastPing, minPing, maxPing;
    private final double averagePing;
    private final boolean wasKicked;
    private final String disconnectReason;

    public AccountSessionSnapshot(String account, String server, long sessionStart, long sessionEnd,
                                  long totalTicks, int breakAttempts, int successes, int failures,
                                  int recoveries, int lastPing, double averagePing,
                                  int minPing, int maxPing, boolean wasKicked, String disconnectReason) {
        this.account = account; this.server = server; this.sessionStart = sessionStart; this.sessionEnd = sessionEnd;
        this.totalTicks = totalTicks; this.breakAttempts = breakAttempts; this.successes = successes;
        this.failures = failures; this.recoveries = recoveries; this.lastPing = lastPing;
        this.averagePing = averagePing; this.minPing = minPing; this.maxPing = maxPing;
        this.wasKicked = wasKicked; this.disconnectReason = disconnectReason;
    }

    public static AccountSessionSnapshot fromCurrent() {
        AccountSessionMetrics m = AccountSessionMetrics.get();
        long end = System.currentTimeMillis();
        long start = Math.max(0L, end - m.getSessionDurationMs());
        LatencyMetrics l = LatencyMetrics.get();
        return new AccountSessionSnapshot(
                m.getAccountName(), ClientConnectionMonitor.get().getServerAddress(), start, end,
                m.getTotalTicks(), m.getBreakAttempts(), m.getBreakSuccesses(), m.getBreakFailures(),
                m.getRecoveries(), m.getLastPing(), l.getAveragePing(), l.getMinPing(), l.getMaxPing(),
                m.wasKicked(), KickReasonRecorder.get().getLastKickReason());
    }

    public String getAccount() { return account; }
    public String getServer() { return server; }
    public long getSessionStart() { return sessionStart; }
    public long getSessionEnd() { return sessionEnd; }
    public long getTotalTicks() { return totalTicks; }
    public int getBreakAttempts() { return breakAttempts; }
    public int getSuccesses() { return successes; }
    public int getFailures() { return failures; }
    public int getRecoveries() { return recoveries; }
    public int getLastPing() { return lastPing; }
    public double getAveragePing() { return averagePing; }
    public int getMinPing() { return minPing; }
    public int getMaxPing() { return maxPing; }
    public boolean wasKicked() { return wasKicked; }
    public String getDisconnectReason() { return disconnectReason; }
}
