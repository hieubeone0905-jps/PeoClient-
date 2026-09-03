package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3965;
import net.minecraft.class_3959;
import net.minecraft.class_239;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Auto Block Reload - Tự động reload block ghost bằng cách mô phỏng giữ chuột phải
 * Không làm giảm tốc độ Nuker, chỉ bổ sung cơ chế reload khi cần
 */
public final class AutoBlockReload {
    private static final class_310 mc = class_310.method_1551();
    private static final ConcurrentLinkedQueue<class_2338> reloadQueue = new ConcurrentLinkedQueue<>();
    private static boolean active = false;
    private static int checkInterval = 0;
    private static long lastReloadTime = 0;
    private static final int RELOAD_COOLDOWN_MS = 100; // 100ms giữa các lần reload

    // Phát hiện ghost block: block mà client thấy là air nhưng server vẫn có block
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

    /**
     * Gọi mỗi tick từ NukerLogic
     */
    public static void tick() {
        if (!isActive()) return;
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) return;

        checkInterval++;

        // Mỗi 3 tick kiểm tra ghost block
        if (checkInterval % 3 == 0) {
            detectAndReloadGhostBlocks();
        }

        // Xử lý reload queue nếu có
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

    /**
     * Phát hiện và xử lý ghost block
     */
    private static void detectAndReloadGhostBlocks() {
        if (mc.field_1724 == null || mc.field_1687 == null) return;

        // Lấy target hiện tại từ NukerLogic thông qua breakingPos
        class_2338 target = getCurrentTarget();
        if (target == null) return;

        // Kiểm tra block tại vị trí target
        class_2680 state = mc.field_1687.method_8320(target);
        boolean clientSeesAir = state.method_26215();

        // Nếu client thấy air nhưng đây là target đang đào, có thể là ghost
        if (clientSeesAir) {
            // Kiểm tra xem có thực sự ghost không (dùng raycast để xác nhận)
            class_3965 hit = mc.field_1724.method_31560(PeoClient.CFG.nukerRange + 1.0, 0.0f, false);
            if (hit.method_17783() == class_239.class_240.field_1332) {
                class_2338 hitPos = hit.method_17777();
                // Nếu raycast trúng block, có thể client đang bị ghost
                if (!hitPos.equals(target)) {
                    // Block thực tế khác với target, thêm vào queue reload
                    if (!reloadQueue.contains(hitPos)) {
                        reloadQueue.add(hitPos);
                        DiagnosticRecorder.get().record("AutoBlockReload", 
                            "Ghost detected at " + target + ", reloading " + hitPos);
                    }
                }
            }
        }

        // Kiểm tra nếu Nuker bị kẹt (queue lớn nhưng không có progress)
        if (PeoClient.NukerLogic.getBreakingProgress() < 0.01f && 
            PeoClient.CFG.nuker && 
            checkInterval % 10 == 0) {
            // Thử reload target hiện tại
            if (target != null && !reloadQueue.contains(target)) {
                reloadQueue.add(target);
                DiagnosticRecorder.get().record("AutoBlockReload", 
                    "Stalled break, reloading " + target);
            }
        }
    }

    /**
     * Thực hiện reload block bằng cách mô phỏng click phải
     */
    private static void performBlockReload(class_2338 pos) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) return;

        // Kiểm tra block còn tồn tại
        class_2680 state = mc.field_1687.method_8320(pos);
        if (state.method_26215()) return;

        // Tính toán side để tương tác
        class_2350 side = getBestSide(pos);
        if (side == null) side = class_2350.field_11036;

        try {
            // Mô phỏng click phải (interact) để trigger block state update
            // Cách 1: Sử dụng interaction manager để attack block (giống như click phải)
            // Điều này sẽ gửi packet update block lên server và client nhận lại state mới
            
            // Cách 2: Gửi packet interact block
            // Sử dụng reflection để tạo packet interact block
            
            // Phương án đơn giản và an toàn: gọi attackBlock (giống như Nuker)
            // Nhưng với mục đích reload, ta sẽ gọi updateBlockBreakingProgress
            mc.field_1761.method_2902(pos, side);
            mc.field_1724.method_6104(net.minecraft.class_1268.field_5808);
            
            // Đánh dấu đã reload
            DiagnosticRecorder.get().record("AutoBlockReload", 
                "Reloaded block at " + pos + " (side=" + side + ")");
            
        } catch (Exception e) {
            DiagnosticRecorder.get().record("AutoBlockReload", 
                "Reload failed at " + pos + ": " + e.getMessage());
        }
    }

    /**
     * Lấy target hiện tại từ NukerLogic
     */
    private static class_2338 getCurrentTarget() {
        try {
            // Dùng reflection để lấy breakingPos từ NukerLogic
            java.lang.reflect.Field field = PeoClient.NukerLogic.class.getDeclaredField("breakingPos");
            field.setAccessible(true);
            return (class_2338) field.get(null);
        } catch (Exception e) {
            // Fallback: lấy block đang nhìn
            if (mc.field_1724 != null) {
                class_3965 hit = mc.field_1724.method_31560(6.0, 0.0f, false);
                if (hit.method_17783() == class_239.class_240.field_1332) {
                    return hit.method_17777();
                }
            }
            return null;
        }
    }

    /**
     * Tìm side tốt nhất để tương tác với block
     */
    private static class_2350 getBestSide(class_2338 pos) {
        if (mc.field_1724 == null) return null;
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

    /**
     * Thủ công thêm block vào queue reload
     */
    public static void queueReload(class_2338 pos) {
        if (pos != null && !reloadQueue.contains(pos)) {
            reloadQueue.add(pos);
        }
    }

    /**
     * Xóa queue reload
     */
    public static void clearQueue() {
        reloadQueue.clear();
    }

    /**
     * Kiểm tra queue có đang chờ không
     */
    public static int getQueueSize() {
        return reloadQueue.size();
    }
}