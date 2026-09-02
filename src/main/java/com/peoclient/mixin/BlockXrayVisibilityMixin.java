package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_2248;
import net.minecraft.class_2680;
import net.minecraft.class_2350;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * X-Ray visibility gate at the block-face level.
 *
 * This is the important part for Wurst-style X-Ray: a non-filtered block
 * contributes no visible faces at all, while filtered blocks are allowed to
 * render. This works even when the normal BlockRenderManager path is not the
 * final visibility decision for a chunk mesh.
 */
@Mixin(class_2248.class)
public final class BlockXrayVisibilityMixin {
    @Inject(method = "method_9607", at = @At("HEAD"), cancellable = true)
    private static void peo$xrayShouldDrawSide(
            class_2680 state,
            class_2680 otherState,
            class_2350 side,
            CallbackInfoReturnable<Boolean> cir) {
        if (!PeoClient.CFG.xray) return;

        // Only blocks in the user's X-Ray filter may contribute faces.
        // This means an empty filter produces a completely empty world mesh
        // (apart from sky/particles/entities that are handled elsewhere).
        if (!PeoClient.isXrayBlock(state.method_26204())) {
            cir.setReturnValue(false);
        }
    }
}
