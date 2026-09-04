package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2338;
import net.minecraft.class_310;
import java.util.Random;

/** Compatibility facade. NukerLogic remains the sole block-break owner. */
public final class NukerBypassUltimateV2 {
    private static final class_310 mc = class_310.method_1551();
    private static boolean requested;
    private static boolean grimMode = true, vulcanMode = true, nocheatplusMode = true;
    private static int intensity = 5, desyncLevel = 4;
    private static final Random RANDOM = new Random();
    private static class_2338 target;

    private NukerBypassUltimateV2() {}

    public static void start(class_2338 newTarget) {
        target = newTarget == null ? PeoClient.NukerLogic.getCurrentTarget() : newTarget.method_10062();
        requested = target != null;
        if (requested) DiagnosticRecorder.get().record("NukerCompatibility", "V2 facade active: " + target);
    }

    public static void stop() { requested = false; target = null; }
    public static boolean isActive() { return requested && PeoClient.CFG.nuker && mc.field_1724 != null; }
    public static void tick() {
        if (!isActive()) { target = null; return; }
        class_2338 current = PeoClient.NukerLogic.getCurrentTarget();
        if (current != null) target = current.method_10062();
    }
    public static void setEnabled(boolean enable) { requested = enable && requested; if (!enable) stop(); }
    public static void setGrimMode(boolean enable) { grimMode = enable; }
    public static void setVulcanMode(boolean enable) { vulcanMode = enable; }
    public static void setNoCheatPlusMode(boolean enable) { nocheatplusMode = enable; }
    public static void setIntensity(int level) { intensity = Math.max(1, Math.min(10, level)); }
    public static void setDesyncLevel(int level) { desyncLevel = Math.max(1, Math.min(5, level)); }

    /** Dynamic local pacing value used by compatibility/UI code. */
    private static long calculateDynamicDelay() {
        long base = 12L + RANDOM.nextInt(20);
        if (intensity > 7) base += 3L;
        if (desyncLevel > 3) base += 2L;
        base += (long) (RANDOM.nextGaussian() * 2.0);
        return Math.max(5L, base);
    }

    public static boolean isGrimMode() { return grimMode; }
    public static boolean isVulcanMode() { return vulcanMode; }
    public static boolean isNoCheatPlusMode() { return nocheatplusMode; }
    public static int getIntensity() { return intensity; }
    public static int getDesyncLevel() { return desyncLevel; }
    public static class_2338 getCurrentTarget() { return target != null ? target : PeoClient.NukerLogic.getCurrentTarget(); }
    public static int getPacketCount() { return 0; }
    public static String getStatus() { return isActive() ? "COMPAT" : "OFF"; }
}
