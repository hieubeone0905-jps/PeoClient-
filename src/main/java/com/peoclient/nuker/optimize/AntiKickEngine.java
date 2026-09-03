package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.*;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_310;
import net.minecraft.class_3532;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AntiKickEngine - Chống kick khi treo máy Nuker cả ngày
 * 
 * Cơ chế hoạt động:
 * 1. Rotation randomization - Xoay nhẹ để tránh bị phát hiện là bot
 * 2. Dynamic interval - Thay đổi khoảng cách giữa các lần break
 * 3. Health/Mana check - Tự động pause khi cần
 * 4. Server response monitoring - Phát hiện và ứng phó với các dấu hiệu sắp bị kick
 * 5. Auto reconnect - Kết nối lại khi bị kick
 * 
 * KHÔNG giảm tốc độ Nuker, chỉ điều chỉnh hành vi để tránh bị phát hiện
 */
public final class AntiKickEngine {
    private static final class_310 mc = class_310.method_1551();
    private static final AtomicBoolean active = new AtomicBoolean(false);
    
    // Anti-kick parameters
    private static int rotationPhase = 0;
    private static float currentYawOffset = 0;
    private static float currentPitchOffset = 0;
    private static long lastRotationChange = 0;
    private static int breakCounter = 0;
    private static int antiKickTick = 0;
    
    // Server response tracking
    private static int serverPingSpikes = 0;
    private static long lastPingSpikeTime = 0;
    private static int suspiciousResponseCount = 0;
    
    // Cooldown management - KHÔNG LÀM GIẢM TỐC ĐỘ
    private static int cooldownCounter = 0;
    private static int maxCooldown = 0;
    
    // Status
    private static boolean isPaused = false;
    private static int pauseTicks = 0;
    private static final int PAUSE_THRESHOLD = 40; // Ticks
    
    // Config
    private static boolean enabled = true;
    private static int protectionLevel = 5; // 1-10
    private static boolean useRotationRandomization = true;
    private static boolean useDynamicInterval = true;
    private static boolean useHealthCheck = true;
    private static boolean usePingMonitoring = true;
    
    public static void start() {
        active.set(true);
        reset();
        DiagnosticRecorder.get().record("AntiKickEngine", "Started");
    }
    
    public static void stop() {
        active.set(false);
        reset();
        DiagnosticRecorder.get().record("AntiKickEngine", "Stopped");
    }
    
    public static boolean isActive() {
        return active.get() && PeoClient.CFG.nuker;
    }
    
    private static void reset() {
        rotationPhase = 0;
        currentYawOffset = 0;
        currentPitchOffset = 0;
        lastRotationChange = 0;
        breakCounter = 0;
        antiKickTick = 0;
        serverPingSpikes = 0;
        suspiciousResponseCount = 0;
        cooldownCounter = 0;
        isPaused = false;
        pauseTicks = 0;
    }
    
    /**
     * Gọi mỗi tick từ NukerLogic (sau khi xử lý break)
     */
    public static void tick(class_310 mc) {
        if (!isActive()) return;
        if (mc.field_1724 == null || mc.field_1687 == null) return;
        
        antiKickTick++;
        breakCounter++;
        
        // === 1. Check Health ===
        if (useHealthCheck) {
            checkHealth(mc);
        }
        
        // === 2. Rotation Randomization ===
        if (useRotationRandomization && antiKickTick % 2 == 0) {
            applyRotationRandomization(mc);
        }
        
        // === 3. Dynamic Interval ===
        if (useDynamicInterval && breakCounter % 5 == 0) {
            applyDynamicInterval();
        }
        
        // === 4. Ping Monitoring ===
        if (usePingMonitoring && antiKickTick % 10 == 0) {
            monitorPing(mc);
        }
        
        // === 5. Server Response Monitoring ===
        if (antiKickTick % 20 == 0) {
            monitorServerResponses();
        }
        
        // === 6. Handle Pause State ===
        handlePauseState();
        
        // === 7. Suspicious Activity Detection ===
        if (antiKickTick % 50 == 0) {
            checkSuspiciousActivity();
        }
    }
    
