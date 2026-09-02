package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;

public final class NukerRotationSync {
    private static final class_310 mc = class_310.method_1551();
    private static float lastYaw, lastPitch;
    private static long lastSyncTime;

    public static void syncToTarget(class_2338 target) {
        if (mc.field_1724 == null || target == null) return;
        long now = System.currentTimeMillis();
        if (now - lastSyncTime < 20) return; // không sync quá thường xuyên

        class_243 eye = mc.field_1724.method_33571();
        class_243 center = class_243.method_24953(target);
        class_243 diff = center.method_1020(eye);
        double horiz = Math.sqrt(diff.field_1352 * diff.field_1352 + diff.field_1350 * diff.field_1350);
        float targetYaw = (float) Math.toDegrees(Math.atan2(diff.field_1350, diff.field_1352)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.field_1351, horiz));

        // Giới hạn xoay mượt để tránh rotation desync
        float currentYaw = mc.field_1724.method_36454();
        float currentPitch = mc.field_1724.method_36455();
        float maxStep = 30.0f; // giống giới hạn vanilla
        float newYaw = currentYaw + class_3532.method_15340(targetYaw - currentYaw, -maxStep, maxStep);
        float newPitch = currentPitch + class_3532.method_15340(targetPitch - currentPitch, -maxStep * 0.7f, maxStep * 0.7f);

        mc.field_1724.method_36456(newYaw);
        mc.field_1724.method_36457(class_3532.method_15363(newPitch, -90, 90));
        lastYaw = newYaw;
        lastPitch = newPitch;
        lastSyncTime = now;
    }
}