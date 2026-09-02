package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.BreakFailureReason;
import com.peoclient.diagnostic.FailureClassifier;
import com.peoclient.nuker.bypass.NukerBypassEngine;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_310;

public final class NukerAntiKick {
    private static final class_310 mc = class_310.method_1551();
    private static boolean enabled = true;

    public static void tick() {
        if (!enabled || !PeoClient.CFG.nuker) return;
        NukerStateManager.updateStaleState();
        // Nếu state bị reset, báo recovery
        if (NukerStateManager.getCurrentTarget() == null) {
            NukerBypassEngine.onRecovery();
        }
    }

    public static boolean shouldBreak(class_2338 target, class_2350 side) {
        if (!enabled) return true;
        if (target == null || mc.field_1687 == null) return false;
        if (mc.field_1687.method_8320(target).method_26215()) return false;
        // Chỉ break nếu target còn hợp lệ và chưa bị stale
        return true;
    }

    public static void onBreakStart(class_2338 target, class_2350 side) {
        if (!enabled) return;
        NukerRotationSync.syncToTarget(target);
        NukerStateManager.setBreakingState(target, side);
    }

    public static void onBreakSuccess(class_2338 target) {
        if (!enabled) return;
        NukerStateManager.resetState();
    }

    public static void onBreakFailure(class_2338 target, boolean interactionFailed, boolean raycastFailed) {
        if (!enabled) return;
        BreakFailureReason reason = FailureClassifier.classify(mc, target, null,
                PeoClient.NukerLogic.getBreakingProgress(), interactionFailed, raycastFailed);
        NukerBypassEngine.onBreakFailure(target, reason);
        NukerStateManager.resetState();
    }
}