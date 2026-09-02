package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the client terrain mesh synchronized with block-state updates while
 * Nuker is active. This only schedules a small render region; it does not
 * change the world state or send any packets.
 */
@Mixin(class_1937.class)
public final class ClientWorldRenderUpdateMixin {
    @Inject(method = "method_8652", at = @At("RETURN"))
    private void peo$nukerBlockStateRenderUpdate(
            class_2338 pos, class_2680 state, int flags,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            class_310 mc = class_310.method_1551();
            if (mc == null || !PeoClient.CFG.nuker || mc.field_1687 == null || mc.field_1769 == null) {
                return;
            }
            if ((Object) mc.field_1687 != (Object) this) {
                return;
            }

            int x = pos.method_10263();
            int y = pos.method_10264();
            int z = pos.method_10260();
            // Small local rebuild around the changed block. This is the same
            // incremental path vanilla uses instead of reloading the renderer.
            mc.field_1769.method_18146(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
        } catch (Throwable ignored) {
            // Render refresh must never break normal block-state processing.
        }
    }
}
