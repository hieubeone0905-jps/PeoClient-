package com.peoclient.modules;

import com.peoclient.PeoClient;

/**
 * AntiVipProMax settings/compatibility module.
 *
 * It leaves PeoClient.NukerLogic and every existing Nuker setting untouched.
 */
public final class AntiVipProMaxModule {
    private AntiVipProMaxModule() {}

    public static void toggle() {
        PeoClient.CFG.antiVipProMax = !PeoClient.CFG.antiVipProMax;
        PeoClient.CFG.save();
    }

    public static boolean isEnabled() { return PeoClient.CFG.antiVipProMax; }

    public static void setGrimMode(boolean value) {
        PeoClient.CFG.antiVipProMaxGrim = value;
        PeoClient.CFG.save();
    }

    public static void setVulcanMode(boolean value) {
        PeoClient.CFG.antiVipProMaxVulcan = value;
        PeoClient.CFG.save();
    }

    public static void setIntensity(int value) {
        PeoClient.CFG.antiVipProMaxIntensity = Math.max(1, Math.min(10, value));
        PeoClient.CFG.save();
    }

    public static void setAutoAdjust(boolean value) {
        PeoClient.CFG.antiVipProMaxAutoAdjust = value;
        PeoClient.CFG.save();
    }

    public static boolean isGrimMode() { return PeoClient.CFG.antiVipProMaxGrim; }
    public static boolean isVulcanMode() { return PeoClient.CFG.antiVipProMaxVulcan; }
    public static int getIntensity() { return PeoClient.CFG.antiVipProMaxIntensity; }
    public static boolean isAutoAdjust() { return PeoClient.CFG.antiVipProMaxAutoAdjust; }

    public static int getSuspicionLevel() { return 0; }

    public static String getStatus() {
        if (!isEnabled()) return "OFF";
        return String.format("G:%s V:%s I:%d S:0/10",
                isGrimMode() ? "ON" : "OFF",
                isVulcanMode() ? "ON" : "OFF",
                getIntensity());
    }

    public static void tick() {
        // Intentionally does not alter Nuker timing, target selection, movement,
        // or packet behaviour. Existing NukerCompatibility remains in control.
    }
}
