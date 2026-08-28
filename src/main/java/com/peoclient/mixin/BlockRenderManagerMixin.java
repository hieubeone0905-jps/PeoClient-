
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
    private static final ThreadLocal<Boolean> PEO_NON_XRAY = ThreadLocal.withInitial(() -> false);

    @Inject(method="renderBlock", at=@At("HEAD"), cancellable=true)
    private void peo$renderBlock(BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices,
                                 VertexConsumer consumer, boolean cull, Random random, CallbackInfo ci) {
        if (!PeoClient.CFG.xray) return;
        boolean ore = PeoClient.CFG.xrayBlocks.contains(net.minecraft.registry.Registries.BLOCK.getId(state.getBlock()).toString());
        if (!ore) {
            if (PeoClient.CFG.xrayHideSurface) {
                if (world instanceof WorldView worldView) {
                    int top = worldView.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 1;
                    if (pos.getY() >= top) { ci.cancel(); return; }
                }
            }
            PEO_NON_XRAY.set(PeoClient.CFG.xrayOpacity);
        }
    }

    @Inject(method="renderBlock", at=@At("RETURN"))
    private void peo$renderBlockEnd(BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices,
                                    VertexConsumer consumer, boolean cull, Random random, CallbackInfo ci) {
        PEO_NON_XRAY.set(false);
    }

    @Inject(method="renderFluid", at=@At("HEAD"), cancellable=true)
    private void peo$renderFluid(BlockPos pos, BlockRenderView world, VertexConsumer consumer,
                                 BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        if (PeoClient.CFG.xray && !PeoClient.CFG.xrayFluids) ci.cancel();
    }

    public static boolean peo$isNonXray() { return PEO_NON_XRAY.get(); }
}
