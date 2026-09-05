package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.*;
import net.minecraft.class_310;
import net.minecraft.class_3532;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AntiKickEngine {
    private static final class_310 mc = class_310.method_1551();
    private static final Random RANDOM = new Random();
    private static final AtomicBoolean active = new AtomicBoolean(false);

    private static int breakCounter = 0;
    private static int antiKickTick = 0;
    private static boolean isPaused = false;
    private static int pauseTicks = 0;
    private static int successCount = 0;
    private static int failCount = 0;
    private static int protectionLevel = 6;
    private static int consecutiveSuccess = 0;
    private static int lastSuccessRate = 100;
    private static long lastLogTime = 0;

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
        breakCounter = 0;
        antiKickTick = 0;
        isPaused = false;
        pauseTicks = 0;
        successCount = 0;
        failCount = 0;
        consecutiveSuccess = 0;
        lastSuccessRate = 100;
    }

    public static void tick(class_310 mc) {
        if (!isActive() || mc.field_1724 == null || mc.field_1687 == null) return;

        antiKickTick++;
        breakCounter++;

        // --- MICRO-PAUSE: sau mỗi 2-3 lần đào thành công, pause 2-4 ticks ---
        if (breakCounter % (2 + RANDOM.nextInt(2)) == 0 && RANDOM.nextInt(3) != 0) {
            isPaused = true;
            pauseTicks = 2 + RANDOM.nextInt(3); // 2-4 ticks
            if (RANDOM.nextInt(5) == 0) {
                DiagnosticRecorder.get().record("AntiKickEngine",
                        "Micro-pause " + pauseTicks + " ticks (breakCounter=" + breakCounter + ")");
            }
        }

        if (isPaused) {
            pauseTicks--;
            if (pauseTicks <= 0) isPaused = false;
        }

        // --- ROTATION RANDOMIZATION: thay đổi góc nhìn liên tục ---
        if (mc.field_1724 != null && RANDOM.nextInt(3) != 0) {
            float yaw = mc.field_1724.method_36454();
            float pitch = mc.field_1724.method_36455();
            yaw += (RANDOM.nextFloat() - 0.5f) * 0.6f; // ±0.3 độ
            pitch += (RANDOM.nextFloat() - 0.5f) * 0.3f;
            pitch = Math.max(-90, Math.min(90, pitch));
            mc.field_1724.method_36456(yaw);
            mc.field_1724.method_36457(pitch);
        }

        // --- SIMULATE FAILURE: cứ 10-15 khối thành công, tạo 1 lần fail giả ---
        if (breakCounter % (10 + RANDOM.nextInt(6)) == 0 && breakCounter > 10) {
            failCount++;
            if (RANDOM.nextInt(4) == 0) {
                DiagnosticRecorder.get().record("AntiKickEngine",
                        "Simulated failure #" + failCount + " (rate " + getSuccessRate() + "%)");
            }
        } else {
            successCount++;
        }

        // --- THEO DÕI TỶ LỆ THÀNH CÔNG ẢO ---
        if (antiKickTick % 40 == 0) {
            int rate = getSuccessRate();
            lastSuccessRate = rate;
            if (rate > 98) {
                protectionLevel = Math.min(10, protectionLevel + 1);
            } else if (rate < 85) {
                protectionLevel = Math.max(1, protectionLevel - 1);
            }
            if (antiKickTick % 80 == 0) {
                DiagnosticRecorder.get().record("AntiKickEngine",
                        "Simulated success rate: " + rate + "%");
            }
        }

        // --- CHECK SỨC KHỎE ---
        if (antiKickTick % 100 == 0) {
            float health = mc.field_1724.method_6032();
            int food = mc.field_1724.method_7344().method_7586();
            if (health < 4.0f || food < 3) {
                DiagnosticRecorder.get().record("AntiKickEngine",
                        "Low health/food: H=" + health + " F=" + food);
            }
        }
    }

    private static int getSuccessRate() {
        int total = successCount + failCount;
        if (total == 0) return 100;
        return (int) ((double) successCount / total * 100);
    }

    public static boolean shouldPause() {
        return isPaused;
    }

    public static int getDynamicCooldown() {
        return isPaused ? 0 : 0;
    }

    public static int getProtectionLevel() {
        return protectionLevel;
    }

    public static void setProtectionLevel(int level) {
        protectionLevel = Math.max(1, Math.min(10, level));
    }

    public static String getStatus() {
        if (!isActive()) return "OFF";
        return "P:" + (isPaused ? "PAUSED" : "RUN") +
               " L:" + protectionLevel +
               " R:" + lastSuccessRate + "%" +
               " C:" + breakCounter;
    }
}