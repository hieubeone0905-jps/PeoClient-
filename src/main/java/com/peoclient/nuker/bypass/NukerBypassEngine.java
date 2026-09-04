package com.peoclient.nuker.bypass;
import com.peoclient.nuker.bypass.NukerAntiKickEngine;

import com.peoclient.diagnostic.BreakFailureReason;
import com.peoclient.diagnostic.DiagnosticEvent;
import com.peoclient.diagnostic.DiagnosticTimeline;
import com.peoclient.diagnostic.DiagnosticUtil;
import com.peoclient.diagnostic.LatencyMetrics;
import com.peoclient.nuker.compat.FailureClassifier;
import com.peoclient.nuker.compat.PerformanceMonitor;
import com.peoclient.nuker.compat.RecoveryManager;
import com.peoclient.nuker.compat.ServerCompatibilityProfile;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_310;

/**
 * AntiVipProMax compatibility/diagnostic engine.
 *
 * Design guarantee: this class never changes Nuker range, multi count, speed,
 * cooldown, target selection or rotation settings and never sends synthetic
 * movement/block-breaking packets. NukerLogic remains the sole owner of the
 * normal Minecraft block-breaking interaction state.
 */
public final class NukerBypassEngine {
    private static final class_310 mc = class_310.method_1551();

    private static boolean enabled;
    private static boolean grimMode = true;
    private static boolean vulcanMode = true;
    private static int intensity = 5;
    private static boolean autoRecovery = true;

    private static final RecoveryManager recoveryManager = RecoveryManager.get();
    private static final PerformanceMonitor performanceMonitor = PerformanceMonitor.get();
    private static final ServerCompatibilityProfile serverProfile = ServerCompatibilityProfile.get();

    private static long ticks;
    private static long nukerTicks;
    private static long activeTicks;
    private static long stagnantTicks;
    private static long lastTickNanos;
    private static double avgTickMs = 50.0;
    private static int suspicionLevel;
    private static long lastAttemptAt;

    private NukerBypassEngine() {}

    public static void setEnabled(boolean enable) {
    if (enabled == enable) return;
    enabled = enable;
    NukerAntiKickEngine.setEnabled(enable);
    if (!enable) resetMetrics();
}

    public static void setIntensity(int level) {
    intensity = Math.max(1, Math.min(10, level));
    NukerAntiKickEngine.setPacketSpoofLevel(intensity);
}
    public static void setGrimMode(boolean on) {
    grimMode = on;
    NukerAntiKickEngine.setGrimMode(on);
}
    public static void setVulcanMode(boolean on) {
    vulcanMode = on;
    NukerAntiKickEngine.setVulcanMode(on);
}
    public static void setAutoRecovery(boolean on) { autoRecovery = on; }

    public static void tick() {
        if (!enabled) return;
        NukerAntiKickEngine.tick();
        long now = System.nanoTime();
        if (lastTickNanos != 0L) {
            double dt = (now - lastTickNanos) / 1_000_000.0;
            if (dt > 0 && dt < 1000) avgTickMs = avgTickMs * 0.90 + dt * 0.10;
        }
        lastTickNanos = now;
        ticks++;

        if ((ticks & 9L) == 0L) LatencyMetrics.get().updatePing();
        String server = getServerAddress();
        int ping = LatencyMetrics.get().getLastPing();
        if (server != null && ping >= 0) serverProfile.recordPing(server, ping);

        int score = 0;
        if (avgTickMs > 75.0) score += 2;
        else if (avgTickMs > 60.0) score += 1;
        if (stagnantTicks > 12) score += 2;
        else if (stagnantTicks > 6) score += 1;
        suspicionLevel = Math.max(0, Math.min(10, score));
    }

    /** Read-only observation of the existing Nuker break state. */
    public static void onNukerTick(boolean active, boolean stagnant) {
        if (!enabled) return;
        nukerTicks++;
        if (active) activeTicks++;
        if (stagnant) stagnantTicks++;
        else if (stagnantTicks > 0) stagnantTicks--;
    }

