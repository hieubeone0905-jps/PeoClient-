package com.peoclient.nuker.spoof;

import com.peoclient.PeoClient;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;

public final class NukerRotationSpoofer {
    private static final class_310 mc = class_310.method_1551();
    private static float targetYaw, targetPitch;
    private static float lastYaw, lastPitch;
    private static long lastSyncTime;
    private static int tickCounter;

    public static void updateTarget(class_2338 target) {
        if (mc.field_1724 == null || target == null) return;
        class_243 eye = mc.field_1724.method_33571();
        class_243 center = class_243.method_24953(target);
        class_243 diff = center.method_1020(eye);
        double horiz = Math.sqrt(diff.field_1352 * diff.field_1352 + diff.field_1350 * diff.field_1350);
        targetYaw = (float) Math.toDegrees(Math.atan2(diff.field_1350, diff.field_1352)) - 90.0f;
        targetPitch = (float) -Math.toDegrees(Math.atan2(diff.field_1351, horiz));
    }

    public static void spoofRotation() {
        if (mc.field_1724 == null) return;
        float currentYaw = mc.field_1724.method_36454();
        float currentPitch = mc.field_1724.method_36455();

        // Tính delta
        float yawDiff = class_3532.method_15393(targetYaw - currentYaw);
        float pitchDiff = class_3532.method_15393(targetPitch - currentPitch);

        // Tạo noise để tránh pattern
        float noise = (float)(Math.sin(System.currentTimeMillis() / 500.0) * 0.1);
        float maxStep = 30.0f + noise; // 30 độ mỗi tick, tối đa vanilla

        // Giới hạn step
        float stepYaw = class_3532.method_15340(yawDiff, -maxStep, maxStep);
        float stepPitch = class_3532.method_15340(pitchDiff, -maxStep * 0.7f, maxStep * 0.7f);

        float newYaw = currentYaw + stepYaw;
        float newPitch = currentPitch + stepPitch;

        // Thêm micro-noise để tránh smooth pattern
        newYaw += (float)((Math.random() - 0.5) * 0.02);
        newPitch += (float)((Math.random() - 0.5) * 0.02);

        // Cập nhật rotation
        mc.field_1724.method_36456(newYaw);
        mc.field_1724.method_36457(class_3532.method_15363(newPitch, -90, 90));

        lastYaw = newYaw;
        lastPitch = newPitch;
        lastSyncTime = System.currentTimeMillis();
    }

    public static void reset() {
        targetYaw = 0; targetPitch = 0;
        lastYaw = 0; lastPitch = 0;
        lastSyncTime = 0;
    }
}