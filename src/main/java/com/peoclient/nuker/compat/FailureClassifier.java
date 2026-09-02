package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.BreakFailureReason;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2404;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;

/** Classifies a local Nuker failure without changing the break operation. */
public final class FailureClassifier {
    private FailureClassifier() {}

    public static BreakFailureReason classify(class_310 mc, class_2338 target, class_2350 side,
                                               float progress, boolean interactionFailed,
                                               boolean raycastFailed) {
        if (mc == null || mc.field_1724 == null || mc.field_1687 == null) {
            return BreakFailureReason.PLAYER_MOVED;
        }
        if (target == null) return BreakFailureReason.NOT_TARGET;
        if (mc.field_1724.method_33571().method_1022(class_243.method_24953(target))
                > PeoClient.CFG.nukerRange + 0.5) return BreakFailureReason.OUT_OF_RANGE;
        if (raycastFailed) return BreakFailureReason.RAYCAST_FAIL;
        if (interactionFailed) return BreakFailureReason.INTERACTION_FAIL;

        class_2680 state = mc.field_1687.method_8320(target);
        if (state.method_26215()) return BreakFailureReason.BLOCK_NOT_CHANGED;
        if (state.method_26204() instanceof class_2404) return BreakFailureReason.UNKNOWN;
        if (progress <= 0.0f) return BreakFailureReason.BLOCK_NOT_CHANGED;
        return BreakFailureReason.UNKNOWN;
    }
}
