package com.peoclient.render;

import com.peoclient.diagnostic.NukerRenderDiagnostics;
import net.minecraft.class_310;

import java.util.HashSet;
import java.util.Set;

/**
 * Coalesces render invalidation for rapid Nuker block changes.
 * Render-only: never changes world state and never sends packets.
 */
public final class NukerRenderBatcher {
    private static final Set<Long> PENDING_SECTIONS = new HashSet<>();
    private static int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private static int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    private static boolean pendingBounds;
    private static boolean hadChanges;
    private static long lastChangeMs;
    private static long firstChangeMs;

    private NukerRenderBatcher() {}

    public static void mark(int x, int y, int z) {
        if (!pendingBounds) {
            minX = maxX = x; minY = maxY = y; minZ = maxZ = z;
            pendingBounds = true;
        } else {
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
        }
        hadChanges = true;
        if (firstChangeMs == 0L) firstChangeMs = System.currentTimeMillis();
        lastChangeMs = System.currentTimeMillis();
        markSectionForBlock(x, y, z);
    }

    public static void markSectionForBlock(int x, int y, int z) {
        int sx = Math.floorDiv(x, 16), sy = Math.floorDiv(y, 16), sz = Math.floorDiv(z, 16);
        int before = PENDING_SECTIONS.size();
        addSection(sx, sy, sz);
        int lx = Math.floorMod(x, 16), ly = Math.floorMod(y, 16), lz = Math.floorMod(z, 16);
        if (lx == 0) addSection(sx - 1, sy, sz);
        if (lx == 15) addSection(sx + 1, sy, sz);
        if (ly == 0) addSection(sx, sy - 1, sz);
        if (ly == 15) addSection(sx, sy + 1, sz);
        if (lz == 0) addSection(sx, sy, sz - 1);
        if (lz == 15) addSection(sx, sy, sz + 1);
        int added = PENDING_SECTIONS.size() - before;
        if (added > 0) NukerRenderDiagnostics.sectionQueued(added);
    }

    private static void addSection(int x, int y, int z) {
        PENDING_SECTIONS.add(pack(x, y, z));
    }

    private static long pack(int x, int y, int z) {
        return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }
    private static int unpackX(long p) { int x=(int)(p>>38); return (x<<6)>>6; }
    private static int unpackY(long p) { int y=(int)(p&0xFFFL); return (y<<20)>>20; }
    private static int unpackZ(long p) { int z=(int)((p>>12)&0x3FFFFFFL); return (z<<6)>>6; }

    public static void flush(class_310 mc) {
        if (PENDING_SECTIONS.isEmpty() && !pendingBounds && !hadChanges) return;
        try {
            if (mc == null || mc.field_1687 == null || mc.field_1769 == null) {
                clear();
                return;
            }

            int sections = PENDING_SECTIONS.size();
            for (long p : PENDING_SECTIONS) {
                mc.field_1769.method_8571(unpackX(p), unpackY(p), unpackZ(p));
            }

            if (pendingBounds) {
                mc.field_1769.method_18146(
                        minX - 1, minY - 1, minZ - 1,
                        maxX + 1, maxY + 1, maxZ + 1);
            }

            if (hadChanges) {
                mc.field_1769.method_3292();
            }

            NukerRenderDiagnostics.flushed(sections);

            // If Nuker has been running continuously, a bounded full renderer
            // rebuild periodically clears any stale queued mesh that survived
            // incremental invalidation. It is throttled and only render-side.
            long now = System.currentTimeMillis();
            if (hadChanges && firstChangeMs > 0L && now - firstChangeMs >= 3000L && NukerRenderDiagnostics.shouldHardRefresh()) {
                mc.field_1769.method_3279();
                NukerRenderDiagnostics.hardRefresh(mc);
            }
            NukerRenderDiagnostics.summary();
        } catch (Throwable t) {
            if (com.peoclient.diagnostic.DiagnosticConfig.get().isEnabled()) {
                com.peoclient.diagnostic.DiagnosticRecorder.get().record(
                        "NukerRender", "RENDER_REFRESH_ERROR " + t.getClass().getSimpleName() + ": " + t.getMessage());
                com.peoclient.diagnostic.DiagnosticRecorder.get().flush();
            }
        } finally {
            clear();
        }
    }

    public static void clear() {
        PENDING_SECTIONS.clear();
        minX=minY=minZ=Integer.MAX_VALUE;
        maxX=maxY=maxZ=Integer.MIN_VALUE;
        pendingBounds=false;
        hadChanges=false;
    }
}
