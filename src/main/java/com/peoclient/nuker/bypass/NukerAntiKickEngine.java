package com.peoclient.nuker.bypass;

import com.peoclient.PeoClient;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3965;

import java.util.Random;

/**
 * Engine giúp Nuker tránh kick từ Grim, Vulcan, NoCheatPlus...
 * Không làm giảm tốc độ Nuker, chỉ thêm các cơ chế spoof nhẹ.
 */
public final class NukerAntiKickEngine {
    private static final class_310 mc = class_310.method_1551();
    private static final Random RANDOM = new Random();

    private static boolean enabled = false;
    private static boolean grimMode = true;
    private static boolean vulcanMode = true;
    private static int packetSpoofLevel = 5; // 1-10

    // Rotation smoothing
    private static float lastSmoothedYaw = 0;
    private static float lastSmoothedPitch = 0;
    private static int rotationPhase = 0;

    // Position desync
    private static double lastDesyncX = 0;
    private static double lastDesyncZ = 0;

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public static void setGrimMode(boolean on) {
        grimMode = on;
    }

    public static void setVulcanMode(boolean on) {
        vulcanMode = on;
    }

    public static void setPacketSpoofLevel(int level) {
        packetSpoofLevel = Math.max(1, Math.min(10, level));
    }

    /**
     * Gọi mỗi tick để thực hiện các cơ chế spoof.
     * Nên được gọi từ NukerBypassEngine.tick()
     */
    public static void tick() {
        if (!enabled || mc.field_1724 == null) return;

        rotationPhase = (rotationPhase + 1) % 5;

        // Spoof rotation mỗi 2 tick
        if (rotationPhase % 2 == 0 && grimMode) {
            spoofRotation();
        }

        // Spoof position mỗi 3 tick
        if (rotationPhase % 3 == 0 && vulcanMode) {
            spoofPosition();
        }
    }

    private static void spoofRotation() {
        if (mc.field_1724 == null) return;
        // Gửi rotation lệch nhẹ so với target để tránh Grim
        float yaw = mc.field_1724.method_36454();
        float pitch = mc.field_1724.method_36455();

        // Thêm noise nhỏ
        float yawNoise = (float)((RANDOM.nextDouble() - 0.5) * 0.02 * packetSpoofLevel / 5.0);
        float pitchNoise = (float)((RANDOM.nextDouble() - 0.5) * 0.02 * packetSpoofLevel / 5.0);

        // Giới hạn bước xoay cho Grim
        float maxStep = grimMode ? 22.5f : 30.0f;
        float currentYaw = mc.field_1724.method_36454();
        float currentPitch = mc.field_1724.method_36455();

        // Nếu đã có target, xoay về target
        class_2338 target = com.peoclient.diagnostic.BreakStateTracker.get().getCurrentTarget();
        if (target != null) {
            class_243 eye = mc.field_1724.method_33571();
            class_243 center = class_243.method_24953(target);
            class_243 diff = center.method_1020(eye);
            double horiz = Math.sqrt(diff.field_1352 * diff.field_1352 + diff.field_1350 * diff.field_1350);
            float targetYaw = (float) Math.toDegrees(Math.atan2(diff.field_1350, diff.field_1352)) - 90.0f;
            float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.field_1351, horiz));

            float yawDiff = class_3532.method_15393(targetYaw - currentYaw);
            float pitchDiff = class_3532.method_15393(targetPitch - currentPitch);

            // Chỉ xoay từ từ, không nhảy đột ngột
            float step = Math.min(maxStep, Math.abs(yawDiff));
            if (Math.abs(yawDiff) > maxStep) {
                float newYaw = currentYaw + Math.signum(yawDiff) * maxStep;
                mc.field_1724.method_36456(newYaw);
            } else {
                mc.field_1724.method_36456(targetYaw);
            }

            float pStep = Math.min(maxStep * 0.7f, Math.abs(pitchDiff));
            if (Math.abs(pitchDiff) > maxStep * 0.7f) {
                float newPitch = currentPitch + Math.signum(pitchDiff) * pStep;
                mc.field_1724.method_36457(newPitch);
            } else {
                mc.field_1724.method_36457(targetPitch);
            }
        }

        // Thêm noise nhẹ vào rotation thực tế
        mc.field_1724.method_36456(mc.field_1724.method_36454() + yawNoise);
        mc.field_1724.method_36457(mc.field_1724.method_36455() + pitchNoise);

        lastSmoothedYaw = mc.field_1724.method_36454();
        lastSmoothedPitch = mc.field_1724.method_36455();
    }

    private static void spoofPosition() {
        if (mc.field_1724 == null) return;
        // Vulcan: position desync nhỏ
        double noise = 0.0001 * (packetSpoofLevel / 5.0);
        double offsetX = Math.sin(System.currentTimeMillis() / 1000.0 + 1.2) * noise;
        double offsetZ = Math.cos(System.currentTimeMillis() / 1000.0 + 0.7) * noise;

        // Không thay đổi vị trí thực tế, chỉ ghi nhận cho diagnostic
        lastDesyncX = offsetX;
        lastDesyncZ = offsetZ;
    }

    public static boolean isEnabled() { return enabled; }
    public static boolean isGrimMode() { return grimMode; }
    public static boolean isVulcanMode() { return vulcanMode; }
    public static int getPacketSpoofLevel() { return packetSpoofLevel; }

    public static float getLastSmoothedYaw() { return lastSmoothedYaw; }
    public static float getLastSmoothedPitch() { return lastSmoothedPitch; }
}