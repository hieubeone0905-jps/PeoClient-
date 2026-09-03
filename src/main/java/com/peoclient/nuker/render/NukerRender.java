package com.peoclient.nuker.render;

import net.minecraft.class_2338;
import net.minecraft.class_310;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lightweight client-side render resynchronization queue.
 *
 * This never fabricates a server state and never sends fake interaction packets.
 * It only asks the already loaded client world/renderer to rebuild the smallest
 * affected area after server block updates arrive.
 */
public final class NukerRender {
    private static final int MAX_PENDING = 4096;
    private static final int MAX_REFRESHES_PER_TICK = 12;
    private static final long FULL_RELOAD_COOLDOWN_MS = 2000L;
    private static final int FULL_RELOAD_THRESHOLD = 128;

    private static final Set<class_2338> pendingPositions = new LinkedHashSet<>();
    private static int totalWorldChanges;
    private static long lastFullReload;

    private NukerRender() {}

    /** Records a server-authoritative/client-world block update for targeted rerendering. */
    public static void observeServerUpdate(class_2338 pos) {
        totalWorldChanges = Math.min(Integer.MAX_VALUE, totalWorldChanges + 1);
        if (pendingPositions.size() < MAX_PENDING) {
            pendingPositions.add(pos.method_10062());
        }
    }

    /** Backwards-compatible counter hook. */
    public static void addWorldChange() {
        totalWorldChanges = Math.min(Integer.MAX_VALUE, totalWorldChanges + 1);
    }

    public static int getPendingWorldChanges() {
        return pendingPositions.size();
    }

    public static int getTotalWorldChanges() {
        return totalWorldChanges;
    }

    public static void resetPending() {
        pendingPositions.clear();
        totalWorldChanges = 0;
    }

    /**
     * Process a small number of precise render refreshes. This is deliberately
     * bounded so a burst of chunk updates cannot turn into a renderer.reload()
     * storm.
     */
    public static void tick(class_310 mc) {
        if (mc == null || mc.field_1687 == null || mc.field_1724 == null) return;

        int processed = 0;
        var it = pendingPositions.iterator();
        while (it.hasNext() && processed < MAX_REFRESHES_PER_TICK) {
            class_2338 pos = it.next();
            it.remove();

            try {
                // Rebuild only the affected client-world block region. This is
                // the closest safe equivalent to the visual refresh caused by a
                // normal player interaction, without generating extra clicks.
                mc.field_1687.method_18113(pos.method_10263(), pos.method_10260(), pos.method_10264());
            } catch (Throwable ignored) {
                // Renderer/version mismatch should never crash the client.
            }
            processed++;
        }

        // Escalate only when a very large backlog indicates that targeted rebuilds
        // are insufficient. The long cooldown prevents repeated full reloads.
        if (pendingPositions.size() >= FULL_RELOAD_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastFullReload >= FULL_RELOAD_COOLDOWN_MS) {
                lastFullReload = now;
                try {
                    mc.field_1769.method_3279();
                } catch (Throwable ignored) {
                    // Best-effort visual recovery only.
                }
                pendingPositions.clear();
            }
        }
    }

    /** Compatibility method retained for existing callers. */
    public static void flush() {
        class_310 mc = class_310.method_1551();
        tick(mc);
    }
}
