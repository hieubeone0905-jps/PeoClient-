package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_239;
import net.minecraft.class_2885;
import net.minecraft.class_1268;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class AutoBlockReload {
    private static final class_310 mc = class_310.method_1551();
    private static final ConcurrentLinkedQueue<class_2338> reloadQueue = new ConcurrentLinkedQueue<>();
    private static boolean active = false;
    private static int checkInterval = 0;
    private static long lastReloadTime = 0;
    private static final int RELOAD_COOLDOWN_MS = 50; // Giảm xuống 50ms để reload nhanh hơn
    private static class_2338 lastGhostPos = null;
    private static int ghostCheckCount = 0;

    public static void start() {
        active = true;
        reloadQueue.clear();
        lastReloadTime = 0;
        DiagnosticRecorder.get().record("AutoBlockReload", "Started");
    }

    public static void stop() {
        active = false;
        reloadQueue.clear();
        DiagnosticRecorder.get().record("AutoBlockReload", "Stopped");
    }

    public static boolean isActive() {
        return active && PeoClient.CFG.nuker;
    }

    public static void tick() {
        if (!isActive()) return;
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) return;

        checkInterval++;

        // Mỗi 2 tick kiểm tra ghost block (nhanh hơn)
        if (checkInterval % 2 == 0) {
            detectAndReloadGhostBlocks();
        }

        // Xử lý reload queue ngay lập tức nếu có
        if (!reloadQueue.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - lastReloadTime > RELOAD_COOLDOWN_MS) {
                class_2338 pos = reloadQueue.poll();
                if (pos != null) {
                    performBlockReload(pos);
                    lastReloadTime = now;
                }
            }
        }
    }

    private static void detectAndReloadGhostBlocks() {
        if (mc.field_1724 == null || mc.field_1687 == null) return;

        class_2338 target = getCurrentTarget();
        if (target == null) return;

        class_2680 state = mc.field_1687.method_8320(target);
        boolean clientSeesAir = state.method_26215();

        // Ghost detection: ưu tiên target đang đào, sau đó kiểm tra raycast để bắt lệch target.
        if (clientSeesAir) {
            boolean breaking = PeoClient.NukerLogic.getBreakingProgress() > 0.0f;
            class_2338 reloadPos = target;
            if (!breaking && mc.field_1724 != null) {
                net.minecraft.class_239 rayHit = mc.field_1724.method_5745(PeoClient.CFG.nukerRange + 1.0, 0.0f, false);
                if (rayHit instanceof class_3965 hit && hit.method_17783() == class_239.class_240.field_1332) {
                    class_2338 hitPos = hit.method_17777();
                    if (!hitPos.equals(target)) reloadPos = hitPos;
                }
            }
            if (reloadPos != null && (breaking || !reloadPos.equals(target))) {
                if (!reloadQueue.contains(reloadPos)) {
                    reloadQueue.add(reloadPos);
                    DiagnosticRecorder.get().record("AutoBlockReload", 
                        "Ghost detected at " + target + ", queued reload " + reloadPos);
                }
            }
        }

        // Nếu Nuker bị kẹt (progress không tăng trong 5 tick)
        if (PeoClient.NukerLogic.getBreakingProgress() < 0.01f && 
            PeoClient.CFG.nuker && 
            checkInterval % 10 == 0) {
            if (target != null && !reloadQueue.contains(target)) {
                reloadQueue.add(target);
                DiagnosticRecorder.get().record("AutoBlockReload", 
                    "Stalled break, reloading " + target);
            }
        }
    }

    private static void performBlockReload(class_2338 pos) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) return;

        class_2680 state = mc.field_1687.method_8320(pos);
        if (state.method_26215()) {
            // Block đã là air, không cần reload
            return;
        }

        class_2350 side = getBestSide(pos);
        if (side == null) side = class_2350.field_11036;

        try {
            // Gửi interact packet (click phải) để server gửi lại block state
            class_3965 hit = new class_3965(
                class_243.method_24953(pos),
                side,
                pos,
                false
            );
            class_2885 interactPacket = new class_2885(
                class_1268.field_5808,
                hit,
                0
            );
            mc.field_1687.method_8522(interactPacket);

            // Cũng gửi attack block để chắc chắn
            mc.field_1761.method_2902(pos, side);
            mc.field_1724.method_6104(class_1268.field_5808);

            DiagnosticRecorder.get().record("AutoBlockReload", 
                "Reloaded block at " + pos + " (side=" + side + ")");
            
        } catch (Exception e) {
            DiagnosticRecorder.get().record("AutoBlockReload", 
                "Reload failed at " + pos + ": " + e.getMessage());
        }
    }

    private static class_2338 getCurrentTarget() {
        try {
            java.lang.reflect.Field field = PeoClient.NukerLogic.class.getDeclaredField("breakingPos");
            field.setAccessible(true);
            return (class_2338) field.get(null);
        } catch (Exception e) {
            if (mc.field_1724 != null) {
                net.minecraft.class_239 rayHit = mc.field_1724.method_5745(6.0, 0.0f, false);
                if (rayHit instanceof class_3965 hit && hit.method_17783() == class_239.class_240.field_1332) {
                    return hit.method_17777();
                }
            }
            return null;
        }
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
            double dot = diff.method_1029().method_1026(normal);
            if (dot > bestDot) {
                bestDot = dot;
                best = side;
            }
        }
        return best;
    }

    public static void queueReload(class_2338 pos) {
        if (pos != null && !reloadQueue.contains(pos)) {
            reloadQueue.add(pos);
        }
    }

    public static void clearQueue() {
        reloadQueue.clear();
    }

    public static int getQueueSize() {
        return reloadQueue.size();
    }
}