package com.peoclient.nuker.bypass;

import com.peoclient.PeoClient;
import net.minecraft.class_310;

/**
 * AntiVipProMax compatibility controller.
 *
 * Important design rule: this controller never creates a background thread and
 * never injects synthetic movement/block-break packets. NukerLogic remains the
 * sole owner of the normal Minecraft block-breaking interaction state.
 *
 * The goal is to avoid two independent packet producers fighting each other,
 * while preserving Nuker's configured range/multi/cooldown/rotation behaviour.
 */
public final class NukerBypassEngine {
    private static final class_310 mc = class_310.method_1551();

    private static boolean enabled;
    private static boolean grimMode = true;
    private static boolean vulcanMode = true;
    private static int intensity = 5;

    private static long ticks;
    private static long nukerTicks;
    private static long activeTicks;
    private static long stagnantTicks;
    private static long lastTickNanos;
    private static double avgTickMs = 50.0;
    private static int suspicionLevel;

    private NukerBypassEngine() {}

    public static void setEnabled(boolean enable) {
        if (enabled == enable) return;
        enabled = enable;
        if (!enable) resetMetrics();
    }

    public static void setIntensity(int level) {
        intensity = Math.max(1, Math.min(10, level));
    }

    public static void setGrimMode(boolean on) {
        grimMode = on;
    }

    public static void setVulcanMode(boolean on) {
        vulcanMode = on;
    }

    /** Called once from the normal client tick. */
    public static void tick() {
        if (!enabled) return;
        long now = System.nanoTime();
        if (lastTickNanos != 0L) {
            double dt = (now - lastTickNanos) / 1_000_000.0;
            if (dt > 0 && dt < 1000) avgTickMs = avgTickMs * 0.90 + dt * 0.10;
        }
        lastTickNanos = now;
        ticks++;

        // This is deliberately conservative: it is a local timing diagnostic,
        // not a claim that we can read a server's anti-cheat suspicion value.
        int score = 0;
        if (avgTickMs > 75.0) score += 2;
        else if (avgTickMs > 60.0) score += 1;
        if (stagnantTicks > 12) score += 2;
        else if (stagnantTicks > 6) score += 1;
        suspicionLevel = Math.max(0, Math.min(10, score));
    }

    /** Called by NukerLogic while it owns the actual break state. */
    public static void onNukerTick(boolean active, boolean stagnant) {
        if (!enabled) return;
        nukerTicks++;
        if (active) activeTicks++;
        if (stagnant) stagnantTicks++;
        else if (stagnantTicks > 0) stagnantTicks--;
    }

    /** Compatibility hook kept for callers from older builds. */
    public static void reset() {
        resetMetrics();
    }

    private static void resetMetrics() {
        ticks = 0;
        nukerTicks = 0;
        activeTicks = 0;
        stagnantTicks = 0;
        lastTickNanos = 0L;
        avgTickMs = 50.0;
        suspicionLevel = 0;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getSuspicionLevel() {
        return suspicionLevel;
    }

    public static long getTicks() { return ticks; }
    public static long getNukerTicks() { return nukerTicks; }
    public static long getActiveTicks() { return activeTicks; }
    public static double getAvgTickMs() { return avgTickMs; }

    public static String getCompatibilityStatus() {
        if (!enabled) return "OFF";
        return String.format("TICK G:%s V:%s I:%d S:%d/10 %.1fms",
                grimMode ? "ON" : "OFF",
                vulcanMode ? "ON" : "OFF",
                intensity,
                suspicionLevel,
                avgTickMs);
    }
}
