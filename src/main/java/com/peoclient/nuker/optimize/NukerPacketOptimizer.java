package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_310;

public final class NukerPacketOptimizer {
    private static final class_310 mc = class_310.method_1551();
    private static long lastStartTime, lastStopTime;
    private static class_2338 lastTarget;

    public static boolean canSendStart(class_2338 target, class_2350 side) {
        if (mc.field_1724 == null) return false;
        long now = System.currentTimeMillis();
        // Không gửi start quá nhanh cho cùng target
        if (target.equals(lastTarget) && (now - lastStartTime) < 50) return false;
        // Kiểm tra block hợp lệ
        if (mc.field_1687.method_8320(target).method_26215()) return false;
        lastTarget = target;
        lastStartTime = now;
        return true;
    }

    public static boolean canSendStop(class_2338 target) {
        if (mc.field_1724 == null) return false;
        long now = System.currentTimeMillis();
        if (target.equals(lastTarget) && (now - lastStopTime) < 50) return false;
        lastStopTime = now;
        return true;
    }

    public static boolean canSendUpdate(class_2338 target) {
        if (mc.field_1724 == null) return false;
        // Chỉ gửi update nếu block vẫn tồn tại
        return !mc.field_1687.method_8320(target).method_26215();
    }
}