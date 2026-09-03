package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.*;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3965;
import net.minecraft.class_279;
import net.minecraft.class_2724;
import net.minecraft.class_2727;
import net.minecraft.class_2729;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Nuker Bypass Ultimate V2 - Bypass Grim/Vulcan/NoCheatPlus
 * 
 * Chiến thuật:
 * 1. Rotation Smoothing - Xoay mượt để tránh rotation check
 * 2. Position Desync - Sai lệch vị trí nhỏ để tránh phát hiện
 * 3. Packet Timing - Điều chỉnh thời gian gửi packet
 * 4. Block Break Simulation - Mô phỏng break progress tự nhiên
 * 5. Server Response Mimic - Bắt chước phản hồi của client vanilla
 * 6. Multi-layer Protection - Nhiều lớp bảo vệ cho các AC khác nhau
 * 
 * KHÔNG làm giảm sức mạnh Nuker - chỉ điều chỉnh cách gửi packet
 */
public final class NukerBypassUltimateV2 {
    private static final class_310 mc = class_310.method_1551();
    private static final Random RANDOM = new Random();
    private static final AtomicBoolean active = new AtomicBoolean(false);
    
    // State
    private static class_2338 currentTarget;
    private static float targetYaw, targetPitch;
    private static float lastSentYaw, lastSentPitch;
    private static double lastSentX, lastSentY, lastSentZ;
    private static int tickCounter = 0;
    private static int phase = 0;
    
    // Bypass parameters
    private static boolean grimMode = true;
    private static boolean vulcanMode = true;
    private static boolean nocheatplusMode = true;
    private static int intensity = 7; // 1-10
    private static int desyncLevel = 3; // 1-5
    
    // Packet queue
    private static final Queue<Object> packetQueue = new ConcurrentLinkedQueue<>();
    
    // Timing
    private static long lastPacketTime = 0;
    private static int packetCounter = 0;
    
    // Rotation buffer
    private static final Queue<Float> yawBuffer = new ConcurrentLinkedQueue<>();
    private static final Queue<Float> pitchBuffer = new ConcurrentLinkedQueue<>();
    
    // Position buffer
    private static final Queue<Double> xBuffer = new ConcurrentLinkedQueue<>();
    private static final Queue<Double> yBuffer = new ConcurrentLinkedQueue<>();
    private static final Queue<Double> zBuffer = new ConcurrentLinkedQueue<>();
    
    public static void start(class_2338 target) {
        if (mc.field_1724 == null || mc.field_1687 == null) return;
        currentTarget = target.toImmutable();
        calculateRotation(currentTarget);
        active.set(true);
        tickCounter = 0;
        phase = 0;
        packetQueue.clear();
        yawBuffer.clear();
        pitchBuffer.clear();
        xBuffer.clear();
        yBuffer.clear();
        zBuffer.clear();
        
        DiagnosticRecorder.get().record("NukerBypassV2", "Started on " + target);
    }
    
    public static void stop() {
        active.set(false);
        packetQueue.clear();
        if (mc.field_1761 != null) {
            mc.field_1761.method_2925();
        }
        DiagnosticRecorder.get().record("NukerBypassV2", "Stopped");
    }
    
    public static boolean isActive() {
        return active.get() && mc.field_1724 != null && currentTarget != null;
    }
    
    public static void tick() {
        if (!isActive()) return;
        if (mc.field_1724 == null || mc.field_1687 == null) return;
        
        tickCounter++;
        long startTime = System.nanoTime();
        
        // Check target still valid
        if (mc.field_1687.method_8320(currentTarget).method_26215()) {
            stop();
            return;
        }
        
        // Phase rotation
        if (tickCounter % 2 == 0) {
            sendSpoofedRotation();
        }
        
        // Phase position
        if (tickCounter % 3 == 0) {
            sendSpoofedPosition();
        }
        
        // Phase break
        if (tickCounter % getBreakInterval() == 0) {
            sendBreakPacket();
        }
        
        // Phase reset
        if (tickCounter % 200 == 0) {
            sendResetPacket();
        }
        
        // Phase sync
        if (tickCounter % 50 == 0) {
            sendSyncPacket();
        }
        
        // Process queued packets
        while (!packetQueue.isEmpty()) {
            Object packet = packetQueue.poll();
            if (packet != null) {
                injectPacket(packet);
            }
        }
        
        // Do not sleep on Minecraft's client tick thread. The original timing
        // calculation is kept as a pacing hint for the next tick instead of
        // blocking the game loop.
        long elapsed = System.nanoTime() - startTime;
        long delay = calculateDynamicDelay();
        lastPacketTime = System.currentTimeMillis();
    }
    
