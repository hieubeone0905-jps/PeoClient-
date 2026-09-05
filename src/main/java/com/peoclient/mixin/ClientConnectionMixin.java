package com.peoclient.mixin;

import com.peoclient.diagnostic.SessionResetManager;
import net.minecraft.class_2678;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-side join hook for Minecraft 1.21.4. */
@Mixin(class_634.class)
public final class ClientConnectionMixin {
    @Inject(method = "method_11120", at = @At("TAIL"))
    private void peo$onGameJoin(class_2678 packet, CallbackInfo ci) {
        SessionResetManager.get().onConnect();
    }
}