    public static void onTargetSelected(class_2338 target) {
        if (!enabled || target == null) return;
        recoveryManager.onTargetChanged(target);
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.TARGET, "SELECTED", target.toString());
    }

    public static void onBreakAttempt(class_2338 target, class_2350 side) {
        if (!enabled || target == null) return;
        performanceMonitor.recordAttemptInterval();
        lastAttemptAt = System.currentTimeMillis();
        recoveryManager.onBreakAttempt(target);
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.BREAK, "ATTEMPT",
                target + " side=" + side);
    }

    public static void onBreakProgress(float progress, class_2338 target) {
        if (!enabled || target == null) return;
        boolean air = mc.field_1687 != null && mc.field_1687.method_8320(target).method_26215();
        recoveryManager.onProgress(mc, target, progress, air);
    }

    public static void onBreakSuccess(class_2338 target, long durationMs) {
        if (!enabled || target == null) return;
        performanceMonitor.recordBreakDuration(durationMs);
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.BREAK, "SUCCESS",
                target + " duration=" + durationMs + "ms");
        String server = getServerAddress();
        if (server != null) serverProfile.recordBreakAttempt(server, true, durationMs);
        recoveryManager.onProgress(mc, target, 1.0f, true);
        lastAttemptAt = 0L;
    }

    public static void onBreakFailure(class_2338 target, BreakFailureReason reason) {
        if (!enabled || target == null) return;
        BreakFailureReason classified = reason == null
                ? FailureClassifier.classify(mc, target, null, 0.0f, true, false)
                : reason;
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.BREAK, "FAILURE",
                target + " reason=" + classified.name());
        String server = getServerAddress();
        if (server != null) serverProfile.recordBreakAttempt(server, false, 0);
        recoveryManager.onFailure(target, classified);
        if (autoRecovery) {
            DiagnosticTimeline.get().record(DiagnosticEvent.Category.NUKER, "AUTO_RECOVERY_READY",
                    "NukerLogic owns the actual recovery path");
        }
        lastAttemptAt = 0L;
    }

    /** Called after NukerLogic has already performed its own vanilla-safe recovery. */
    public static void onRecovery() {
        if (!enabled) return;
        stagnantTicks = 0;
        class_2338 target = recoveryManager.getCurrentTarget();
        recoveryManager.onRecovery(target, "NukerLogic stale-state recovery");
        String server = getServerAddress();
        if (server != null) serverProfile.recordRecovery(server);
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.NUKER, "RECOVERY", "Recovery completed by NukerLogic");
    }

    public static boolean isEnabled() { return enabled; }
    public static boolean isAutoRecovery() { return autoRecovery; }
    public static boolean isGrimMode() { return grimMode; }
    public static boolean isVulcanMode() { return vulcanMode; }
    public static int getIntensity() { return intensity; }
    public static int getSuspicionLevel() { return suspicionLevel; }
    public static long getTicks() { return ticks; }
    public static long getNukerTicks() { return nukerTicks; }
    public static long getActiveTicks() { return activeTicks; }
    public static double getAvgTickMs() { return avgTickMs; }
    public static long getLastAttemptAt() { return lastAttemptAt; }
    public static RecoveryManager getRecoveryManager() { return recoveryManager; }
    public static PerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
    public static ServerCompatibilityProfile getServerProfile() { return serverProfile; }

    public static String getCompatibilityStatus() {
        if (!enabled) return "OFF";
        return String.format("TICK G:%s V:%s I:%d S:%d/10 %.1fms R:%s",
                grimMode ? "ON" : "OFF", vulcanMode ? "ON" : "OFF", intensity,
                suspicionLevel, avgTickMs, autoRecovery ? "ON" : "OFF");
    }

    public static void reset() {
        resetMetrics();
        recoveryManager.reset();
        performanceMonitor.clear();
    }

    private static String getServerAddress() {
        String address = DiagnosticUtil.serverAddress(mc);
        return "UNKNOWN".equals(address) ? null : address;
    }

    private static void resetMetrics() {
        ticks = 0;
        nukerTicks = 0;
        activeTicks = 0;
        stagnantTicks = 0;
        lastTickNanos = 0L;
        avgTickMs = 50.0;
        suspicionLevel = 0;
        lastAttemptAt = 0L;
    }
}
