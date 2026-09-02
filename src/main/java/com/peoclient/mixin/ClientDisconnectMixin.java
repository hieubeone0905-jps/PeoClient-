package com.peoclient.mixin;

import com.peoclient.diagnostic.DisconnectListener;
import net.minecraft.class_8673;
import net.minecraft.class_9812;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the client-side disconnect lifecycle in Minecraft 1.21.4. */
@Mixin(class_8673.class)
public final class ClientDisconnectMixin {
    @Inject(method = "method_10839", at = @At("HEAD"))
    private void peo$onDisconnected(class_9812 info, CallbackInfo ci) {
        DisconnectListener.onDisconnect(info);
    }
}
