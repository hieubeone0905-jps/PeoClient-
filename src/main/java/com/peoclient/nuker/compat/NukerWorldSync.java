package com.peoclient.nuker.compat;

import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;

/**
 * Client-side Nuker/world synchronisation guard với cơ chế phát hiện ghost block mạnh mẽ.
 */
public final class NukerWorldSync {
    private static class_2338 watched;
    private static long watchedSince;
    private static long lastResolved;
    private static int staleRecoveries;
    private static int ghostBlockDetections;
    private static long lastGhostDetectionTime;

    private NukerWorldSync() {}

    public static void tick(class_310 mc) {
        if (mc == null || mc.field_1687 == null || mc.field_1724 == null) return;
        // Kiểm tra ghost block: nếu watched block là air nhưng chưa được resolve
        if (watched != null && mc.field_1687.method_8320(watched).method_26215()) {
            // Nếu block đã là air nhưng chưa được resolve, đây có thể là ghost block
            if (System.currentTimeMillis() - watchedSince > 200) {
                // Coi như ghost block, tự động recover
                ghostBlockDetections++;
                lastGhostDetectionTime = System.currentTimeMillis();
                DiagnosticRecorder.get().record("NukerWorldSync", 
                    "Ghost block detected at " + watched + ", forced recovery");
                onStaleTarget(mc, watched, 0.0f);
            }
        }
        // Nếu không có watched, reset
        if (watched != null && mc.field_1687.method_8320(watched).method_26215()) {
            watched = null;
            watchedSince = 0L;
        }
    }

    public static void onTargetStarted(class_310 mc, class_2338 pos) {
        watched = pos == null ? null : pos.method_10062();
        watchedSince = System.currentTimeMillis();
    }

    public static void onBreakResolved(class_310 mc, class_2338 pos) {
        if (pos != null && pos.equals(watched)) {
            watched = null;
            watchedSince = 0L;
        }
        lastResolved = System.currentTimeMillis();
    }

    public static void onStaleTarget(class_310 mc, class_2338 pos, float progress) {
        if (mc == null || mc.field_1761 == null || pos == null) return;
        staleRecoveries++;
        // Hủy trạng thái đào hiện tại
        mc.field_1761.method_2925();
        // Nếu block vẫn còn, gửi packet abort để đồng bộ
        if (!mc.field_1687.method_8320(pos).method_26215()) {
            // Gửi packet abort nếu cần
        }
        watched = null;
        watchedSince = 0L;
        DiagnosticRecorder.get().record("NukerWorldSync",
                "Stale target recovered: " + pos + " progress=" + progress);
    }

    public static boolean isWatched(class_2338 pos) {
        return watched != null && watched.equals(pos);
    }

    public static long getWatchedAgeMs() {
        return watchedSince <= 0 ? 0L : Math.max(0L, System.currentTimeMillis() - watchedSince);
    }

    public static long getLastResolved() { return lastResolved; }
    public static int getStaleRecoveries() { return staleRecoveries; }
    public static int getGhostBlockDetections() { return ghostBlockDetections; }
    public static void reset() { watched = null; watchedSince = 0L; lastResolved = 0L; staleRecoveries = 0; ghostBlockDetections = 0; }
}