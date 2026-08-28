package com.peoclient.mixin;

import com.peoclient.PeoClient;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderManager.class)
public final class BlockRenderManagerMixin {
    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void peo$xray(BlockState state, BlockPos pos, BlockRenderView world,
                          MatrixStack matrices, VertexConsumer consumer, boolean cull,
                          Random random, CallbackInfo ci) {
        if (!PeoClient.CFG.xray) return;

        boolean target = PeoClient.isXrayBlock(state.getBlock());
        if (target && PeoClient.CFG.xrayExposedOnly) {
            boolean exposed = false;
            for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.values()) {
                BlockPos neighbour = pos.offset(direction);
                if (!world.getBlockState(neighbour).isFullCube(world, neighbour)) {
                    exposed = true;
                    break;
                }
            }
            if (!exposed) {
                ci.cancel();
                return;
            }
        }
        if (!target) {
            int alpha = Math.max(0, Math.min(255, PeoClient.CFG.xrayBackgroundOpacity));
            if (alpha == 0) {
                ci.cancel();
                return;
            }
            consumer.fixedColor(255, 255, 255, alpha);
        }
    }

    @Inject(method = "renderFluid", at = @At("HEAD"), cancellable = true)
    private void peo$xrayFluid(BlockPos pos, BlockRenderView world, VertexConsumer consumer,
                               BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        if (PeoClient.CFG.xray && !PeoClient.CFG.xrayFluids) ci.cancel();
    }
}
