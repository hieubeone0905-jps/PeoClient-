package com.peoclient.mixin;

import com.peoclient.inventory.InventoryCleaner;
import com.peoclient.diagnostic.ServerInfoCollector;
import net.minecraft.class_2653;
import net.minecraft.class_634;
import net.minecraft.class_2719;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_634.class)
public final class ClientPlayNetworkHandlerMixin {
    // Hook inventory slot update
    @Inject(method = "method_11109", at = @At("TAIL"))
    private void peo$inventoryAck(class_2653 packet, CallbackInfo ci) {
        InventoryCleaner.onServerSlotUpdate(
                packet.method_11452(), packet.method_11450(), packet.method_11449(), packet.method_37439());
    }

    // Hook CustomPayload (Plugin Message) - thụ động, không gửi lệnh
    @Inject(method = "method_11490", at = @At("HEAD"))
    private void peo$pluginMessage(class_2719 packet, CallbackInfo ci) {
        try {
            String channel = packet.method_11472().toString();
            byte[] data = packet.method_11471();
            ServerInfoCollector.get().recordPluginMessage(channel, data);

            // Nếu là "minecraft:brand" thì ghi lại brand
            if ("minecraft:brand".equals(channel) || "MC|Brand".equals(channel)) {
                if (data != null && data.length > 0) {
                    String brand = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    ServerInfoCollector.get().setServerBrand(brand);
                }
            }
        } catch (Throwable ignored) {
            // Không để lỗi làm crash client
        }
    }
}