package com.peoclient.nuker.compat;

import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;

/**
 * Client-side Nuker/world synchronisation guard.
 *
 * This class does not spoof packets or attempt to defeat server-side checks.
 * It prevents a stale local break target from being reused after the server/world
 * state has stopped changing, which is the common cause of visible ghost blocks.
 * Normal successful breaks take no extra delay.
 */
public final class NukerWorldSync {
    private static class_2338 watched;
    private static long watchedSince;
    private static long lastResolved;
    private static int staleRecoveries;

    private NukerWorldSync() {}

    public static void tick(class_310 mc) {
        if (mc == null || mc.field_1687 == null || mc.field_1724 == null) return;
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
        // Only abort the vanilla client-side break state. Do not fabricate a
        // right-click/interact packet or overwrite the local world with a guess.
        mc.field_1761.method_2925();
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
    public static void reset() { watched = null; watchedSince = 0L; lastResolved = 0L; staleRecoveries = 0; }
}
