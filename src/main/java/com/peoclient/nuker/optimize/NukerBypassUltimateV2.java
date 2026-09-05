package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.nuker.bypass.BypassPacketManager;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_310;
import java.util.Random;

public final class NukerBypassUltimateV2 {
    private static final class_310 mc = class_310.method_1551();
    private static boolean requested;
    private static boolean grimMode = true, vulcanMode = true, nocheatplusMode = true;
    private static int intensity = 7, desyncLevel = 4;
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

        if (target != null && mc.field_1724 != null) {
            // Rotation giả mỗi tick
            float yaw = mc.field_1724.method_36454() + (float)(RANDOM.nextGaussian() * 0.6);
            float pitch = mc.field_1724.method_36455() + (float)(RANDOM.nextGaussian() * 0.3);
            boolean onGround = mc.field_1724.method_24828();
            BypassPacketManager.sendRotation(yaw, pitch, onGround);

            // Position giả 2 lần mỗi 5 tick
            if (RANDOM.nextInt(5) < 2) {
                class_243 pos = mc.field_1724.method_19538();
                BypassPacketManager.sendPosition(
                    pos.field_1352 + RANDOM.nextDouble() * 0.003 - 0.0015,
                    pos.field_1351 + RANDOM.nextDouble() * 0.003 - 0.0015,
                    pos.field_1350 + RANDOM.nextDouble() * 0.003 - 0.0015,
                    onGround
                );
            }

            // Abort block action ngẫu nhiên
            if (RANDOM.nextInt(7) == 0) {
                BypassPacketManager.sendBlockAction(target, class_2350.field_11036);
            }
        }
    }

    public static void setEnabled(boolean enable) { requested = enable && requested; if (!enable) stop(); }
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
    public static class_2338 getCurrentTarget() { return target != null ? target : PeoClient.NukerLogic.getCurrentTarget(); }
    public static int getPacketCount() { return 0; }
    public static String getStatus() { return isActive() ? "COMPAT" : "OFF"; }
}