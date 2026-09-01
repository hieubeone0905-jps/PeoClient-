// NukerBypass.java
package com.peoclient.nuker;

import com.peoclient.PeoClient;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Nuker Bypass module - Production-grade packet manipulation for 1.21.4
 * Uses reflection injection + rotation spoofing + position desync
 */
public final class NukerBypass {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private static final Queue<PlayerMoveC2SPacket.PositionAndOnGround> positionBuffer = new ArrayDeque<>(10);
    
    private static BlockPos targetPos;
    private static float targetYaw, targetPitch;
    private static boolean active = false;
    private static int rotationTicks = 0;
    private static double lastSentY = 0;
    private static boolean hasSentPosition = false;
    
    // Anti-detection parameters
    private static final float MAX_ROTATION_STEP = 30.0f;
    private static final double POSITION_JITTER = 0.0005;
    private static final int POSITION_BUFFER_SIZE = 5;
    
    public static void start(BlockPos target) {
        if (mc.player == null || mc.world == null) return;
        
        targetPos = target.toImmutable();
        calculateRotation(targetPos);
        active = true;
        rotationTicks = 0;
        hasSentPosition = false;
        positionBuffer.clear();
        
        // Start bypass thread
        new Thread(NukerBypass::bypassLoop, "Nuker-Bypass-Thread").start();
    }
    
    public static void stop() {
        active = false;
        packetQueue.clear();
        positionBuffer.clear();
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
    }
    
    public static boolean isActive() {
        return active && mc.player != null;
    }
    
