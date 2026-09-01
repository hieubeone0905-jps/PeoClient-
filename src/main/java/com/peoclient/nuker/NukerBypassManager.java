// NukerBypassManager.java
package com.peoclient.nuker;

import com.peoclient.PeoClient;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Manages Nuker Bypass integration with existing NukerLogic
 */
public final class NukerBypassManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean bypassMode = false;
    private static BlockPos currentTarget = null;
    private static int bypassCooldown = 0;
    
    public static void tick() {
        if (!PeoClient.CFG.nuker || mc.player == null || mc.world == null) {
            if (NukerBypass.isActive()) {
                NukerBypass.stop();
            }
            return;
        }
        
        // Check if bypass should be active
        boolean shouldBypass = shouldEnableBypass();
        
        if (shouldBypass && bypassCooldown <= 0) {
            // Find nearest valid block
            BlockPos target = findTargetBlock();
            if (target != null && !target.equals(currentTarget)) {
                currentTarget = target;
                NukerBypass.start(target);
                bypassMode = true;
                bypassCooldown = 5;
            }
        } else {
            if (NukerBypass.isActive()) {
                NukerBypass.stop();
                bypassMode = false;
                currentTarget = null;
            }
        }
        
        if (bypassCooldown > 0) {
            bypassCooldown--;
        }
        
        // If bypass is active, spoof additional packets
        if (bypassMode && NukerBypass.isActive()) {
            // Periodic position spoof to avoid rubber-band detection
            if (mc.player.age % 4 == 0) {
                NukerBypass.spoofPosition(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ()
                );
            }
            
            // Periodic rotation sync
            if (mc.player.age % 5 == 0) {
                NukerBypass.syncRotation(
                    mc.player.getYaw(),
                    mc.player.getPitch()
                );
            }
        }
    }
    
    private static boolean shouldEnableBypass() {
        // Enable bypass in these conditions:
        // 1. Nuker is enabled and target in range
        // 2. Block hardness > 0 (not bedrock/air)
        // 3. Player is creative or survival with tools
        
        if (mc.player == null || mc.world == null) return false;
        if (!PeoClient.CFG.nuker) return false;
        
        // Check if player is looking at a valid block
        var hit = mc.player.raycast(PeoClient.CFG.nukerRange + 2.0, 1.0f, false);
        if (hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            BlockState state = mc.world.getBlockState(pos);
            float hardness = state.getHardness(mc.world, pos);
            
            // Not breakable or already broken
            if (hardness < 0 || state.isAir()) return false;
            
            // Check distance
            double dist = mc.player.getEyePos().distanceTo(
                new net.minecraft.util.math.Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
            );
            if (dist > PeoClient.CFG.nukerRange + 1.0) return false;
            
            return true;
        }
        return false;
    }
    
    private static BlockPos findTargetBlock() {
        if (mc.player == null || mc.world == null) return null;
        
        double range = MathHelper.clamp(PeoClient.CFG.nukerRange, 1.0, 8.0);
        int r = MathHelper.ceil(range);
        BlockPos center = BlockPos.ofFloored(mc.player.getEyePos());
        
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);
                    double dist = mc.player.getEyePos().distanceTo(
                        new net.minecraft.util.math.Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    );
                    if (dist > range + 0.5) continue;
                    
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.isAir()) continue;
                    
                    // Check if player can see the block
                    if (PeoClient.CFG.nukerRaycast) {
                        var hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                            mc.player.getEyePos(),
                            new net.minecraft.util.math.Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                            net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                            net.minecraft.world.RaycastContext.FluidHandling.NONE,
                            mc.player
                        ));
                        if (hit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK || 
                            !hit.getBlockPos().equals(pos)) continue;
                    }
                    
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }
    
    public static boolean isBypassActive() {
        return bypassMode && NukerBypass.isActive();
    }
    
    public static BlockPos getCurrentTarget() {
        return currentTarget;
    }
    
    public static void reset() {
        NukerBypass.reset();
        bypassMode = false;
        currentTarget = null;
        bypassCooldown = 0;
    }
}