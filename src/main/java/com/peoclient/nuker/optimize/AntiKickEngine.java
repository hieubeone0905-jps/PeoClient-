package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.*;
import net.minecraft.class_310;
import net.minecraft.class_3532;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AntiKickEngine - Tạo ngẫu nhiên và phá vỡ pattern để tránh kick.
 * Không làm giảm tốc độ đào đáng kể.
 */
public final class AntiKickEngine {
    private static final class_310 mc = class_310.method_1551();
    private static final Random RANDOM = new Random();
    private static final AtomicBoolean active = new AtomicBoolean(false);

    // Trạng thái
    private static int antiKickTick = 0;
    private static int breakCounter = 0;
    private static int pauseTicks = 0;
    private static boolean isPaused = false;
    private static int protectionLevel = 7;
    private static int consecutiveSuccess = 0;
    private static int lastSuccessRate = 100;

    private static boolean enabled = true;

    public static void start() {
        active.set(true);
        reset();
        DiagnosticRecorder.get().record("AntiKickEngine", "Started (randomized pattern)");
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
        antiKickTick = 0;
        breakCounter = 0;
        pauseTicks = 0;
        isPaused = false;
        consecutiveSuccess = 0;
        lastSuccessRate = 100;
    }

    public static void tick(class_310 mc) {
        if (!isActive() || mc.field_1724 == null || mc.field_1687 == null) return;

        antiKickTick++;
        breakCounter++;

        // Tạo pause ngẫu nhiên: cứ 3-8 lần đào thì nghỉ 1-3 ticks
        if (breakCounter % (3 + RANDOM.nextInt(6)) == 0 && RANDOM.nextInt(4) == 0) {
            isPaused = true;
            pauseTicks = 1 + RANDOM.nextInt(3);
            DiagnosticRecorder.get().record("AntiKickEngine",
                    "Micro-pause " + pauseTicks + " ticks (breakCounter=" + breakCounter + ")");
        }

        if (isPaused) {
            pauseTicks--;
            if (pauseTicks <= 0) isPaused = false;
        }

        // Xoay nhẹ ngẫu nhiên (0.1-0.5 độ) để phá pattern, nhưng không ảnh hưởng đến hướng đào
        if (mc.field_1724 != null && RANDOM.nextInt(3) == 0) {
            float yaw = mc.field_1724.method_36454();
            float pitch = mc.field_1724.method_36455();
            yaw += (RANDOM.nextFloat() - 0.5f) * 0.6f; // ±0.3 độ
            pitch += (RANDOM.nextFloat() - 0.5f) * 0.3f;
            pitch = Math.max(-90, Math.min(90, pitch));
            mc.field_1724.method_36456(yaw);
            mc.field_1724.method_36457(pitch);
        }

        // Giả lập tỉ lệ thành công 90-95% để tránh bị phát hiện bot
        if (breakCounter % 20 == 0) {
            int rate = 90 + RANDOM.nextInt(6); // 90-95%
            lastSuccessRate = rate;
            DiagnosticRecorder.get().record("AntiKickEngine",
                    "Simulated success rate: " + rate + "%");
        }

        // Điều chỉnh protection level dựa trên ping và success rate ảo
        int ping = LatencyMetrics.get().getLastPing();
        if (ping > 0 && ping > 100) {
            protectionLevel = Math.min(10, protectionLevel + 1);
        } else if (ping > 0 && ping < 50) {
            protectionLevel = Math.max(1, protectionLevel - 1);
        }

        // Không bao giờ để protectionLevel quá thấp
        if (protectionLevel < 3) protectionLevel = 3;
    }

    // === Getters/Setters ===
    public static boolean shouldPause() { return isPaused; }
    public static boolean isPaused() { return isPaused; }
    public static int getDynamicCooldown() {
        // Không thêm cooldown thực tế, chỉ trả về 0 để giữ tốc độ
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
    public static int getConsecutiveSuccess() { return consecutiveSuccess; }
    public static int getLastSuccessRate() { return lastSuccessRate; }
    public static String getStatus() {
        if (!isActive()) return "OFF";
        return String.format("P:%s L:%d R:%d%% Ping:%d",
            isPaused ? "PAUSED" : "RUNNING",
            protectionLevel,
            lastSuccessRate,
            LatencyMetrics.get().getLastPing()
        );
    }
}