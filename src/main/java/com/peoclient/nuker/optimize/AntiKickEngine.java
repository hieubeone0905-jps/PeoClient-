package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import com.peoclient.diagnostic.LatencyMetrics;
import net.minecraft.class_310;
import java.util.concurrent.atomic.AtomicBoolean;

/** Monitor-only Nuker stability layer. Never spoofs packets or throttles Nuker. */
public final class AntiKickEngine {
    private static final AtomicBoolean active = new AtomicBoolean(false);
    private static int tick, pingSpikes, corrections;
    private static int protectionLevel = 7;
    private static long lastEvent;
    private static boolean enabled = true;
    private AntiKickEngine() {}
    public static void start() { active.set(true); reset(); DiagnosticRecorder.get().record("AntiKickEngine", "Started (monitor-only)"); }
    public static void stop() { active.set(false); reset(); DiagnosticRecorder.get().record("AntiKickEngine", "Stopped"); }
    public static boolean isActive() { return enabled && active.get() && PeoClient.CFG.nuker; }
    public static void tick(class_310 mc) {
        if (!isActive() || mc == null || mc.field_1724 == null) return;
        tick++;
        if (tick % 5 == 0) monitorPing();
        if (tick % 10 == 0) monitorCorrections();
    }
    private static void monitorPing() {
        int ping = LatencyMetrics.get().getLastPing();
        int avg = (int) LatencyMetrics.get().getAveragePing();
        if (ping > 0 && avg > 0 && ping > avg * 2) {
            pingSpikes++;
            if (System.currentTimeMillis() - lastEvent > 1000) {
                lastEvent = System.currentTimeMillis();
                DiagnosticRecorder.get().record("AntiKickEngine", "Ping spike observed: " + ping + "ms avg=" + avg + ". No Nuker throttle applied.");
            }
        }
    }
    private static void monitorCorrections() {
        try {
            var responses = com.peoclient.diagnostic.ServerResponseMonitor.get().getResponses();
            int count = 0;
            for (var response : responses) if (response != null && "POSITION_CORRECTION".equals(response.type)) count++;
            corrections += count;
        } catch (Throwable ignored) {}
    }
    public static boolean shouldPause() { return false; }
    public static boolean isPaused() { return false; }
    public static int getProtectionLevel() { return protectionLevel; }
    public static int getPingSpikes() { return pingSpikes; }
    public static int getCorrections() { return corrections; }
    public static String getStatus() { return isActive() ? "MONITOR" : "OFF"; }
    public static void reset() { tick = 0; pingSpikes = 0; corrections = 0; lastEvent = 0L; }
}
