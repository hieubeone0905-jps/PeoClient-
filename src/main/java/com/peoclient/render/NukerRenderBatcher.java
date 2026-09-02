package com.peoclient.render;

import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_310;

import java.util.HashSet;
import java.util.Set;

/**
 * Coalesces Nuker render updates.  Only one render pass is submitted per
 * client tick, regardless of how many blocks changed during that tick.
 * This is render-only and never changes world state or packets.
 */
public final class NukerRenderBatcher {
    private static final Set<Long> PENDING_SECTIONS = new HashSet<>();
    private static int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private static int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    private static boolean pendingBounds;
    private static long totalChanges;
    private static long totalFlushes;
    private static long lastHardRefreshMs;
    private static int changesSinceHardRefresh;

    private NukerRenderBatcher() {}

    public static void mark(int x, int y, int z) {
        totalChanges++;
        changesSinceHardRefresh++;
        if (!pendingBounds) {
            minX = maxX = x; minY = maxY = y; minZ = maxZ = z;
            pendingBounds = true;
        } else {
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
        }
        markSectionForBlock(x, y, z);
    }

    public static void markSectionForBlock(int x, int y, int z) {
        int sx = Math.floorDiv(x, 16), sy = Math.floorDiv(y, 16), sz = Math.floorDiv(z, 16);
        addSection(sx, sy, sz);
        int lx = Math.floorMod(x, 16), ly = Math.floorMod(y, 16), lz = Math.floorMod(z, 16);
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
        return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }
    private static int unpackX(long p) { int v=(int)(p>>38); return (v<<6)>>6; }
    private static int unpackY(long p) { int v=(int)(p&0xFFFL); return (v<<20)>>20; }
    private static int unpackZ(long p) { int v=(int)((p>>12)&0x3FFFFFFL); return (v<<6)>>6; }

    public static void flush(class_310 mc) {
        if (PENDING_SECTIONS.isEmpty() && !pendingBounds) return;
        int sections = PENDING_SECTIONS.size();
        try {
            if (mc == null || mc.field_1687 == null || mc.field_1769 == null) return;

            // One queue submission per unique section. Do not also submit a
            // large bounding-box/terrain refresh: that was causing a render
            // backlog during sustained Multi mining.
            for (long packed : PENDING_SECTIONS) {
                mc.field_1769.method_8571(unpackX(packed), unpackY(packed), unpackZ(packed));
            }

            totalFlushes++;
            long now = System.currentTimeMillis();
            if (changesSinceHardRefresh > 0 && now - lastHardRefreshMs >= 2500L) {
                // Safety valve only after sustained activity. 2.5s cadence
                // avoids repeatedly rebuilding the entire renderer.
                mc.field_1769.method_3279();
                lastHardRefreshMs = now;
                changesSinceHardRefresh = 0;
                DiagnosticRecorder.get().record("NukerRender",
                        "HARD_REFRESH renderer.reload() sections=" + sections + " totalChanges=" + totalChanges);
            }

            DiagnosticRecorder.get().record("NukerRender",
                    "FLUSH sections=" + sections + " totalChanges=" + totalChanges + " flushes=" + totalFlushes);
        } catch (Throwable t) {
            DiagnosticRecorder.get().record("NukerRender", "RENDER_ERROR " + t.getClass().getSimpleName() + ": " + t.getMessage());
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
