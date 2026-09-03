package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import com.peoclient.diagnostic.LatencyMetrics;
import com.peoclient.diagnostic.ServerResponseMonitor;
import net.minecraft.class_310;

/**
 * Local compatibility monitor.
 *
 * It no longer mutates player rotation, inserts random pauses, changes break
 * timing, or fabricates packets.  Those behaviours could desynchronise the
 * vanilla interaction manager and were a major source of ghost blocks.
 */
public final class AntiKickEngine {
    private static boolean active;
    private static boolean enabled = true;
    private static int protectionLevel = 7;
    private static boolean useHealthCheck = true;
    private static boolean usePingMonitoring = true;
    private static boolean paused;
    private static int lowHealthTicks;
    private static int pingSpikes;
    private static int suspiciousResponses;

    private AntiKickEngine() {}

    public static void start() {
        active = true;
        reset();
        DiagnosticRecorder.get().record("AntiKickEngine", "Started (monitor-only)");
    }

    public static void stop() {
        active = false;
        reset();
        DiagnosticRecorder.get().record("AntiKickEngine", "Stopped");
    }

    public static boolean isActive() { return enabled && active && PeoClient.CFG.nuker; }

    public static void tick(class_310 mc) {
        if (!isActive() || mc.field_1724 == null) return;

        if (useHealthCheck) {
            float health = mc.field_1724.method_6032();
            int food = mc.field_1724.method_7344().method_7586();
            if (health < 4.0f || food < 4) {
                lowHealthTicks++;
                paused = lowHealthTicks >= 2;
            } else {
                lowHealthTicks = 0;
                paused = false;
            }
        }

        if (usePingMonitoring) {
            int ping = LatencyMetrics.get().getLastPing();
            int avg = (int) LatencyMetrics.get().getAveragePing();
            if (ping > 0 && avg > 0 && ping > avg * 2) pingSpikes++;
            else if (pingSpikes > 0) pingSpikes--;
        }

        int suspicious = 0;
        for (var response : ServerResponseMonitor.get().getResponses()) {
            if (response != null && "POSITION_CORRECTION".equals(response.type)) suspicious++;
        }
        if (suspicious > 0) suspiciousResponses = Math.min(100, suspiciousResponses + suspicious);
        else if (suspiciousResponses > 0) suspiciousResponses--;
    }

    public static boolean shouldPause() {
        // Keep this API for existing callers.  Only genuine local safety state
        // may pause; no anti-cheat-driven random micro-pauses are used.
        return paused;
    }

    public static int getDynamicCooldown() { return PeoClient.CFG.nukerCooldown; }
    public static int getProtectionLevel() { return protectionLevel; }
    public static void setProtectionLevel(int level) { protectionLevel = Math.max(1, Math.min(10, level)); }
    public static void setEnabled(boolean enable) { enabled = enable; if (!enable) stop(); }
    public static void setRotationRandomization(boolean enable) { /* retained for config compatibility */ }
    public static void setDynamicInterval(boolean enable) { /* retained for config compatibility */ }
    public static void setHealthCheck(boolean enable) { useHealthCheck = enable; }
    public static void setPingMonitoring(boolean enable) { usePingMonitoring = enable; }
    public static void setMicroPause(boolean enable) { /* retained for config compatibility */ }
    public static boolean isPaused() { return paused; }

    public static void setGrimMode(boolean enable) { /* compatibility flag only */ }
    public static void setVulcanMode(boolean enable) { /* compatibility flag only */ }
    public static void setPacketSpoofLevel(int level) { protectionLevel = Math.max(1, Math.min(10, level)); }

    public static boolean isEnabled() { return enabled; }

    public static String getStatus() {
        if (!isActive()) return "OFF";
        return String.format("P:%s L:%d H:%s Ping:%d Spikes:%d Resp:%d",
                paused ? "PAUSED" : "RUNNING", protectionLevel,
                useHealthCheck ? "ON" : "OFF", LatencyMetrics.get().getLastPing(),
                pingSpikes, suspiciousResponses);
    }

    private static void reset() {
        paused = false;
        lowHealthTicks = 0;
        pingSpikes = 0;
        suspiciousResponses = 0;
    }
}
