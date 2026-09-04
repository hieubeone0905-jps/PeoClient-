package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.*;
import net.minecraft.class_310;
import net.minecraft.class_3532;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AntiKickEngine - Chống kick khi treo máy Nuker cả ngày
 * Tối ưu hóa để tránh bị phát hiện bởi Grim/Vulcan
 */
public final class AntiKickEngine {
    private static final class_310 mc = class_310.method_1551();
    private static final Random RANDOM = new Random();
    private static final AtomicBoolean active = new AtomicBoolean(false);
    
    // Anti-kick parameters - ĐIỀU CHỈNH MẠNH HƠN
    private static int rotationPhase = 0;
    private static long lastRotationChange = 0;
    private static int breakCounter = 0;
    private static int antiKickTick = 0;
    
    // Server response tracking
    private static int serverPingSpikes = 0;
    private static long lastPingSpikeTime = 0;
    private static int suspiciousResponseCount = 0;
    
    // Status
    private static boolean isPaused = false;
    private static int pauseTicks = 0;
    private static final int PAUSE_THRESHOLD = 60;
    private static int microPauseTicks = 0;
    private static final int MICRO_PAUSE_INTERVAL = 45; // Giảm xuống 45 để tránh pattern
    
    // Config - ĐIỀU CHỈNH
    private static boolean enabled = true;
    private static int protectionLevel = 8; // Tăng từ 7 lên 8
    private static boolean useRotationRandomization = true;
    private static boolean useDynamicInterval = true;
    private static boolean useHealthCheck = true;
    private static boolean usePingMonitoring = true;
    private static boolean useMicroPause = true;
    
