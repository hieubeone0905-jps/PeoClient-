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
    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void peo$xray(class_2680 state, class_2338 pos, class_1920 world,
                          class_4587 matrices, class_4588 consumer, boolean cull,
                          class_5819 random, CallbackInfo ci) {
        if (!PeoClient.CFG.xray) return;

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
            if (!exposed) ci.cancel();
            return;
        }

        // In X-Ray mode, hide non-target blocks when background opacity is zero.
        // Otherwise vanilla rendering is retained to avoid corrupting the render pipeline.
        if (!target && PeoClient.CFG.xrayBackgroundOpacity <= 0) ci.cancel();
    }

    @Inject(method = "renderFluid", at = @At("HEAD"), cancellable = true)
    private void peo$xrayFluid(class_2338 pos, class_1920 world, class_4588 consumer,
                               class_2680 blockState, class_3610 fluidState, CallbackInfo ci) {
        if (PeoClient.CFG.xray && !PeoClient.CFG.xrayFluids) ci.cancel();
    }
}
