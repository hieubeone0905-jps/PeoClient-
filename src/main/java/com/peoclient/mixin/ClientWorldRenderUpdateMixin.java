package com.peoclient.mixin;
import com.peoclient.render.NukerRenderBatcher;

import com.peoclient.PeoClient;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Render-only repair for ClientWorld block-state updates.
 *
 * The important part is that the renderer receives the ACTUAL old and new
 * states. Merely asking the renderer to reload/schedule terrain can leave a
 * stale chunk mesh behind when a server block update has already changed the
 * client world state.
 *
 * This mixin never changes the world state and never sends or modifies packets.
 */
@Mixin(class_638.class)
public final class ClientWorldRenderUpdateMixin {
    @Unique
    private static final ThreadLocal<class_2680> peo$oldState = new ThreadLocal<>();

    @Inject(method = "method_30092", at = @At("HEAD"))
    private void peo$captureOldState(
            class_2338 pos, class_2680 state, int flags, int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            class_310 mc = class_310.method_1551();
            if (mc != null && PeoClient.CFG.nuker && mc.field_1687 == (Object) this) {
                peo$oldState.set(mc.field_1687.method_8320(pos));
            }
        } catch (Throwable ignored) {
            peo$oldState.remove();
        }
    }

    @Inject(method = "method_30092", at = @At("RETURN"))
    private void peo$refreshAfterBlockUpdate(
            class_2338 pos, class_2680 state, int flags, int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            class_310 mc = class_310.method_1551();
            if (mc == null || !PeoClient.CFG.nuker || mc.field_1687 == null || mc.field_1769 == null) {
                return;
            }
            if (mc.field_1687 != (Object) this || !cir.getReturnValueZ()) {
                return;
            }

            class_2680 oldState = peo$oldState.get();
            class_2680 newState = mc.field_1687.method_8320(pos);
            int x = pos.method_10263();
            int y = pos.method_10264();
            int z = pos.method_10260();

            if (oldState != null && !oldState.equals(newState)) {
                // Preserve the exact old/new incremental update.
                mc.field_1769.method_8570(mc.field_1687, pos, oldState, newState, flags);
                NukerRenderBatcher.mark(x, y, z);
            }
        } catch (Throwable ignored) {
            // Renderer repair must never break vanilla world updates.
        } finally {
            peo$oldState.remove();
        }
    }
}
