package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_2338;
import net.minecraft.class_852;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_852.class)
public final class ChunkOcclusionDataBuilderMixin {
    @Inject(method = "markClosed", at = @At("HEAD"), cancellable = true)
    private void peo$xrayNoOcclusion(class_2338 pos, CallbackInfo ci) {
        if (PeoClient.CFG.xray) ci.cancel();
    }
}
