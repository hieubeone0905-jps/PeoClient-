package com.peoclient.mixin;

import com.peoclient.nuker.render.NukerRender;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Watches authoritative ClientWorld block updates and schedules a small,
 * deduplicated client-side render refresh for the updated positions.
 */
@Mixin(class_638.class)
public final class ClientWorldRenderUpdateMixin {
    @Inject(method = "method_41928", at = @At("TAIL"))
    private void peo$onServerBlockUpdate(class_2338 pos, class_2680 state, int flags, CallbackInfo ci) {
        NukerRender.observeServerUpdate(pos);
    }
}
