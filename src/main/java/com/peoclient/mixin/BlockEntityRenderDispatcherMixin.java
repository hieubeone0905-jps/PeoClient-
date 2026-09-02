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

/** Hides block-entity models in X-Ray FPS mode (chests, barrels, etc.). */
@Mixin(class_824.class)
public final class BlockEntityRenderDispatcherMixin {
    @Inject(method = "method_3555", at = @At("HEAD"), cancellable = true)
    private <E extends class_2586> void peo$xrayHideBlockEntity(
            E blockEntity, float tickDelta, class_4587 matrices,
            class_4597 vertexConsumers, CallbackInfo ci) {
        if (PeoClient.CFG.xray && PeoClient.CFG.xraySkyOnly) {
            ci.cancel();
        }
    }
}
