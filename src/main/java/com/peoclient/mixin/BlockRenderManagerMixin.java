package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.WorldView;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {
    @Inject(method="renderBlock", at=@At("HEAD"), cancellable=true)
    private void peo$xray(BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices,
                          VertexConsumer consumer, boolean cull, Random random, CallbackInfo ci) {
        if(!PeoClient.CFG.xray) return;
        String id=net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString();
        if(!PeoClient.CFG.xrayBlocks.contains(id)) {
            ci.cancel();
        }
    }

    @Inject(method="renderFluid", at=@At("HEAD"), cancellable=true)
    private void peo$xrayFluid(BlockPos pos, BlockRenderView world, VertexConsumer consumer,
                               BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        if(PeoClient.CFG.xray && !PeoClient.CFG.xrayFluids) ci.cancel();
    }
}
