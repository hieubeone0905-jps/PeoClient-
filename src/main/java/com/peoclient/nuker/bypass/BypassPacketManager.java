package com.peoclient.nuker.bypass;

import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_279;
import net.minecraft.class_310;
import net.minecraft.class_2724;
import net.minecraft.class_2727;
import net.minecraft.class_2729;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Quản lý packet injection cho bypass.
 * Dùng reflection để chèn packet trực tiếp vào sendQueue.
 */
public final class BypassPacketManager {
    private static final class_310 mc = class_310.method_1551();
    private static final Queue<Object> packetQueue = new ConcurrentLinkedQueue<>();
    
    private BypassPacketManager() {}
    
    public static void sendRotation(float yaw, float pitch, boolean onGround) {
        class_2729 packet = new class_2729(yaw, pitch, onGround);
        inject(packet);
    }
    
    public static void sendPosition(double x, double y, double z, boolean onGround) {
        class_2727 packet = new class_2727(x, y, z, onGround);
        inject(packet);
    }
    
    public static void sendBlockAction(class_279.class_280 action, class_2338 pos, class_2350 side) {
        class_2724 packet = new class_2724(action, pos, side);
        inject(packet);
    }
    
    public static void sendResetPacket() {
        if (mc.field_1724 == null) return;
        class_2729 full = new class_2729(
            mc.field_1724.method_36454(),
            mc.field_1724.method_36455(),
            mc.field_1724.field_6228
        );
        inject(full);
    }
    
    public static void clearQueue() {
        packetQueue.clear();
    }
    
    @SuppressWarnings("unchecked")
    private static void inject(Object packet) {
        if (mc.field_1724 == null || packet == null) return;
        var handler = mc.field_1724.field_6214;
        if (handler == null) return;
        
        // Thử injection qua sendQueue
        try {
            Field sendQueueField = handler.getClass().getDeclaredField("field_11121"); // sendQueue
            sendQueueField.setAccessible(true);
            Object queue = sendQueueField.get(handler);
            if (queue instanceof Queue) {
                ((Queue<Object>) queue).add(packet);
                return;
            }
        } catch (Exception e) {
            // Fallback: gửi bình thường
            handler.method_10839((net.minecraft.class_2596) packet);
        }
    }
}