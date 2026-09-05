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

    // Phần còn lại giữ nguyên (monitor ping, server responses, ...)
    // ...
}
    // Compatibility methods required by the existing client UI/core.
    // Return neutral values so the existing Nuker configuration remains unchanged.
    public static int getDynamicCooldown() {
        return 0;
    }

    public static String getStatus() {
        return isActive() ? "ON" : "OFF";
    }

    public static int getProtectionLevel() {
        return protectionLevel;
    }

    public static void setProtectionLevel(int value) {
        protectionLevel = Math.max(1, Math.min(10, value));
    }

}
