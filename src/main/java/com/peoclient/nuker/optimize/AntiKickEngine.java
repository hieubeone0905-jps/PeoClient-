package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.*;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_310;
import net.minecraft.class_3532;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final int PAUSE_THRESHOLD = 40;
    private static int microPauseTicks = 0;
    private static final int MICRO_PAUSE_INTERVAL = 60; // Sau 60 ticks, pause 1 tick
    
    // Config
    private static boolean enabled = true;
    private static int protectionLevel = 7; // Tăng lên 7
    private static boolean useRotationRandomization = true;
    private static boolean useDynamicInterval = true;
    private static boolean useHealthCheck = true;
    private static boolean usePingMonitoring = true;
    private static boolean useMicroPause = true;
    
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
        return enabled && active.get() && PeoClient.CFG.nuker;
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
        microPauseTicks = 0;
    }
    
    public static void tick(class_310 mc) {
        if (!isActive()) return;
        if (mc.field_1724 == null || mc.field_1687 == null) return;
        
        antiKickTick++;
        breakCounter++;
        
        // === Micro-pause: dừng 1 tick mỗi 60 tick để tránh pattern ===
        if (useMicroPause) {
            microPauseTicks++;
            if (microPauseTicks >= MICRO_PAUSE_INTERVAL) {
                microPauseTicks = 0;
                if (!isPaused) {
                    isPaused = true;
                    pauseTicks = 0;
                    DiagnosticRecorder.get().record("AntiKickEngine", "Micro-pause triggered");
                }
            }
        }
        
        // === 1. Check Health ===
        if (useHealthCheck) {
            checkHealth(mc);
        }
        
        // === 2. Rotation Randomization ===
        if (useRotationRandomization && antiKickTick % 2 == 0) {
            applyRotationRandomization(mc);
        }
        
        // === 3. Dynamic Interval ===
        if (useDynamicInterval && breakCounter % 3 == 0) { // Giảm xuống 3
            applyDynamicInterval();
        }
        
        // === 4. Ping Monitoring ===
        if (usePingMonitoring && antiKickTick % 5 == 0) { // Tăng tần suất
            monitorPing(mc);
        }
        
        // === 5. Server Response Monitoring ===
        if (antiKickTick % 10 == 0) { // Tăng tần suất
            monitorServerResponses();
        }
        
        // === 6. Handle Pause State ===
        handlePauseState();
        
        // === 7. Suspicious Activity Detection ===
        if (antiKickTick % 30 == 0) { // Tăng tần suất
            checkSuspiciousActivity();
        }
    }
    
    private static void checkHealth(class_310 mc) {
        if (mc.field_1724 == null) return;
        
        float health = mc.field_1724.method_6032();
        int food = mc.field_1724.method_7344().method_7586();
        
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
    
    private static void applyRotationRandomization(class_310 mc) {
        if (mc.field_1724 == null) return;
        
        long now = System.currentTimeMillis();
        if (now - lastRotationChange < 2000) return; // Giảm xuống 2s
        
        // Random mạnh hơn từ -1.0 đến 1.0 độ
        float noise = (float)((Math.random() - 0.5) * 2.0);
        float pitchNoise = (float)((Math.random() - 0.5) * 1.0);
        
        float currentYaw = mc.field_1724.method_36454();
        float currentPitch = mc.field_1724.method_36455();
        
        mc.field_1724.method_36456(currentYaw + noise);
        mc.field_1724.method_36457(class_3532.method_15363(currentPitch + pitchNoise, -90, 90));
        
        lastRotationChange = now;
        rotationPhase = (rotationPhase + 1) % 4;
        
        if (rotationPhase == 0 && antiKickTick % 50 == 0) {
            DiagnosticRecorder.get().record("AntiKickEngine", 
                "Rotation noise applied: yaw=" + noise + ", pitch=" + pitchNoise);
        }
    }
    
    private static void applyDynamicInterval() {
        // Variation rộng hơn từ -3 đến 3
        int variation = (int)((Math.random() - 0.5) * 6);
        maxCooldown = Math.max(0, PeoClient.CFG.nukerCooldown + variation);
        if (maxCooldown > PeoClient.CFG.nukerCooldown + 3) {
            maxCooldown = PeoClient.CFG.nukerCooldown + 3;
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
                "Ping spike detected: " + ping + "ms (avg=" + avgPing + "ms)");
            
            if (serverPingSpikes > 3) {
                DiagnosticRecorder.get().record("AntiKickEngine", 
                    "Multiple ping spikes, adjusting behavior");
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
        
        if (suspicious > 5) { // Giảm ngưỡng
            suspiciousResponseCount++;
            DiagnosticRecorder.get().record("AntiKickEngine", 
                "Suspicious server responses: " + suspicious);
            
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
                DiagnosticRecorder.get().record("AntiKickEngine", "Auto-resumed after pause");
            }
        }
    }
    
    private static void checkSuspiciousActivity() {
        int attempts = AccountSessionMetrics.get().getBreakAttempts();
        int successes = AccountSessionMetrics.get().getBreakSuccesses();
        
        if (attempts > 500 && successes > 400) {
            double successRate = (double) successes / attempts;
            if (successRate > 0.95) {
                DiagnosticRecorder.get().record("AntiKickEngine", 
                    "High success rate: " + String.format("%.2f%%", successRate * 100));
                // Tăng cường rotation noise
                if (protectionLevel < 8) protectionLevel++;
            }
        }
    }
    
    public static boolean shouldPause() {
        return isPaused;
    }
    
    public static int getDynamicCooldown() {
        if (!useDynamicInterval) return PeoClient.CFG.nukerCooldown;
        if (maxCooldown > 0) {
            if (cooldownCounter++ > 8) {
                maxCooldown = Math.max(0, maxCooldown - 1);
                cooldownCounter = 0;
            }
            return maxCooldown;
        }
        return PeoClient.CFG.nukerCooldown;
    }
    
    public static int getProtectionLevel() {
        return protectionLevel;
    }
    
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
    
    public static boolean isPaused() {
        return isPaused;
    }
    
    public static String getStatus() {
        if (!isActive()) return "OFF";
        return String.format("P:%s L:%d R:%s D:%s H:%s M:%s Ping:%d",
            isPaused ? "PAUSED" : "RUNNING",
            protectionLevel,
            useRotationRandomization ? "ON" : "OFF",
            useDynamicInterval ? "ON" : "OFF",
            useHealthCheck ? "ON" : "OFF",
            useMicroPause ? "ON" : "OFF",
            LatencyMetrics.get().getLastPing()
        );
    }
}