package com.peoclient.render;

import net.minecraft.class_310;

import java.util.HashSet;
import java.util.Set;

/**
 * Render-only coalescer for rapid Nuker block updates.
 *
 * Instead of relying only on a large bounding-box dirty request, this keeps
 * the exact chunk-section coordinates touched by Nuker and queues those
 * built sections directly through WorldRenderer.scheduleChunkRender
 * (method_8571) once per client tick. This is much closer to the vanilla
 * renderer's normal update path and avoids rebuilding a large 3x3x3 region.
 */
public final class NukerRenderBatcher {
    private static final Set<Long> PENDING_SECTIONS = new HashSet<>();

    private static int minX = Integer.MAX_VALUE;
    private static int minY = Integer.MAX_VALUE;
    private static int minZ = Integer.MAX_VALUE;
    private static int maxX = Integer.MIN_VALUE;
    private static int maxY = Integer.MIN_VALUE;
    private static int maxZ = Integer.MIN_VALUE;
    private static boolean pendingBounds;

    private NukerRenderBatcher() {}

    public static void mark(int x, int y, int z) {
        if (!pendingBounds) {
            minX = maxX = x;
            minY = maxY = y;
            minZ = maxZ = z;
            pendingBounds = true;
        } else {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }
        markSectionForBlock(x, y, z);
    }

    public static void markSectionForBlock(int x, int y, int z) {
        int sx = Math.floorDiv(x, 16);
        int sy = Math.floorDiv(y, 16);
        int sz = Math.floorDiv(z, 16);
        addSection(sx, sy, sz);

        // Block faces can affect a neighboring built section when the block is
        // exactly on a section boundary. Queue only those touching neighbors.
        int lx = Math.floorMod(x, 16);
        int ly = Math.floorMod(y, 16);
        int lz = Math.floorMod(z, 16);
        if (lx == 0) addSection(sx - 1, sy, sz);
        if (lx == 15) addSection(sx + 1, sy, sz);
        if (ly == 0) addSection(sx, sy - 1, sz);
        if (ly == 15) addSection(sx, sy + 1, sz);
        if (lz == 0) addSection(sx, sy, sz - 1);
        if (lz == 15) addSection(sx, sy, sz + 1);
    }

    private static void addSection(int x, int y, int z) {
        PENDING_SECTIONS.add(pack(x, y, z));
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (y & 0xFFFL);
    }

    private static int unpackX(long packed) {
        int x = (int) (packed >> 38);
        return (x << 6) >> 6;
    }

    private static int unpackY(long packed) {
        int y = (int) (packed & 0xFFFL);
        return (y << 20) >> 20;
    }

    private static int unpackZ(long packed) {
        int z = (int) ((packed >> 12) & 0x3FFFFFFL);
        return (z << 6) >> 6;
    }

    public static void flush(class_310 mc) {
        if (PENDING_SECTIONS.isEmpty() && !pendingBounds) return;
        try {
            if (mc == null || mc.field_1687 == null || mc.field_1769 == null) return;

            // Directly queue each affected built chunk section. This is the
            // same WorldRenderer entry point used by vanilla for section work.
            for (long packed : PENDING_SECTIONS) {
                mc.field_1769.method_8571(
                        unpackX(packed),
                        unpackY(packed),
                        unpackZ(packed));
            }

            if (pendingBounds) {
                int bx0 = minX - 1;
                int by0 = minY - 1;
                int bz0 = minZ - 1;
                int bx1 = maxX + 1;
                int by1 = maxY + 1;
                int bz1 = maxZ + 1;

                // Keep a small block-level dirty pass for connected geometry.
                mc.field_1769.method_18146(bx0, by0, bz0, bx1, by1, bz1);
                mc.field_1769.method_3292();
            }
        } catch (Throwable ignored) {
            // Render repair must never affect gameplay or vanilla updates.
        } finally {
            clear();
        }
    }

    public static void clear() {
        PENDING_SECTIONS.clear();
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
        pendingBounds = false;
    }
}
