package com.peoclient.diagnostic;

import net.minecraft.class_310;

public final class LatencyMetrics {
    private static final LatencyMetrics instance = new LatencyMetrics();
    private volatile int lastPing = -1;
    private volatile long lastPingTime = 0;
    private volatile int minPing = Integer.MAX_VALUE;
    private volatile int maxPing = 0;
    private volatile long totalPingSum = 0;
    private volatile int pingCount = 0;

    private LatencyMetrics() {}

    public static LatencyMetrics get() { return instance; }

    public void updatePing() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1724.field_6214 == null) return;
        int ping = mc.field_1724.field_6214.method_11028();
        if (ping > 0) {
            lastPing = ping;
            AccountSessionMetrics.get().updatePing(ping);
            lastPingTime = System.currentTimeMillis();
            if (ping < minPing) minPing = ping;
            if (ping > maxPing) maxPing = ping;
            totalPingSum += ping;
            pingCount++;
        }
    }

    public int getLastPing() { return lastPing; }
    public long getLastPingTime() { return lastPingTime; }
    public int getMinPing() { return minPing == Integer.MAX_VALUE ? 0 : minPing; }
    public int getMaxPing() { return maxPing; }
    public double getAveragePing() { return pingCount == 0 ? 0 : (double)totalPingSum / pingCount; }
    public int getPingCount() { return pingCount; }
}