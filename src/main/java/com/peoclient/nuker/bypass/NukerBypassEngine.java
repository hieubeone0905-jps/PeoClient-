package com.peoclient.nuker.bypass;

import com.peoclient.PeoClient;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_239;
import net.minecraft.class_746;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Random;

/**
 * Nuker Bypass Engine – chạy song song với NukerLogic.
 * Gửi packet spoof rotation, position, và block break để đánh lừa Grim/Vulcan.
 * Không can thiệp vào logic break chính, giữ nguyên tốc độ Nuker.
 */
public final class NukerBypassEngine {
    private static final class_310 mc = class_310.method_1551();
    private static final Random RANDOM = new Random();
    
    private static Thread workerThread;
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static int intensity = 5;
    private static boolean grimMode = true;
    private static boolean vulcanMode = true;
    private static boolean enabled = false;
    
    // Trạng thái gửi packet
    private static float lastSpoofedYaw = 0;
    private static float lastSpoofedPitch = 0;
    private static double lastSpoofedX = 0;
    private static double lastSpoofedZ = 0;
    private static int tickCounter = 0;
    
    public static void setEnabled(boolean enable) {
        if (enable == enabled) return;
        enabled = enable;
        if (enable) {
            start();
        } else {
            stop();
        }
    }
    
    public static void setIntensity(int level) {
        intensity = Math.max(1, Math.min(10, level));
    }
    
    public static void setGrimMode(boolean on) {
        grimMode = on;
    }
    
    public static void setVulcanMode(boolean on) {
        vulcanMode = on;
    }
    
    private static void start() {
        if (workerThread != null && workerThread.isAlive()) return;
        running.set(true);
        workerThread = new Thread(NukerBypassEngine::loop, "NukerBypass-Engine");
        workerThread.setDaemon(true);
        workerThread.start();
    }
    
    private static void stop() {
        running.set(false);
        if (workerThread != null) {
            try {
                workerThread.interrupt();
                workerThread.join(1000);
            } catch (InterruptedException ignored) {}
            workerThread = null;
        }
        BypassPacketManager.clearQueue();
    }
    
    private static void loop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (mc.field_1724 == null || mc.field_1687 == null || !PeoClient.CFG.nuker) {
                    Thread.sleep(50);
                    continue;
                }
                
                tickCounter++;
                long start = System.nanoTime();
                
                // Lấy target hiện tại từ NukerLogic (nếu có)
                class_2338 target = getCurrentTarget();
                if (target == null) {
                    Thread.sleep(10);
                    continue;
                }
                
                // Phase rotation spoof
                if (tickCounter % 2 == 0) {
                    sendSpoofedRotation(target);
                }
                
                // Phase position spoof
                if (tickCounter % 3 == 0) {
                    sendSpoofedPosition();
                }
                
                // Phase block break spoof (đánh lạc hướng)
                if (tickCounter % 7 == 0) {
                    sendSpoofedBlockBreak(target);
                }
                
                // Reset định kỳ
                if (tickCounter % 200 == 0) {
                    BypassPacketManager.sendResetPacket();
                }
                
