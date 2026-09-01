package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_702;
import net.minecraft.class_703;
import net.minecraft.class_727;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_702.class)
public final class ParticleManagerMixin {
    @Inject(method = "method_3058", at = @At("HEAD"), cancellable = true)
    private void peo$nukerNoParticles(class_703 particle, CallbackInfo ci) {
        if (PeoClient.CFG.nuker && PeoClient.CFG.nukerNoParticles && particle instanceof class_727)
            ci.cancel();
    }
}
