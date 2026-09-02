package com.peoclient.render;

import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_310;

import java.util.HashSet;
import java.util.Set;

/**
 * Coalesces Nuker render updates into one bounded section refresh per client tick.
 * Render-only: does not change world state, packets, or Nuker behaviour.
 */
public final class NukerRenderBatcher {
    private static final Set<Long> PENDING_SECTIONS = new HashSet<>();
    private static final int MAX_PENDING_SECTIONS = 256;
    private static long totalChanges;
    private static long totalFlushes;

    private NukerRenderBatcher() {}

    public static void mark(int x, int y, int z) {
        totalChanges++;
        markSectionForBlock(x, y, z);
    }

    public static void markSectionForBlock(int x, int y, int z) {
        int sx = Math.floorDiv(x, 16);
        int sy = Math.floorDiv(y, 16);
        int sz = Math.floorDiv(z, 16);

        // Only queue the changed section.  Neighbour sections are intentionally
        // omitted here to prevent a large render backlog during Multi mining.
        addSection(sx, sy, sz);
    }

    private static void addSection(int x, int y, int z) {
        if (PENDING_SECTIONS.size() < MAX_PENDING_SECTIONS) {
            PENDING_SECTIONS.add(pack(x, y, z));
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (y & 0xFFFL);
    }

    private static int unpackX(long p) {
        int v = (int) (p >> 38);
        return (v << 6) >> 6;
    }

    private static int unpackY(long p) {
        int v = (int) (p & 0xFFFL);
        return (v << 20) >> 20;
    }

    private static int unpackZ(long p) {
        int v = (int) ((p >> 12) & 0x3FFFFFFL);
        return (v << 6) >> 6;
    }

    public static void flush(class_310 mc) {
        if (PENDING_SECTIONS.isEmpty()) {
            return;
        }

        int sections = PENDING_SECTIONS.size();
        try {
            if (mc == null || mc.field_1687 == null || mc.field_1769 == null) {
                return;
            }

            for (long packed : PENDING_SECTIONS) {
                mc.field_1769.method_8571(
                        unpackX(packed),
                        unpackY(packed),
                        unpackZ(packed)
                );
            }

            totalFlushes++;
            DiagnosticRecorder.get().record(
                    "NukerRender",
                    "FLUSH sections=" + sections
                            + " totalChanges=" + totalChanges
                            + " flushes=" + totalFlushes
            );
        } catch (Throwable t) {
            DiagnosticRecorder.get().record(
                    "NukerRender",
                    "RENDER_ERROR " + t.getClass().getSimpleName() + ": " + t.getMessage()
            );
        } finally {
            clear();
        }
    }

    public static void clear() {
        PENDING_SECTIONS.clear();
    }
}