                // Delay động để tránh pattern
                long elapsed = System.nanoTime() - start;
                long baseDelay = 10 + RANDOM.nextInt(15);
                long delay = Math.max(1, baseDelay - elapsed / 1_000_000);
                Thread.sleep(delay);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Silent
            }
        }
    }
    
    private static class_2338 getCurrentTarget() {
        // Lấy target từ breakingPos trong NukerLogic (dùng reflection hoặc getter)
        // Vì PeoClient.NukerLogic.breakingPos là private static, ta dùng reflection
        try {
            java.lang.reflect.Field field = PeoClient.NukerLogic.class.getDeclaredField("breakingPos");
            field.setAccessible(true);
            return (class_2338) field.get(null);
        } catch (Exception e) {
            // Không dùng fallback raycast mapping phụ thuộc phiên bản;
            // NukerLogic là nguồn target chính.
            return null;
        }
    }
    
    private static void sendSpoofedRotation(class_2338 target) {
        if (mc.field_1724 == null) return;
        class_746 player = mc.field_1724;
        
        // Tính góc cần xoay
        class_243 eye = player.method_33571();
        class_243 center = class_243.method_24953(target);
        class_243 diff = center.method_1020(eye);
        double horiz = Math.sqrt(diff.field_1352 * diff.field_1352 + diff.field_1350 * diff.field_1350);
        float targetYaw = (float) Math.toDegrees(Math.atan2(diff.field_1350, diff.field_1352)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.field_1351, horiz));
        
        float currentYaw = player.method_36454();
        float currentPitch = player.method_36455();
        
        float yawDiff = class_3532.method_15393(targetYaw - currentYaw);
        float pitchDiff = class_3532.method_15393(targetPitch - currentPitch);
        
        float maxStep = grimMode ? 22.5f : 30.0f;
        float noise = (float)(Math.sin(System.currentTimeMillis() / 600.0) * 0.12);
        float step = maxStep * (0.6f + (intensity / 10.0f) * 0.4f) * (1.0f + noise);
        
        float newYaw = currentYaw + Math.max(-step, Math.min(step, yawDiff));
        float newPitch = currentPitch + Math.max(-step * 0.7f, Math.min(step * 0.7f, pitchDiff));
        
        newYaw += (float)((RANDOM.nextDouble() - 0.5) * 0.02);
        newPitch += (float)((RANDOM.nextDouble() - 0.5) * 0.02);
        newYaw = class_3532.method_15393(newYaw);
        newPitch = class_3532.method_15363(newPitch, -90, 90);
        
        // Gửi packet rotation
        BypassPacketManager.sendRotation(newYaw, newPitch, player.method_24828());
        
        lastSpoofedYaw = newYaw;
        lastSpoofedPitch = newPitch;
    }
    
    private static void sendSpoofedPosition() {
        if (mc.field_1724 == null) return;
        class_746 player = mc.field_1724;
        
        double noise = 0.00008 * intensity;
        double offsetX = Math.sin(tickCounter / 30.0 + 1.2) * noise;
        double offsetZ = Math.cos(tickCounter / 30.0 + 0.7) * noise;
        double offsetY = Math.sin(tickCounter / 25.0 + 2.1) * noise * 0.5;
        
        double x = player.method_23317() + offsetX + (RANDOM.nextDouble() - 0.5) * 0.00005;
        double y = player.method_23318() + offsetY + (RANDOM.nextDouble() - 0.5) * 0.00003;
        double z = player.method_23321() + offsetZ + (RANDOM.nextDouble() - 0.5) * 0.00005;
        
        BypassPacketManager.sendPosition(x, y, z, player.method_24828());
        lastSpoofedX = x;
        lastSpoofedZ = z;
    }
    
    private static void sendSpoofedBlockBreak(class_2338 target) {
        if (mc.field_1724 == null || mc.field_1687 == null) return;
        // Gửi packet ABORT_DESTROY_BLOCK cho một block khác để đánh lạc hướng
        class_2338 center = class_2338.method_49638(mc.field_1724.method_33571());
        for (int i = 0; i < 3; i++) {
            class_2338 spoofPos = center.method_10069(
                RANDOM.nextInt(5) - 2,
                RANDOM.nextInt(3) - 1,
                RANDOM.nextInt(5) - 2
            );
            if (!mc.field_1687.method_8320(spoofPos).method_26215()) {
                class_2350 side = class_2350.values()[RANDOM.nextInt(class_2350.values().length)];
                BypassPacketManager.sendBlockAction(spoofPos, side);
                break;
            }
        }
    }
    
    public static boolean isEnabled() {
        return enabled && running.get();
    }
    
    public static int getSuspicionLevel() {
        // Dựa trên số lần gửi packet và độ trễ, có thể trả về 0-10
        return 0; // tạm thời
    }
    
    public static void reset() {
        stop();
        tickCounter = 0;
    }
}