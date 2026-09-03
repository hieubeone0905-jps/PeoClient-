package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import com.peoclient.diagnostic.LatencyMetrics;

/** Safe latency monitor that adjusts local Nuker pacing without packet spoofing. */
public final class LatencyCompensator {
    private static boolean enabled;
    private static int tickCounter;
    private static int dynamicCooldownBase;
    private static int pingSpikeThreshold = 150;
    private static int microPauseInterval = 50;
    private static long lastLogTime;

    private LatencyCompensator() {}

    public static void start() {
        enabled = true;
        tickCounter = 0;
        dynamicCooldownBase = 0;
        lastLogTime = 0;
        DiagnosticRecorder.get().record("LatencyCompensator", "Started (latency-aware pacing)");
    }

    public static void stop() {
        enabled = false;
        tickCounter = 0;
        dynamicCooldownBase = 0;
        DiagnosticRecorder.get().record("LatencyCompensator", "Stopped");
    }

    public static boolean isEnabled() {
        return enabled && PeoClient.CFG.nuker;
    }

    public static void tick() {
        if (!isEnabled()) return;
        tickCounter++;
        int ping = LatencyMetrics.get().getLastPing();
        if (ping < 0) ping = 50;

        dynamicCooldownBase = ping > pingSpikeThreshold ? 2 : 0;

        if (dynamicCooldownBase > 0 && tickCounter % 20 == 0) {
            long now = System.currentTimeMillis();
            if (now - lastLogTime > 1000) {
                lastLogTime = now;
                DiagnosticRecorder.get().record("LatencyCompensator",
                        "High ping=" + ping + "ms; local pacing protection=" + dynamicCooldownBase + " ticks");
            }
        }
    }

    public static int getDynamicCooldown() {
        return dynamicCooldownBase;
    }

    public static void setPingThreshold(int threshold) {
        pingSpikeThreshold = Math.max(50, Math.min(500, threshold));
    }

    public static void setMicroPauseInterval(int interval) {
        microPauseInterval = Math.max(20, Math.min(100, interval));
    }

    public static int getPingThreshold() { return pingSpikeThreshold; }
    public static int getMicroPauseInterval() { return microPauseInterval; }
}
