package com.peoclient.modules;

/**
 * AntiPeo: local safety/compatibility switch.
 *
 * This module does not attempt to bypass or evade server anti-cheat.
 * It only makes selected automated client actions more conservative by
 * enforcing a small client-side delay.
 */
public final class AntiPeoModule {
    private static boolean enabled;
    private static int actionDelayTicks = 2;

    private AntiPeoModule() {}

    public static void toggle() { enabled = !enabled; }
    public static boolean isEnabled() { return enabled; }

    public static int getActionDelayTicks() {
        return enabled ? Math.max(2, actionDelayTicks) : 0;
    }

    public static void setActionDelayTicks(int ticks) {
        actionDelayTicks = Math.max(2, Math.min(20, ticks));
    }

    public static String getStatus() {
        return enabled ? "SAFE / CONSERVATIVE" : "OFF";
    }
}
