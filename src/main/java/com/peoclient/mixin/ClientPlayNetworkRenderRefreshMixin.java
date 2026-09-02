package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_2626;
import net.minecraft.class_2637;
import net.minecraft.class_634;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Render-only repair for server block updates.
 *
 * Some servers send block changes through the play network handler and the
 * client world state changes correctly, but the terrain mesh is not rebuilt
 * until another normal interaction causes a render update.  Hook the two
 * server block-update paths AFTER vanilla has processed them and explicitly
 * schedule a terrain rebuild.  This never changes block state and never sends
 * or modifies packets.
 */
@Mixin(class_634.class)
public final class ClientPlayNetworkRenderRefreshMixin {
    @Inject(method = "method_11136", at = @At("RETURN"))
    private void peo$refreshAfterBlockUpdate(class_2626 packet, CallbackInfo ci) {
        peo$refresh();
    }

    @Inject(method = "method_11100", at = @At("RETURN"))
    private void peo$refreshAfterChunkDelta(class_2637 packet, CallbackInfo ci) {
        peo$refresh();
    }

    private static void peo$refresh() {
        try {
            class_310 mc = class_310.method_1551();
            if (mc == null || !PeoClient.CFG.nuker || mc.field_1687 == null || mc.field_1769 == null) {
                return;
            }

            // Ask the terrain renderer to rebuild dirty terrain immediately
            // after vanilla has applied the server's block update.
            mc.field_1769.method_3292();
        } catch (Throwable ignored) {
            // Rendering repair must never interfere with packet handling.
        }
    }
}
