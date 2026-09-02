package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_2586;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_824;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wurst-style X-Ray block-entity filtering. Chests, barrels, etc. are visible
 * only when their cached block state is explicitly present in the X-Ray filter.
 */
@Mixin(class_824.class)
public final class BlockEntityRenderDispatcherMixin {
    @Inject(method = "method_3555", at = @At("HEAD"), cancellable = true)
    private <E extends class_2586> void peo$xrayFilterBlockEntity(
            E blockEntity, float tickDelta, class_4587 matrices,
            class_4597 vertexConsumers, CallbackInfo ci) {
        if (PeoClient.CFG.xray && !PeoClient.isXrayBlock(blockEntity.method_11010().method_26204())) {
            ci.cancel();
        }
    }
}
