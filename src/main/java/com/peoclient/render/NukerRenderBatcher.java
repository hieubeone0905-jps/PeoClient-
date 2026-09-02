package com.peoclient.render;

import net.minecraft.class_310;

/**
 * Render-only coalescer for rapid ClientWorld block updates.
 *
 * Many Nuker updates can hit the same render sections in one client tick.
 * Instead of enqueueing a separate large render request for every block,
 * collect one bounding box and flush it once after Nuker finishes its tick.
 * This never changes world state and never sends or modifies packets.
 */
public final class NukerRenderBatcher {
    private static int minX = Integer.MAX_VALUE;
    private static int minY = Integer.MAX_VALUE;
    private static int minZ = Integer.MAX_VALUE;
    private static int maxX = Integer.MIN_VALUE;
    private static int maxY = Integer.MIN_VALUE;
    private static int maxZ = Integer.MIN_VALUE;
    private static boolean pending;

    private NukerRenderBatcher() {}

    public static void mark(int x, int y, int z) {
        if (!pending) {
            minX = maxX = x;
            minY = maxY = y;
            minZ = maxZ = z;
            pending = true;
            return;
        }
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (z < minZ) minZ = z;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
        if (z > maxZ) maxZ = z;
    }

    public static void flush(class_310 mc) {
        if (!pending) return;
        try {
            if (mc == null || mc.field_1687 == null || mc.field_1769 == null) return;

            // Include the immediate neighboring blocks because block faces,
            // fences and other connected/transparent geometry can depend on them.
            int bx0 = minX - 1;
            int by0 = minY - 1;
            int bz0 = minZ - 1;
            int bx1 = maxX + 1;
            int by1 = maxY + 1;
            int bz1 = maxZ + 1;

            // 1.21.4's section-range dirty operation is stronger than merely
            // scheduling individual block rerenders and lets the renderer
            // coalesce the work into the affected chunk sections.
            mc.field_1769.method_62219(bx0, by0, bz0, bx1, by1, bz1);

            // Keep the normal block-level path as well for exact state changes.
            mc.field_1769.method_18146(bx0, by0, bz0, bx1, by1, bz1);

            // Ask the terrain system to consume the dirty work promptly.
            mc.field_1769.method_3292();
        } catch (Throwable ignored) {
            // Render repair must never affect gameplay or vanilla updates.
        } finally {
            clear();
        }
    }

    public static void clear() {
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
        pending = false;
    }
}
