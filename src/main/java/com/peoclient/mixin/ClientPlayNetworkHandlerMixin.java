package com.peoclient.mixin;

import com.peoclient.inventory.InventoryCleaner;
import net.minecraft.class_2653;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_634.class)
public final class ClientPlayNetworkHandlerMixin {
    @Inject(method = "method_11109", at = @At("TAIL"))
    private void peo$inventoryAck(class_2653 packet, CallbackInfo ci) {
        InventoryCleaner.onServerSlotUpdate(
                packet.method_11452(), packet.method_11450(), packet.method_11449(), packet.method_37439());
    }
}