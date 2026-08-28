package com.peoclient.mixin;

import com.peoclient.inventory.InventoryCleaner;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public final class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("TAIL"))
    private void peo$inventoryAck(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        InventoryCleaner.onServerSlotUpdate(
                packet.getSyncId(), packet.getSlot(), packet.getStack(), packet.getRevision());
    }
}