    private static void sendSpoofedRotation() {
        if (mc.field_1724 == null || currentTarget == null) return;
        
        // Recalculate rotation
        calculateRotation(currentTarget);
        
        float currentYaw = mc.field_1724.method_36454();
        float currentPitch = mc.field_1724.method_36455();
        
        float yawDiff = class_3532.method_15393(targetYaw - currentYaw);
        float pitchDiff = class_3532.method_15393(targetPitch - currentPitch);
        
        // Grim: Max step ~22.5 độ/tick
        float grimMaxStep = 22.5f;
        // Vulcan: Max step ~18 độ/tick
        float vulcanMaxStep = 18.0f;
        
        float maxStep = grimMode ? grimMaxStep : 30.0f;
        if (vulcanMode) maxStep = Math.min(maxStep, vulcanMaxStep);
        
        // Intensity scaling
        float intensityFactor = 0.6f + (intensity / 10.0f) * 0.4f;
        float step = maxStep * intensityFactor;
        
        // Add noise
        float noise = (float)(Math.sin(System.currentTimeMillis() / 500.0) * 0.1);
        step *= (1.0f + noise);
        
        float newYaw = currentYaw + class_3532.method_15340(yawDiff, -step, step);
        float newPitch = currentPitch + class_3532.method_15340(pitchDiff, -step * 0.7f, step * 0.7f);
        
        // Add micro-noise
        newYaw += (float)((RANDOM.nextDouble() - 0.5) * 0.02);
        newPitch += (float)((RANDOM.nextDouble() - 0.5) * 0.02);
        
        newYaw = class_3532.method_15393(newYaw);
        newPitch = class_3532.method_15363(newPitch, -90, 90);
        
        // Buffer smoothing
        yawBuffer.add(newYaw);
        pitchBuffer.add(newPitch);
        if (yawBuffer.size() > 5) yawBuffer.poll();
        if (pitchBuffer.size() > 5) pitchBuffer.poll();
        
        float smoothedYaw = newYaw;
        float smoothedPitch = newPitch;
        if (yawBuffer.size() >= 3) {
            smoothedYaw = (float) yawBuffer.stream().mapToDouble(f -> f).average().orElse(newYaw);
            smoothedPitch = (float) pitchBuffer.stream().mapToDouble(f -> f).average().orElse(newPitch);
        }
        
        // Send rotation packet
        class_2729 rotationPacket = new class_2729(smoothedYaw, smoothedPitch, mc.field_1724.field_6228);
        packetQueue.add(rotationPacket);
        
        // Update local player
        mc.field_1724.method_36456(newYaw);
        mc.field_1724.method_36457(newPitch);
        
        lastSentYaw = smoothedYaw;
        lastSentPitch = smoothedPitch;
    }
    
    private static void sendSpoofedPosition() {
        if (mc.field_1724 == null) return;
        
        double noise = 0.00008 * intensity;
        double desync = 0.00005 * desyncLevel;
        
        double offsetX = Math.sin(tickCounter / 30.0 + 1.2) * noise + (RANDOM.nextDouble() - 0.5) * desync;
        double offsetZ = Math.cos(tickCounter / 30.0 + 0.7) * noise + (RANDOM.nextDouble() - 0.5) * desync;
        double offsetY = Math.sin(tickCounter / 25.0 + 2.1) * noise * 0.5 + (RANDOM.nextDouble() - 0.5) * desync * 0.5;
        
        double x = mc.field_1724.method_23317() + offsetX;
        double y = mc.field_1724.method_23318() + offsetY;
        double z = mc.field_1724.method_23321() + offsetZ;
        
        // Buffer smoothing
        xBuffer.add(x);
        yBuffer.add(y);
        zBuffer.add(z);
        if (xBuffer.size() > 3) xBuffer.poll();
        if (yBuffer.size() > 3) yBuffer.poll();
        if (zBuffer.size() > 3) zBuffer.poll();
        
        if (xBuffer.size() >= 3) {
            x = xBuffer.stream().mapToDouble(d -> d).average().orElse(x);
            y = yBuffer.stream().mapToDouble(d -> d).average().orElse(y);
            z = zBuffer.stream().mapToDouble(d -> d).average().orElse(z);
        }
        
        class_2727 posPacket = new class_2727(x, y, z, mc.field_1724.field_6228);
        packetQueue.add(posPacket);
        
        lastSentX = x;
        lastSentY = y;
        lastSentZ = z;
    }
    
    private static void sendBreakPacket() {
        if (mc.field_1724 == null || mc.field_1687 == null || currentTarget == null) return;
        if (mc.field_1687.method_8320(currentTarget).method_26215()) {
            active.set(false);
            return;
        }
        
        class_2350 side = getBestSide(currentTarget);
        if (side == null) side = class_2350.field_11036;
        
        // Send start destroy
        class_2724 startPacket = new class_2724(
            class_279.class_280.field_1370, // START_DESTROY_BLOCK
            currentTarget,
            side
        );
        packetQueue.add(startPacket);
        
        // Simulate break progress
        if (!"Instant".equalsIgnoreCase(PeoClient.CFG.nukerMode)) {
            int steps = 2 + RANDOM.nextInt(3);
            for (int i = 0; i < steps; i++) {
                class_2724 progressPacket = new class_2724(
                    class_279.class_280.field_1369, // ABORT_DESTROY_BLOCK
                    currentTarget,
                    side
                );
                packetQueue.add(progressPacket);
            }
        }
        
        // Send stop destroy
        class_2724 stopPacket = new class_2724(
            class_279.class_280.field_1371, // STOP_DESTROY_BLOCK
            currentTarget,
            side
        );
        packetQueue.add(stopPacket);
        
        // Swing hand
        mc.field_1724.method_6104(net.minecraft.class_1268.field_5808);
    }
    
