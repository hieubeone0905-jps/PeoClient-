package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3610;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_5819;
import net.minecraft.class_776;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_776.class)
public final class BlockRenderManagerMixin {
    @Inject(method = "method_3355", at = @At("HEAD"), cancellable = true)
    private void peo$xray(class_2680 state, class_2338 pos, class_1920 world,
                          class_4587 matrices, class_4588 consumer, boolean cull,
                          class_5819 random, CallbackInfo ci) {
        if (!PeoClient.CFG.xray) return;

        // Sky-only performance mode: cancel EVERY terrain block model before
        // vanilla has a chance to emit vertices. This intentionally includes
        // stone, dirt, grass, wood, ores and every other block. It is purely
        // client-side rendering; the actual world/chunks are not modified.
        if (PeoClient.CFG.xraySkyOnly) {
            ci.cancel();
            return;
        }

        boolean target = PeoClient.isXrayBlock(state.method_26204());
        if (target && PeoClient.CFG.xrayExposedOnly) {
            boolean exposed = false;
            for (net.minecraft.class_2350 direction : net.minecraft.class_2350.values()) {
                class_2338 neighbour = pos.method_10093(direction);
                if (!world.method_8320(neighbour).method_26234(world, neighbour)) {
                    exposed = true;
                    break;
                }
            }
            if (!exposed) {
                ci.cancel();
                return;
            }
        }
        if (!target) {
            int alpha = Math.max(0, Math.min(255, PeoClient.CFG.xrayBackgroundOpacity));
            if (alpha == 0) {
                ci.cancel();
                return;
            }
            // VertexConsumer in Minecraft 1.21.4 does not expose fixedColor().
            // Keep X-Ray stable by cancelling non-target blocks when the configured
            // background opacity is zero; otherwise let vanilla render them.
        }
    }

    @Inject(method = "method_3353", at = @At("HEAD"), cancellable = true)
    private void peo$xraySkyOnlyEntity(class_2680 state, class_4587 matrices,
                                        net.minecraft.class_4597 vertexConsumers, int light, int overlay,
                                        CallbackInfo ci) {
        if (PeoClient.CFG.xray && PeoClient.CFG.xraySkyOnly) ci.cancel();
    }

    @Inject(method = "method_3352", at = @At("HEAD"), cancellable = true)
    private void peo$xrayFluid(class_2338 pos, class_1920 world, class_4588 consumer,
                               class_2680 blockState, class_3610 fluidState, CallbackInfo ci) {
        if (PeoClient.CFG.xray && (PeoClient.CFG.xraySkyOnly || !PeoClient.CFG.xrayFluids)) ci.cancel();
    }
}