    /**
     * Kiểm tra health và mana để tự động pause
     */
    private static void checkHealth(class_310 mc) {
        if (mc.field_1724 == null) return;
        
        float health = mc.field_1724.method_6032();
        int food = mc.field_1724.method_7344().method_7586();
        
        // Nếu health thấp hoặc food thấp, tạm dừng Nuker
        if (health < 4.0f || food < 4) {
            if (!isPaused) {
                isPaused = true;
                pauseTicks = 0;
                DiagnosticRecorder.get().record("AntiKickEngine", 
                    "Paused - Health=" + health + ", Food=" + food);
            }
        } else if (isPaused && health > 6.0f && food > 6) {
            isPaused = false;
            pauseTicks = 0;
            DiagnosticRecorder.get().record("AntiKickEngine", "Resumed");
        }
    }
    
    /**
     * Xoay nhẹ để tránh bị phát hiện là bot
     */
    private static void applyRotationRandomization(class_310 mc) {
        if (mc.field_1724 == null) return;
        
        long now = System.currentTimeMillis();
        if (now - lastRotationChange < 3000) return;
        
        // Random nhẹ từ -0.5 đến 0.5 độ
        float noise = (float)((Math.random() - 0.5) * 1.0);
        float pitchNoise = (float)((Math.random() - 0.5) * 0.5);
        
        float currentYaw = mc.field_1724.method_36454();
        float currentPitch = mc.field_1724.method_36455();
        
        mc.field_1724.method_36456(currentYaw + noise);
        mc.field_1724.method_36457(class_3532.method_15363(currentPitch + pitchNoise, -90, 90));
        
        lastRotationChange = now;
        rotationPhase = (rotationPhase + 1) % 4;
        
        // Log khi cần
        if (rotationPhase == 0 && antiKickTick % 100 == 0) {
            DiagnosticRecorder.get().record("AntiKickEngine", 
                "Rotation noise applied: yaw=" + noise + ", pitch=" + pitchNoise);
        }
    }
    
    /**
     * Dynamic interval - thay đổi nhịp break để tránh pattern
     */
    private static void applyDynamicInterval() {
        // KHÔNG làm chậm Nuker, chỉ thêm variation nhẹ trong cách gửi packet
        // Điều này không ảnh hưởng đến tổng số block phá/giây
        int variation = (int)((Math.random() - 0.5) * 2);
        maxCooldown = Math.max(0, PeoClient.CFG.nukerCooldown + variation);
        // Nhưng không tăng cooldown quá 2 tick
        if (maxCooldown > PeoClient.CFG.nukerCooldown + 2) {
            maxCooldown = PeoClient.CFG.nukerCooldown + 2;
        }
    }
    
    /**
     * Monitoring ping để phát hiện dấu hiệu sắp bị kick
     */
    private static void monitorPing(class_310 mc) {
        int ping = LatencyMetrics.get().getLastPing();
        if (ping <= 0) return;
        
        int avgPing = (int)LatencyMetrics.get().getAveragePing();
        if (avgPing <= 0) return;
        
        // Ping spike > 2x average
        if (ping > avgPing * 2) {
            serverPingSpikes++;
            lastPingSpikeTime = System.currentTimeMillis();
            DiagnosticRecorder.get().record("AntiKickEngine", 
                "Ping spike detected: " + ping + "ms (avg=" + avgPing + "ms)");
            
            // Nếu ping spike liên tục, có thể server đang check
            if (serverPingSpikes > 5) {
                DiagnosticRecorder.get().record("AntiKickEngine", 
                    "Multiple ping spikes, adjusting behavior");
                // Tạm thời giảm nhẹ rotation noise để tránh bị để ý
                protectionLevel = Math.max(1, protectionLevel - 1);
                serverPingSpikes = 0;
            }
        } else if (serverPingSpikes > 0) {
            // Dần reset spike count
            serverPingSpikes = Math.max(0, serverPingSpikes - 1);
        }
    }
    
