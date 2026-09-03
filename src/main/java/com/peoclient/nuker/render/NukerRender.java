package com.peoclient.nuker.render;

import net.minecraft.class_2338;
import net.minecraft.class_310;

/**
 * Compatibility shim retained for older PeoClient callers.
 *
 * Ghost-block recovery is intentionally NOT implemented by forcing renderer
 * reloads. ClientWorld/server block updates are allowed to flow through
 * Minecraft's normal rendering path instead. This class therefore only keeps
 * diagnostic counters and exposes no renderer work queue.
 */
public final class NukerRender {
    private static int totalWorldChanges;

    private NukerRender() {}

    public static void observeServerUpdate(class_2338 pos) {
        totalWorldChanges = Math.min(Integer.MAX_VALUE, totalWorldChanges + 1);
    }

    public static void addWorldChange() {
        totalWorldChanges = Math.min(Integer.MAX_VALUE, totalWorldChanges + 1);
    }

    public static int getPendingWorldChanges() {
        return 0;
    }

    public static int getTotalWorldChanges() {
        return totalWorldChanges;
    }

    public static void resetPending() {
        totalWorldChanges = 0;
    }

    /** No forced rebuilds or WorldRenderer.reload() calls. */
    public static void tick(class_310 mc) {
        // Intentionally empty: vanilla ClientWorld updates own the render state.
    }

    /** Compatibility method retained for existing callers. */
    public static void flush() {
        // Intentionally empty.
    }
}
