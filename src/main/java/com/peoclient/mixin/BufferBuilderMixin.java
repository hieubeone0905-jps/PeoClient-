
package com.peoclient.mixin;
import com.peoclient.PeoClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {
    @ModifyVariable(method="color(IIII)Lnet/minecraft/client/render/VertexConsumer;", at=@At("HEAD"), ordinal=3, argsOnly=true)
    private int peo$alpha(int alpha) {
        if (PeoClient.isNonXrayActive() && PeoClient.CFG.xrayOpacity) return PeoClient.CFG.xrayAlpha;
        return alpha;
    }
}