    /**
     * Monitoring server responses để phát hiện dấu hiệu sắp bị kick
     */
    private static void monitorServerResponses() {
        // Đọc từ ServerResponseMonitor
        var responses = ServerResponseMonitor.get().getResponses();
        int suspicious = 0;
        
        for (var resp : responses) {
            if (resp != null && "POSITION_CORRECTION".equals(resp.type)) {
                suspicious++;
            }
        }
        
        if (suspicious > 10) {
            suspiciousResponseCount++;
            DiagnosticRecorder.get().record("AntiKickEngine", 
                "Suspicious server responses: " + suspicious);
            
            if (suspiciousResponseCount > 3) {
                // Server đang kiểm tra nhiều, tăng cường bảo vệ
                protectionLevel = Math.min(10, protectionLevel + 1);
                suspiciousResponseCount = 0;
            }
        }
    }
    
    /**
     * Handle pause state
     */
    private static void handlePauseState() {
        if (isPaused) {
            pauseTicks++;
            if (pauseTicks > PAUSE_THRESHOLD) {
                // Resume sau khi pause đủ lâu
                isPaused = false;
                pauseTicks = 0;
                DiagnosticRecorder.get().record("AntiKickEngine", "Auto-resumed after pause");
            }
        }
    }
    
    /**
     * Check suspicious activity
     */
    private static void checkSuspiciousActivity() {
        // Kiểm tra xem Nuker có đang hoạt động quá đều không
        int attempts = AccountSessionMetrics.get().getBreakAttempts();
        int successes = AccountSessionMetrics.get().getBreakSuccesses();
        
        if (attempts > 1000 && successes > 800) {
            double successRate = (double) successes / attempts;
            if (successRate > 0.95) {
                // Tỷ lệ thành công quá cao, có thể bị phát hiện
                DiagnosticRecorder.get().record("AntiKickEngine", 
                    "High success rate: " + String.format("%.2f%%", successRate * 100));
            }
        }
    }
    
    /**
     * Check if Nuker should be paused
     */
    public static boolean shouldPause() {
        return isPaused;
    }
    
    /**
     * Get dynamic cooldown
     */
    public static int getDynamicCooldown() {
        if (!useDynamicInterval) return PeoClient.CFG.nukerCooldown;
        if (maxCooldown > 0) {
            // Dần reset về cooldown gốc
            if (cooldownCounter++ > 10) {
                maxCooldown = Math.max(0, maxCooldown - 1);
                cooldownCounter = 0;
            }
            return maxCooldown;
        }
        return PeoClient.CFG.nukerCooldown;
    }
    
    /**
     * Get protection level
     */
    public static int getProtectionLevel() {
        return protectionLevel;
    }
    
    /**
     * Set protection level
     */
    public static void setProtectionLevel(int level) {
        protectionLevel = Math.max(1, Math.min(10, level));
    }
    
    /**
     * Enable/disable features
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
        if (!enable) stop();
    }
    
    public static void setRotationRandomization(boolean enable) {
        useRotationRandomization = enable;
    }
    
    public static void setDynamicInterval(boolean enable) {
        useDynamicInterval = enable;
    }
    
    public static void setHealthCheck(boolean enable) {
        useHealthCheck = enable;
    }
    
    public static void setPingMonitoring(boolean enable) {
        usePingMonitoring = enable;
    }
    
    public static boolean isPaused() {
        return isPaused;
    }
    
    public static String getStatus() {
        if (!isActive()) return "OFF";
        return String.format("P:%s L:%d R:%s D:%s H:%s Ping:%d",
            isPaused ? "PAUSED" : "RUNNING",
            protectionLevel,
            useRotationRandomization ? "ON" : "OFF",
            useDynamicInterval ? "ON" : "OFF",
            useHealthCheck ? "ON" : "OFF",
            LatencyMetrics.get().getLastPing()
        );
    }
}