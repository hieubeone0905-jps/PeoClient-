package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Refreshes the client renderer after a server-driven ClientWorld block-state
 * update while Nuker is active. This is render-only: it never changes the
 * world state and never sends packets.
 *
 * Minecraft 1.21.4 ClientWorld overrides the four-argument setBlockState
 * (method_30092). Hooking World#method_8652 alone misses this path.
 */
@Mixin(class_638.class)
public final class ClientWorldRenderUpdateMixin {
    @Inject(method = "method_30092", at = @At("RETURN"))
    private void peo$nukerClientWorldBlockUpdate(
            class_2338 pos, class_2680 state, int flags, int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            class_310 mc = class_310.method_1551();
            if (mc == null || !PeoClient.CFG.nuker || mc.field_1687 == null || mc.field_1769 == null) {
                return;
            }
            if (mc.field_1687 != (Object) this) {
                return;
            }
            if (!cir.getReturnValueZ()) {
                return;
            }

            int x = pos.method_10263();
            int y = pos.method_10264();
            int z = pos.method_10260();

            // Force the small section containing the changed block to rebuild.
            // Do not reload the whole renderer or the surrounding 3x3x3 chunks.
            mc.field_1769.method_18146(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
        } catch (Throwable ignored) {
            // Renderer refresh must never break vanilla block updates.
        }
    }
}
