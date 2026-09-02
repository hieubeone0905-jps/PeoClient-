package com.peoclient.modules;

import com.peoclient.PeoClient;
import com.peoclient.nuker.bypass.NukerBypassEngine;

/**
 * AntiVipProMax – Bypass Nuker chống Grim/Vulcan.
 * Không làm thay đổi logic Nuker gốc, chỉ thêm bypass song song.
 */
public final class AntiVipProMaxModule {
    private AntiVipProMaxModule() {}

    public static void toggle() {
        PeoClient.CFG.antiVipProMax = !PeoClient.CFG.antiVipProMax;
        updateEngine();
        PeoClient.CFG.save();
    }

    public static boolean isEnabled() { return PeoClient.CFG.antiVipProMax; }

    public static void setGrimMode(boolean value) {
        if (PeoClient.CFG.antiVipProMaxGrim == value) return;
        PeoClient.CFG.antiVipProMaxGrim = value;
        NukerBypassEngine.setGrimMode(value);
        PeoClient.CFG.save();
    }

    public static void setVulcanMode(boolean value) {
        if (PeoClient.CFG.antiVipProMaxVulcan == value) return;
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

    public static boolean isGrimMode() { return PeoClient.CFG.antiVipProMaxGrim; }
    public static boolean isVulcanMode() { return PeoClient.CFG.antiVipProMaxVulcan; }
    public static int getIntensity() { return PeoClient.CFG.antiVipProMaxIntensity; }
    public static boolean isAutoAdjust() { return PeoClient.CFG.antiVipProMaxAutoAdjust; }

    public static int getSuspicionLevel() {
        return NukerBypassEngine.getSuspicionLevel();
    }

    public static String getStatus() {
        if (!isEnabled()) return "OFF";
        return String.format("G:%s V:%s I:%d S:%d/10",
                isGrimMode() ? "ON" : "OFF",
                isVulcanMode() ? "ON" : "OFF",
                getIntensity(),
                getSuspicionLevel());
    }

    private static void updateEngine() {
        boolean enable = isEnabled();
        NukerBypassEngine.setEnabled(enable);
        if (enable) {
            NukerBypassEngine.setGrimMode(isGrimMode());
            NukerBypassEngine.setVulcanMode(isVulcanMode());
            NukerBypassEngine.setIntensity(getIntensity());
        }
    }

    public static void tick() {
        // Khởi động engine nếu chưa chạy và được bật
        if (isEnabled() && !NukerBypassEngine.isEnabled()) {
            updateEngine();
        }
        // Tự động điều chỉnh intensity nếu bật auto adjust
        if (isEnabled() && isAutoAdjust()) {
            int susp = getSuspicionLevel();
            if (susp > 6) setIntensity(8);
            else if (susp > 3) setIntensity(6);
            else setIntensity(5);
        }
    }
}