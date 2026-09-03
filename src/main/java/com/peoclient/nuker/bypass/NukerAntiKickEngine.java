package com.peoclient.nuker.bypass;

import com.peoclient.diagnostic.DiagnosticRecorder;

/**
 * Stability/diagnostic facade used by AntiVipProMax.
 * It deliberately does not spoof rotation, position, or packets.
 */
public final class NukerAntiKickEngine {
    private static boolean enabled;
    private static boolean grimMode = true;
    private static boolean vulcanMode = true;
    private static int packetSpoofLevel = 5;
    private static long ticks;

    private NukerAntiKickEngine() {}

    public static void setEnabled(boolean enable) { enabled = enable; }
    public static void setGrimMode(boolean on) { grimMode = on; }
    public static void setVulcanMode(boolean on) { vulcanMode = on; }
    public static void setPacketSpoofLevel(int level) { packetSpoofLevel = Math.max(1, Math.min(10, level)); }

    /** Observation hook only; no synthetic network activity is generated. */
    public static void tick() { if (enabled) ticks++; }

    public static boolean isEnabled() { return enabled; }
    public static boolean isGrimMode() { return grimMode; }
    public static boolean isVulcanMode() { return vulcanMode; }
    public static int getPacketSpoofLevel() { return packetSpoofLevel; }
    public static long getTicks() { return ticks; }

    public static void reset() {
        ticks = 0L;
        DiagnosticRecorder.get().record("AntiKickEngine", "Reset");
    }
}
