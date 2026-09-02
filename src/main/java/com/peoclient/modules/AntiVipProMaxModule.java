package com.peoclient.modules;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.AccountSessionMetrics;
import com.peoclient.diagnostic.DiagnosticRecorder;
import com.peoclient.diagnostic.LatencyMetrics;
import com.peoclient.nuker.bypass.NukerBypassEngine;
import net.minecraft.class_2338;

/** AntiVipProMax compatibility, diagnostics and recovery controller. */
public final class AntiVipProMaxModule {
    private AntiVipProMaxModule() {}

    public static void toggle() {
        PeoClient.CFG.antiVipProMax = !PeoClient.CFG.antiVipProMax;
        updateEngine();
        PeoClient.CFG.save();
        DiagnosticRecorder.get().record("AntiVipProMax", "Toggled to " + PeoClient.CFG.antiVipProMax);
    }

    public static boolean isEnabled() { return PeoClient.CFG.antiVipProMax; }

    public static void setGrimMode(boolean value) {
        PeoClient.CFG.antiVipProMaxGrim = value;
        NukerBypassEngine.setGrimMode(value);
        PeoClient.CFG.save();
    }

    public static void setVulcanMode(boolean value) {
        PeoClient.CFG.antiVipProMaxVulcan = value;
        NukerBypassEngine.setVulcanMode(value);
        PeoClient.CFG.save();
    }

    public static void setIntensity(int value) {
        int clamped = Math.max(1, Math.min(10, value));
        if (PeoClient.CFG.antiVipProMaxIntensity == clamped) return;
        PeoClient.CFG.antiVipProMaxIntensity = clamped;
        NukerBypassEngine.setIntensity(clamped);
        PeoClient.CFG.save();
    }

    public static void setAutoAdjust(boolean value) {
        if (PeoClient.CFG.antiVipProMaxAutoAdjust == value) return;
        PeoClient.CFG.antiVipProMaxAutoAdjust = value;
        PeoClient.CFG.save();
    }

    public static void setAutoRecovery(boolean value) {
        PeoClient.CFG.antiVipProMaxAutoRecovery = value;
        NukerBypassEngine.setAutoRecovery(value);
        PeoClient.CFG.save();
    }

    public static boolean isGrimMode() { return PeoClient.CFG.antiVipProMaxGrim; }
    public static boolean isVulcanMode() { return PeoClient.CFG.antiVipProMaxVulcan; }
    public static int getIntensity() { return PeoClient.CFG.antiVipProMaxIntensity; }
    public static boolean isAutoAdjust() { return PeoClient.CFG.antiVipProMaxAutoAdjust; }
    public static boolean isAutoRecovery() { return PeoClient.CFG.antiVipProMaxAutoRecovery; }
    public static int getSuspicionLevel() { return NukerBypassEngine.getSuspicionLevel(); }

    public static String getStatus() {
        if (!isEnabled()) return "OFF";
        return String.format("G:%s V:%s I:%d S:%d/10 R:%s",
                isGrimMode() ? "ON" : "OFF", isVulcanMode() ? "ON" : "OFF",
                getIntensity(), getSuspicionLevel(), isAutoRecovery() ? "ON" : "OFF");
    }

    public static String getNukerState() {
        if (!isEnabled()) return "Disabled";
        return NukerBypassEngine.getRecoveryManager().getState().name();
    }

    public static String getCurrentTarget() {
        class_2338 target = NukerBypassEngine.getRecoveryManager().getCurrentTarget();
        return target != null ? target.toString() : "None";
    }

    public static float getBreakingProgress() { return PeoClient.NukerLogic.getBreakingProgress(); }
    public static int getStagnantTicks() { return NukerBypassEngine.getRecoveryManager().getStagnantTicks(); }
    public static int getRecoveryAttempts() { return NukerBypassEngine.getRecoveryManager().getRecoveryAttempts(); }
    public static int getBreakAttempts() { return AccountSessionMetrics.get().getBreakAttempts(); }
    public static int getBreakSuccesses() { return AccountSessionMetrics.get().getBreakSuccesses(); }
    public static int getBreakFailures() { return AccountSessionMetrics.get().getBreakFailures(); }
    public static int getRecoveries() { return AccountSessionMetrics.get().getRecoveries(); }
    public static int getPing() { return LatencyMetrics.get().getLastPing(); }
    public static double getAverageTickMs() { return NukerBypassEngine.getAvgTickMs(); }

    private static void updateEngine() {
        boolean enable = isEnabled();
        NukerBypassEngine.setEnabled(enable);
        if (enable) {
            NukerBypassEngine.setGrimMode(isGrimMode());
            NukerBypassEngine.setVulcanMode(isVulcanMode());
            NukerBypassEngine.setIntensity(getIntensity());
            NukerBypassEngine.setAutoRecovery(isAutoRecovery());
        }
    }

    public static void tick() {
        if (isEnabled() && !NukerBypassEngine.isEnabled()) updateEngine();
        if (isEnabled()) NukerBypassEngine.tick();
        // Auto Adjust is observation-only. It never changes the configured
        // intensity and never touches Nuker settings/timing/targets.
        // The intensity value is retained only as a user-selected diagnostic label.
        if (isEnabled() && isAutoAdjust()) {
            int susp = getSuspicionLevel();
            if (susp >= 7) {
                DiagnosticRecorder.get().record("AntiVipProMax",
                        "High diagnostic suspicion=" + susp + " (observation only)");
            }
        }
    }
}