    // Phát hiện pattern đều (dấu hiệu bot)
    private static int consecutiveSuccess = 0;
    private static int lastSuccessRate = 0;
    
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
        lastRotationChange = 0;
        breakCounter = 0;
        antiKickTick = 0;
        serverPingSpikes = 0;
        suspiciousResponseCount = 0;
        isPaused = false;
        pauseTicks = 0;
        microPauseTicks = 0;
        consecutiveSuccess = 0;
        lastSuccessRate = 0;
    }
    
    /**
     * Gọi mỗi tick từ NukerLogic
     */
    public static void tick(class_310 mc) {
        if (!isActive()) return;
        if (mc.field_1724 == null || mc.field_1687 == null) return;
        antiKickTick++;

        // Monitor-only by design. Do not rotate, pause, spoof, or alter the
        // Nuker action stream. This keeps the break engine deterministic and
        // avoids client-side recovery loops interfering with vanilla breaking.
        if (usePingMonitoring && antiKickTick % 20 == 0) monitorPing(mc);
        if (antiKickTick % 40 == 0) monitorServerResponses();
    }
    
    private static void checkHealth(class_310 mc) {
        if (mc.field_1724 == null) return;
        
        float health = mc.field_1724.method_6032();
        int food = mc.field_1724.method_7344().method_7586();
        
        if (health < 5.0f || food < 5) {
            if (!isPaused) {
                isPaused = true;
                pauseTicks = 0;
                DiagnosticRecorder.get().record("AntiKickEngine", 
                    "Paused - Health=" + health + ", Food=" + food);
            }
        } else if (isPaused && health > 7.0f && food > 7) {
            isPaused = false;
            pauseTicks = 0;
            DiagnosticRecorder.get().record("AntiKickEngine", "Resumed");
        }
    }
    
    private static void applyRotationRandomization(class_310 mc) {
        if (mc.field_1724 == null) return;
        
        long now = System.currentTimeMillis();
        if (now - lastRotationChange < 1500) return; // Giảm xuống 1.5s
        
        // Noise mạnh hơn, random theo Gaussian
        float yawNoise = (float)(RANDOM.nextGaussian() * 0.8);
        float pitchNoise = (float)(RANDOM.nextGaussian() * 0.4);
        
        float currentYaw = mc.field_1724.method_36454();
        float currentPitch = mc.field_1724.method_36455();
        
        mc.field_1724.method_36456(currentYaw + yawNoise);
        mc.field_1724.method_36457(class_3532.method_15363(currentPitch + pitchNoise, -90, 90));
        
        lastRotationChange = now;
        rotationPhase = (rotationPhase + 1) % 3;
        
        if (rotationPhase == 0 && antiKickTick % 30 == 0) {
            DiagnosticRecorder.get().record("AntiKickEngine", 
                "Rotation noise: yaw=" + String.format("%.2f", yawNoise) + 
                ", pitch=" + String.format("%.2f", pitchNoise));
        }
    }
    
    private static void applyDynamicInterval() {
        // Variation rộng hơn
        int variation = (int)((RANDOM.nextDouble() - 0.5) * 8);
        int base = PeoClient.CFG.nukerCooldown;
        int newCooldown = Math.max(0, base + variation);
        
        // Lưu vào static để NukerLogic đọc
        // (cần thêm getter)
    }
    
    private static void monitorPing(class_310 mc) {
        int ping = LatencyMetrics.get().getLastPing();
        if (ping <= 0) return;
        
        int avgPing = (int)LatencyMetrics.get().getAveragePing();
        if (avgPing <= 0) return;
        
        if (ping > avgPing * 2) {
            serverPingSpikes++;
            lastPingSpikeTime = System.currentTimeMillis();
            DiagnosticRecorder.get().record("AntiKickEngine", 
                "Ping spike: " + ping + "ms (avg=" + avgPing + "ms)");
            
            if (serverPingSpikes > 3) {
                protectionLevel = Math.max(1, protectionLevel - 1);
                serverPingSpikes = 0;
            }
        } else if (serverPingSpikes > 0) {
            serverPingSpikes = Math.max(0, serverPingSpikes - 1);
        }
    }
    
    private static void monitorServerResponses() {
        var responses = ServerResponseMonitor.get().getResponses();
        int suspicious = 0;
        
        for (var resp : responses) {
            if (resp != null && "POSITION_CORRECTION".equals(resp.type)) {
                suspicious++;
            }
        }
        
        if (suspicious > 4) {
            suspiciousResponseCount++;
            DiagnosticRecorder.get().record("AntiKickEngine", 
                "Suspicious responses: " + suspicious);
            
            if (suspiciousResponseCount > 2) {
                protectionLevel = Math.min(10, protectionLevel + 1);
                suspiciousResponseCount = 0;
            }
        }
    }
    
    private static void handlePauseState() {
        if (isPaused) {
            pauseTicks++;
            if (pauseTicks > PAUSE_THRESHOLD) {
                isPaused = false;
                pauseTicks = 0;
                DiagnosticRecorder.get().record("AntiKickEngine", "Auto-resumed");
            }
        }
    }
    
    private static void checkSuspiciousActivity() {
        int attempts = AccountSessionMetrics.get().getBreakAttempts();
        int successes = AccountSessionMetrics.get().getBreakSuccesses();
        
        if (attempts > 100 && successes > 80) {
            double successRate = (double) successes / attempts;
            int rate = (int)(successRate * 100);
            
            // Nếu success rate quá cao (> 95%), server có thể đang theo dõi
            if (rate > 95) {
                consecutiveSuccess++;
                DiagnosticRecorder.get().record("AntiKickEngine", 
                    "High success rate: " + rate + "% (consecutive=" + consecutiveSuccess + ")");
                
                if (consecutiveSuccess > 3) {
                    // Tăng cường bảo vệ
                    protectionLevel = Math.min(10, protectionLevel + 1);
                    // Tạo micro-pause bất thường để phá pattern
                    if (!isPaused) {
                        isPaused = true;
                        pauseTicks = 0;
                        DiagnosticRecorder.get().record("AntiKickEngine", 
                            "Emergency pause due to high success rate");
                    }
                    consecutiveSuccess = 0;
                }
            } else {
                consecutiveSuccess = Math.max(0, consecutiveSuccess - 1);
            }
            
            lastSuccessRate = rate;
        }
    }
    
    private static void adjustProtectionLevel() {
        // Tự động điều chỉnh protection level
        if (protectionLevel < 5) {
            protectionLevel = 5;
        }
    }
    
    // ===== Getters/Setters =====
    public static boolean shouldPause() { return isPaused; }
    
    public static int getDynamicCooldown() {
        return 0;
    }
    
    public static int getProtectionLevel() { return protectionLevel; }
    public static void setProtectionLevel(int level) {
        protectionLevel = Math.max(1, Math.min(10, level));
    }
    
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
    
    public static void setMicroPause(boolean enable) {
        useMicroPause = enable;
    }
    
    public static boolean isPaused() { return isPaused; }
    public static int getConsecutiveSuccess() { return consecutiveSuccess; }
    public static int getLastSuccessRate() { return lastSuccessRate; }
    
    public static String getStatus() {
        if (!isActive()) return "OFF";
        return String.format("P:%s L:%d R:%s D:%s H:%s M:%s S:%d%% Ping:%d",
            isPaused ? "PAUSED" : "RUNNING",
            protectionLevel,
            useRotationRandomization ? "ON" : "OFF",
            useDynamicInterval ? "ON" : "OFF",
            useHealthCheck ? "ON" : "OFF",
            useMicroPause ? "ON" : "OFF",
            lastSuccessRate,
            LatencyMetrics.get().getLastPing()
        );
    }
}