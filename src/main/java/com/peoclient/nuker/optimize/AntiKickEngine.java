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
        if (!isActive() || mc.field_1724 == null || mc.field_1687 == null) return;

        antiKickTick++;
        breakCounter++;

        // Micro-pause ngẫu nhiên (1-2 ticks) sau mỗi 3-5 lần đào thành công
        if (breakCounter % 4 == 0 && RANDOM.nextInt(3) == 0) {
            // Chỉ tạm dừng 1 tick – không ảnh hưởng nhiều đến tốc độ
            isPaused = true;
            pauseTicks = 1 + RANDOM.nextInt(2); // 1-2 ticks
        }

        if (isPaused) {
            pauseTicks--;
            if (pauseTicks <= 0) isPaused = false;
        }

        // Thay đổi rotation nhẹ mỗi tick để không bị phát hiện pattern
        if (mc.field_1724 != null) {
            float yaw = mc.field_1724.method_36454();
            float pitch = mc.field_1724.method_36455();
            yaw += (RANDOM.nextFloat() - 0.5f) * 0.4f; // ±0.2 độ
            pitch += (RANDOM.nextFloat() - 0.5f) * 0.2f;
            // Giới hạn pitch trong khoảng -90..90
            pitch = Math.max(-90, Math.min(90, pitch));
            mc.field_1724.method_36456(yaw);
            mc.field_1724.method_36457(pitch);
        }

        // Observation only. Never pause Nuker, alter rotation, or alter cooldown.
        if (useHealthCheck && antiKickTick % 20 == 0) checkHealth(mc);
        if (usePingMonitoring && antiKickTick % 10 == 0) monitorPing(mc);
        if (antiKickTick % 20 == 0) monitorServerResponses();
        if (antiKickTick % 40 == 0) checkSuspiciousActivity();
        adjustProtectionLevel();
    }

    private static void checkHealth(class_310 mc) {
        if (mc.field_1724 == null) return;
        float health = mc.field_1724.method_6032();
        int food = mc.field_1724.method_7344().method_7586();
        if (health < 5.0f || food < 5) {
            DiagnosticRecorder.get().record("AntiKickEngine",
                    "Low health/food observed: H=" + health + " F=" + food);
        }
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
        // Telemetry only. Never translate local success-rate statistics into
        // pauses, cooldowns, rotation changes, or other Nuker behaviour.
        int attempts = AccountSessionMetrics.get().getBreakAttempts();
        int successes = AccountSessionMetrics.get().getBreakSuccesses();

        if (attempts > 100) {
            int rate = (int) Math.max(0, Math.min(100,
                    ((double) successes / Math.max(1, attempts)) * 100.0));
            lastSuccessRate = rate;
            if (rate > 95) {
                consecutiveSuccess = Math.min(consecutiveSuccess + 1, 1000);
            } else {
                consecutiveSuccess = Math.max(0, consecutiveSuccess - 1);
            }
        }
    }

    private static void adjustProtectionLevel() {
        // Tự động điều chỉnh protection level
        if (protectionLevel < 5) {
            protectionLevel = 5;
        }
    }
    
    // ===== Getters/Setters =====
    public static boolean shouldPause() { return false; }
    
    public static int getDynamicCooldown() {
        // Preserve Nuker strength/speed: compatibility adds no cooldown.
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
    public static int getLastSuccessRate() { return lastSuccessRate; } // <-- THÊM DÒNG NÀY
    
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