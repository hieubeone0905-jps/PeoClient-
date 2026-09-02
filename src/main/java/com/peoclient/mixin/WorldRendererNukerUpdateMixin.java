package com.peoclient.mixin;

import com.peoclient.PeoClient;
import com.peoclient.render.NukerRenderBatcher;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_761;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Render-only repair at the actual WorldRenderer#updateBlock boundary.
 *
 * Minecraft 1.21.4 exposes updateBlock as method_8570.  The vanilla renderer
 * normally marks the affected built chunk from this path.  Nuker can generate
 * a burst of ClientWorld updates, so explicitly queue the affected chunk
 * sections after the vanilla update has completed.  This never changes world
 * state and never sends or modifies packets.
 */
@Mixin(class_761.class)
public final class WorldRendererNukerUpdateMixin {
    @Inject(method = "method_8570", at = @At("RETURN"))
    private void peo$queueNukerSection(
            class_1920 world,
            class_2338 pos,
            class_2680 oldState,
            class_2680 newState,
            int flags,
            CallbackInfo ci) {
        try {
            if (!PeoClient.CFG.nuker || oldState == null || newState == null || oldState.equals(newState)) {
                return;
            }

            int x = pos.method_10263();
            int y = pos.method_10264();
            int z = pos.method_10260();
            NukerRenderBatcher.markSectionForBlock(x, y, z);
        } catch (Throwable ignored) {
            // Render repair must never affect vanilla rendering/gameplay.
        }
    }
}
