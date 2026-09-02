package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3610;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_5819;
import net.minecraft.class_776;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Full terrain suppression for X-Ray FPS mode. The world data remains intact;
 * this only prevents block geometry from being emitted into the render buffers.
 */
@Mixin(class_776.class)
public final class BlockRenderManagerMixin {
    @Inject(method = "method_3355", at = @At("HEAD"), cancellable = true)
    private void peo$xray(class_2680 state, class_2338 pos, class_1920 world,
                          class_4587 matrices, class_4588 consumer, boolean cull,
                          class_5819 random, CallbackInfo ci) {
        if (PeoClient.CFG.xray && PeoClient.CFG.xraySkyOnly) {
            ci.cancel();
        }
    }

    @Inject(method = "method_3353", at = @At("HEAD"), cancellable = true)
    private void peo$xrayBlockAsEntity(class_2680 state, class_4587 matrices,
                                         class_4597 vertexConsumers, int light, int overlay,
                                         CallbackInfo ci) {
        if (PeoClient.CFG.xray && PeoClient.CFG.xraySkyOnly) {
            ci.cancel();
        }
    }

    @Inject(method = "method_3352", at = @At("HEAD"), cancellable = true)
    private void peo$xrayFluid(class_2338 pos, class_1920 world, class_4588 consumer,
                               class_2680 blockState, class_3610 fluidState, CallbackInfo ci) {
        if (PeoClient.CFG.xray && PeoClient.CFG.xraySkyOnly) {
            ci.cancel();
        }
    }

    @Inject(method = "method_23071", at = @At("HEAD"), cancellable = true)
    private void peo$xrayDamage(class_2680 state, class_2338 pos, class_1920 world,
                                class_4587 matrices, class_4588 consumer, CallbackInfo ci) {
        if (PeoClient.CFG.xray && PeoClient.CFG.xraySkyOnly) {
            ci.cancel();
        }
    }
}
