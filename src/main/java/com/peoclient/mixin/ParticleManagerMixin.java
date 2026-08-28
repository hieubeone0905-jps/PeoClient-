package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.client.particle.BlockDustParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public final class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void peo$nukerNoParticles(Particle particle, CallbackInfo ci) {
        if (PeoClient.CFG.nuker && PeoClient.CFG.nukerNoParticles && particle instanceof BlockDustParticle)
            ci.cancel();
    }
}
