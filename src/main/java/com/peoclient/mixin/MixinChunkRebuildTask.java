package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.class_776;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Render-path guard for Minecraft 1.21.4 section rebuilds.
 *
 * This intentionally does not replace SectionBuilder's block rendering and
 * does not touch Nuker logic. It only provides a safe rebuild-path hook so
 * PeoClient's render mixins never alter normal block rendering while X-Ray is
 * disabled.
 */
@Mixin(class_776.class)
public final class MixinChunkRebuildTask {
    @Inject(method = "method_3355", at = @At("HEAD"))
    private void peo$sectionRenderGuard(
            net.minecraft.class_2680 state,
            net.minecraft.class_2338 pos,
            net.minecraft.class_1920 world,
            net.minecraft.class_4587 matrices,
            net.minecraft.class_4588 consumer,
            boolean cull,
            net.minecraft.class_5819 random,
            CallbackInfo ci) {
        // Keep vanilla SectionBuilder/BlockRenderManager rendering untouched
        // when X-Ray is disabled. The branch is deliberately empty: this
        // hook exists as a stable guard point for the rebuild path.
        if (!PeoClient.CFG.xray) return;
    }
}
