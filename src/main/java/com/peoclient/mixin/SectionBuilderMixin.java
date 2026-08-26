
package com.peoclient.mixin;
import com.peoclient.PeoClient;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.chunk.SectionBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionBuilder.class)
public class SectionBuilderMixin {
    @Redirect(method="build", at=@At(value="INVOKE", target="Lnet/minecraft/client/render/RenderLayers;getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer peo$layer(BlockState state) {
        if (PeoClient.CFG.xray && PeoClient.CFG.xrayOpacity &&
            !PeoClient.CFG.xrayBlocks.contains(net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString()))
            return RenderLayer.getTranslucent();
        return RenderLayers.getBlockLayer(state);
    }
}