    private static void calculateRotation(BlockPos pos) {
        if (mc.player == null) return;
        
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3d diff = targetVec.subtract(eyePos);
        
        double horiz = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        targetYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, horiz));
    }
    
    private static void bypassLoop() {
        while (active && mc.player != null && mc.world != null) {
            try {
                long startTime = System.currentTimeMillis();
                
                // Rotate with smoothing
                if (rotationTicks % 2 == 0) {
                    sendSmoothRotation();
                }
                
                // Send position with micro-offset
                if (rotationTicks % 3 == 0) {
                    sendPositionUpdate();
                }
                
                // Send break packet
                if (rotationTicks % 1 == 0) {
                    sendBreakPacket();
                }
                
                rotationTicks = (rotationTicks + 1) % 6;
                
                // Dynamic delay to avoid timing detection
                long elapsed = System.currentTimeMillis() - startTime;
                long delay = Math.max(1, 25 - elapsed); // ~40 packets/sec
                Thread.sleep(delay, (int)(Math.random() * 500000));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Silent fail
            }
        }
    }
    
    private static void sendSmoothRotation() {
        if (mc.player == null) return;
        
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        
        // Smooth interpolation with acceleration
        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = MathHelper.wrapDegrees(targetPitch - currentPitch);
        
        // Add random noise to avoid pattern detection
        float noise = (float)(Math.sin(System.currentTimeMillis() / 1000.0) * 0.15);
        float step = MAX_ROTATION_STEP * (1.0f + noise * 0.1f);
        
        float newYaw = currentYaw + MathHelper.clamp(yawDiff, -step, step);
        float newPitch = currentPitch + MathHelper.clamp(pitchDiff, -step * 0.8f, step * 0.8f);
        
        // Send rotation packet via injection
        PlayerMoveC2SPacket.LookAndOnGround rotationPacket = new PlayerMoveC2SPacket.LookAndOnGround(
            newYaw, newPitch, mc.player.isOnGround(), mc.player.horizontalCollision
        );
        injectPacket(rotationPacket);
        
        // Update local player silently
        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
    }
    
    private static void sendPositionUpdate() {
        if (mc.player == null) return;
        
        // Random micro-offset
        double offsetX = Math.sin(System.currentTimeMillis() / 400.0) * POSITION_JITTER;
        double offsetZ = Math.cos(System.currentTimeMillis() / 400.0) * POSITION_JITTER;
        double offsetY = Math.sin(System.currentTimeMillis() / 300.0 + 1.0) * POSITION_JITTER * 0.5;
        
        double posX = mc.player.getX() + offsetX;
        double posY = mc.player.getY() + offsetY;
        double posZ = mc.player.getZ() + offsetZ;
        
        PlayerMoveC2SPacket.PositionAndOnGround posPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
            posX, posY, posZ, mc.player.isOnGround(), mc.player.horizontalCollision
        );
        
        // Buffer for validation
        positionBuffer.add(posPacket);
        if (positionBuffer.size() > POSITION_BUFFER_SIZE) {
            positionBuffer.poll();
        }
        
        // Inject via reflection
        injectPacket(posPacket);
        hasSentPosition = true;
        lastSentY = posY;
    }
    
    private static void sendBreakPacket() {
        if (mc.player == null || mc.world == null || targetPos == null) return;
        
        // Verify block still exists
        BlockState state = mc.world.getBlockState(targetPos);
        if (state.isAir()) {
            active = false;
            return;
        }
        
        // Method 1: Use reflection to spoof CPacketPlayerDigging
        try {
            PlayerActionC2SPacket digPacket = new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                targetPos,
                Direction.UP
            );
            injectPacket(digPacket);
            
            PlayerActionC2SPacket stopPacket = new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                targetPos,
                Direction.UP
            );
            injectPacket(stopPacket);
            
            // Swing hand to look legitimate
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            
            // Method 2: Use interaction packet as fallback
            if (Math.random() < 0.2) {
                ClientPlayerInteractBlockC2SPacket interactPacket = new ClientPlayerInteractBlockC2SPacket(
                    net.minecraft.util.Hand.MAIN_HAND,
                    new BlockHitResult(
                        new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5),
                        Direction.UP,
                        targetPos,
                        false
                    )
                );
                injectPacket(interactPacket);
            }
            
        } catch (Exception e) {
            // Fallback: normal interaction
            if (mc.interactionManager != null) {
                mc.interactionManager.attackBlock(targetPos, Direction.UP);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void injectPacket(Packet<?> packet) {
        if (mc.player == null) return;
        ClientPlayNetworkHandler handler = mc.player.networkHandler;
        if (handler == null) return;
        
        // Primary method: Direct queue injection
        try {
            Field sendQueueField = handler.getClass().getDeclaredField("sendQueue");
            sendQueueField.setAccessible(true);
            Object queue = sendQueueField.get(handler);
            
            if (queue instanceof java.util.concurrent.ConcurrentLinkedQueue) {
                java.util.concurrent.ConcurrentLinkedQueue<Packet<?>> q = 
                    (java.util.concurrent.ConcurrentLinkedQueue<Packet<?>>) queue;
                q.add(packet);
                return;
            }
        } catch (Exception e) {
            // Fallback to standard send
            handler.sendPacket(packet);
        }
    }
    
    /**
     * Spoof position for anti-cheat desync detection
     */
    public static void spoofPosition(double x, double y, double z) {
        if (!active || mc.player == null) return;
        
        PlayerMoveC2SPacket.PositionAndOnGround posPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
            x + Math.sin(System.currentTimeMillis() / 200.0) * 0.0002,
            y + Math.cos(System.currentTimeMillis() / 200.0) * 0.0002,
            z + Math.sin(System.currentTimeMillis() / 300.0) * 0.0002,
            mc.player.isOnGround(), mc.player.horizontalCollision
        );
        injectPacket(posPacket);
    }
    
    /**
     * Bypass for rotation desync detection
     */
    public static void syncRotation(float yaw, float pitch) {
        if (!active || mc.player == null) return;
        
        // Send accurate rotation every few ticks
        PlayerMoveC2SPacket.LookAndOnGround rotationPacket = new PlayerMoveC2SPacket.LookAndOnGround(
            yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision
        );
        injectPacket(rotationPacket);
    }
    
    /**
     * Reset bypass state
     */
    public static void reset() {
        active = false;
        packetQueue.clear();
        positionBuffer.clear();
        rotationTicks = 0;
        hasSentPosition = false;
    }
}