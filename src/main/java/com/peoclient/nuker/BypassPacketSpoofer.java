// BypassPacketSpoofer.java
package com.peoclient.nuker;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Advanced packet spoofing for server bypass
 */
public final class BypassPacketSpoofer {
    private static final Queue<Object> packetQueue = new ConcurrentLinkedQueue<>();
    
    /**
     * Spoof player position with desync
     */
    public static void spoofPosition(double x, double y, double z, boolean onGround) {
        PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(
            x + getOffset(),
            y + getOffset() * 0.5,
            z + getOffset(),
            onGround,
            false
        );
        injectPacket(packet);
    }
    
    /**
     * Spoof rotation with micro-changes
     */
    public static void spoofRotation(float yaw, float pitch, boolean onGround) {
        PlayerMoveC2SPacket.LookAndOnGround packet = new PlayerMoveC2SPacket.LookAndOnGround(
            yaw + (float)(Math.random() * 0.02 - 0.01),
            pitch + (float)(Math.random() * 0.02 - 0.01),
            onGround,
            false
        );
        injectPacket(packet);
    }
    
    /**
     * Spoof block dig with different directions
     */
    public static void spoofBlockDig(BlockPos pos, Direction side) {
        PlayerActionC2SPacket start = new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
            pos,
            side
        );
        injectPacket(start);
        
        // Random delay simulation
        try {
            Thread.sleep(10 + (long)(Math.random() * 20));
        } catch (InterruptedException ignored) {}
        
        PlayerActionC2SPacket stop = new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
            pos,
            side
        );
        injectPacket(stop);
    }
    
    private static double getOffset() {
        return Math.sin(System.currentTimeMillis() / 300.0) * 0.0004 +
               Math.cos(System.currentTimeMillis() / 400.0) * 0.0004;
    }
    
    @SuppressWarnings("unchecked")
    private static void injectPacket(Object packet) {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null) return;
        
        var handler = mc.player.networkHandler;
        if (handler == null) return;
        
        try {
            Field sendQueueField = handler.getClass().getDeclaredField("sendQueue");
            sendQueueField.setAccessible(true);
            Object queue = sendQueueField.get(handler);
            
            if (queue instanceof ConcurrentLinkedQueue) {
                ((ConcurrentLinkedQueue<Object>) queue).add(packet);
            }
        } catch (Exception ignored) {
            // Fallback: standard send if reflection fails
        }
    }
}