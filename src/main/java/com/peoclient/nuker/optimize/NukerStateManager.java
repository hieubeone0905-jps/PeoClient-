package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_310;

public final class NukerStateManager {
    private static final class_310 mc = class_310.method_1551();
    private static class_2338 currentTarget;
    private static class_2350 currentSide;
    private static long stateStartTime;
    private static int staleTicks;

    public static void setBreakingState(class_2338 target, class_2350 side) {
        if (target != null && !target.equals(currentTarget)) {
            resetState();
        }
        currentTarget = target;
        currentSide = side;
        stateStartTime = System.currentTimeMillis();
        staleTicks = 0;
    }

    public static void updateStaleState() {
        if (currentTarget == null) return;
        if (mc.field_1687 == null || mc.field_1687.method_8320(currentTarget).method_26215()) {
            resetState();
            return;
        }
        // Kiểm tra progress stuck
        float progress = PeoClient.NukerLogic.getBreakingProgress();
        if (progress < 0.01f && System.currentTimeMillis() - stateStartTime > 3000) {
            staleTicks++;
            if (staleTicks > 10) {
                resetState();
                PeoClient.NukerLogic.resetState();
            }
        } else {
            staleTicks = 0;
        }
    }

    public static void resetState() {
        currentTarget = null;
        currentSide = null;
        stateStartTime = 0;
        staleTicks = 0;
    }

    public static class_2338 getCurrentTarget() { return currentTarget; }
    public static class_2350 getCurrentSide() { return currentSide; }
}