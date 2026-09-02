package com.peoclient.diagnostic;

import net.minecraft.class_310;

public final class AccountSessionMetrics {
    private static final AccountSessionMetrics instance = new AccountSessionMetrics();
    private volatile String accountName = "unknown";
    private volatile long sessionStart = 0;
    private volatile long totalTicks = 0;
    private volatile int breakAttempts = 0;
    private volatile int breakSuccesses = 0;
    private volatile int breakFailures = 0;
    private volatile int recoveries = 0;
    private volatile int lastPing = -1;
    private volatile boolean wasKicked = false;

    private AccountSessionMetrics() {}

    public static AccountSessionMetrics get() { return instance; }

    public void startSession() {
        accountName = class_310.method_1551().method_1548().method_1676();
        sessionStart = System.currentTimeMillis();
        totalTicks = 0;
        breakAttempts = 0;
        breakSuccesses = 0;
        breakFailures = 0;
        recoveries = 0;
        wasKicked = false;
        lastPing = -1;
    }

    public void tick() { totalTicks++; }
    public void recordBreakAttempt() { breakAttempts++; }
    public void recordBreakSuccess() { breakSuccesses++; }
    public void recordBreakFailure() { breakFailures++; }
    public void recordRecovery() { recoveries++; }
    public void recordKick() { wasKicked = true; }
    public void updatePing(int ping) { if (ping > 0) lastPing = ping; }

    public String getAccountName() { return accountName; }
    public long getSessionDurationMs() { return System.currentTimeMillis() - sessionStart; }
    public long getTotalTicks() { return totalTicks; }
    public int getBreakAttempts() { return breakAttempts; }
    public int getBreakSuccesses() { return breakSuccesses; }
    public int getBreakFailures() { return breakFailures; }
    public int getRecoveries() { return recoveries; }
    public boolean wasKicked() { return wasKicked; }
    public int getLastPing() { return lastPing; }
    public long getSessionStart() { return sessionStart; }
}