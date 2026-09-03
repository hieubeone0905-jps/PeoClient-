package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2338;
import net.minecraft.class_310;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Nuker compatibility coordinator.
 *
 * Important: NukerLogic is the single owner of block-breaking and outgoing
 * interaction packets.  This class deliberately does not create synthetic
 * movement/break packets or a second breaking state.  Its purpose is to keep
 * the existing UI/configuration API while tracking the active target and
 * detecting target completion.
 */
public final class NukerBypassUltimateV2 {
    private static final class_310 mc = class_310.method_1551();
    private static final AtomicBoolean active = new AtomicBoolean(false);

    private static volatile class_2338 currentTarget;
    private static volatile int tickCounter;
    private static volatile int packetCounter;

    private static boolean grimMode = true;
    private static boolean vulcanMode = true;
    private static boolean nocheatplusMode = true;
    private static int intensity = 7;
    private static int desyncLevel = 3;

    private NukerBypassUltimateV2() {}

    public static void start(class_2338 target) {
        if (mc.field_1724 == null || mc.field_1687 == null || target == null) return;
        currentTarget = target.method_10062();
        tickCounter = 0;
        packetCounter = 0;
        active.set(true);
        DiagnosticRecorder.get().record("NukerBypassV2", "Compatibility coordinator started on " + target);
    }

    public static void stop() {
        active.set(false);
        currentTarget = null;
        tickCounter = 0;
        packetCounter = 0;
        DiagnosticRecorder.get().record("NukerBypassV2", "Compatibility coordinator stopped");
    }

    public static boolean isActive() {
        return active.get() && mc.field_1724 != null && currentTarget != null;
    }

    public static void tick() {
        if (!isActive() || mc.field_1687 == null) return;
        tickCounter++;
        if (mc.field_1687.method_8320(currentTarget).method_26215()) {
            stop();
        }
    }

    public static void setEnabled(boolean enable) {
        if (!enable) {
            stop();
            return;
        }
        if (!isActive()) {
            class_2338 target = PeoClient.NukerLogic.getCurrentTarget();
            if (target != null) start(target);
        }
    }

    public static void setGrimMode(boolean enable) { grimMode = enable; }
    public static void setVulcanMode(boolean enable) { vulcanMode = enable; }
    public static void setNoCheatPlusMode(boolean enable) { nocheatplusMode = enable; }
    public static void setIntensity(int level) { intensity = Math.max(1, Math.min(10, level)); }
    public static void setDesyncLevel(int level) { desyncLevel = Math.max(1, Math.min(5, level)); }

    public static boolean isGrimMode() { return grimMode; }
    public static boolean isVulcanMode() { return vulcanMode; }
    public static boolean isNoCheatPlusMode() { return nocheatplusMode; }
    public static int getIntensity() { return intensity; }
    public static int getDesyncLevel() { return desyncLevel; }
    public static class_2338 getCurrentTarget() { return currentTarget; }
    public static int getPacketCount() { return packetCounter; }

    public static String getStatus() {
        if (!isActive()) return "OFF";
        return String.format("G:%s V:%s N:%s I:%d D:%d T:%d",
                grimMode ? "ON" : "OFF",
                vulcanMode ? "ON" : "OFF",
                nocheatplusMode ? "ON" : "OFF",
                intensity, desyncLevel, tickCounter);
    }
}