    private static void sendResetPacket() {
        if (mc.field_1724 == null) return;
        class_2729 resetPacket = new class_2729(
            mc.field_1724.method_36454(),
            mc.field_1724.method_36455(),
            mc.field_1724.field_6228
        );
        packetQueue.add(resetPacket);
    }
    
    private static void sendSyncPacket() {
        if (mc.field_1724 == null) return;
        class_2729 syncPacket = new class_2729(
            mc.field_1724.method_36454(),
            mc.field_1724.method_36455(),
            mc.field_1724.field_6228
        );
        packetQueue.add(syncPacket);
    }
    
    private static void calculateRotation(class_2338 pos) {
        if (mc.field_1724 == null) return;
        class_243 eye = mc.field_1724.method_33571();
        class_243 center = class_243.method_24953(pos);
        class_243 diff = center.method_1020(eye);
        double horiz = Math.sqrt(diff.field_1352 * diff.field_1352 + diff.field_1350 * diff.field_1350);
        targetYaw = (float) Math.toDegrees(Math.atan2(diff.field_1350, diff.field_1352)) - 90.0f;
        targetPitch = (float) -Math.toDegrees(Math.atan2(diff.field_1351, horiz));
    }
    
    private static class_2350 getBestSide(class_2338 pos) {
        if (mc.field_1724 == null) return class_2350.field_11036;
        class_243 eye = mc.field_1724.method_33571();
        class_243 center = class_243.method_24953(pos);
        class_243 diff = eye.method_1020(center);
        class_2350 best = class_2350.field_11036;
        double bestDot = -Double.MAX_VALUE;
        for (class_2350 side : class_2350.values()) {
            class_243 normal = new class_243(side.method_10148(), side.method_10164(), side.method_10165());
            double dot = diff.normalize().method_1020(normal);
            if (dot > bestDot) {
                bestDot = dot;
                best = side;
            }
        }
        return best;
    }
    
    private static int getBreakInterval() {
        int base = PeoClient.CFG.nukerCooldown + 1;
        if (grimMode) base += 1;
        if (vulcanMode) base += 1;
        if (nocheatplusMode) base += 1;
        base += RANDOM.nextInt(2);
        return Math.max(1, Math.min(6, base));
    }
    
    private static long calculateDynamicDelay() {
        long base = 8 + RANDOM.nextInt(15);
        
        // Adjust based on protection level
        if (intensity > 7) base += 2;
        if (desyncLevel > 3) base += 1;
        
        return Math.max(1, base);
    }
    
    private static void injectPacket(Object packet) {
        if (mc.field_1724 == null || packet == null) return;
        var handler = mc.field_1724.field_6214;
        if (handler == null) return;
        
        // Try queue injection
        try {
            Field sendQueueField = handler.getClass().getDeclaredField("field_11121");
            sendQueueField.setAccessible(true);
            Object queue = sendQueueField.get(handler);
            if (queue instanceof ConcurrentLinkedQueue) {
                ((ConcurrentLinkedQueue<Object>) queue).add(packet);
                return;
            }
        } catch (Exception e) {
            // Fallback: normal send
            handler.method_10839((net.minecraft.class_2596) packet);
        }
    }
    
    // Getters/Setters
    public static void setEnabled(boolean enable) {
        if (!enable) {
            stop();
            return;
        }
        if (active.get()) return;
        class_2338 target = com.peoclient.PeoClient.NukerLogic.getCurrentTarget();
        if (target != null) start(target);
    }
    
    public static void setGrimMode(boolean enable) { grimMode = enable; }
    public static void setVulcanMode(boolean enable) { vulcanMode = enable; }
    public static void setNoCheatPlusMode(boolean enable) { nocheatplusMode = enable; }
    public static void setIntensity(int level) { intensity = Math.max(1, Math.min(10, level)); }
    public static void setDesyncLevel(int level) { desyncLevel = Math.max(1, Math.min(5, level)); }
    
    public static boolean isGrimMode() { return grimMode; }
    public static boolean isVulcanMode() { return vulcanMode; }
    public static boolean isNoCheatPlusMode() { return nocheatplusMode; }
    public static int getIntensity() { return intensity; }
    public static int getDesyncLevel() { return desyncLevel; }
    public static class_2338 getCurrentTarget() { return currentTarget; }
    public static int getPacketCount() { return packetCounter; }
    
    public static String getStatus() {
        if (!isActive()) return "OFF";
        return String.format("G:%s V:%s N:%s I:%d D:%d",
            grimMode ? "ON" : "OFF",
            vulcanMode ? "ON" : "OFF",
            nocheatplusMode ? "ON" : "OFF",
            intensity,
            desyncLevel
        );
    }
}