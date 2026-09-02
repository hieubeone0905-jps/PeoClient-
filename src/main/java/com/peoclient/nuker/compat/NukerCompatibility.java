package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.*;
import net.minecraft.class_310;

public final class NukerCompatibility {
    private static boolean enabled = true;
    private static int quietTicks = 0;
    private static long lastTargetReset = 0L;

    public static void tick(class_310 mc) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) return;
        if (!PeoClient.CFG.nuker) return;

        // Record tick start
        TickMetrics.get().recordTickStart();

        if (!enabled) {
            PeoClient.NukerLogic.tick(mc);
            TickMetrics.get().recordTickEnd();
            return;
        }

        quietTicks = Math.max(0, quietTicks - 1);
        if (quietTicks > 0) {
            TickMetrics.get().recordTickEnd();
            return;
        }

        // NukerLogic will call diagnostic hooks internally
        PeoClient.NukerLogic.tick(mc);

        // Update latency metrics
        LatencyMetrics.get().updatePing();

        TickMetrics.get().recordTickEnd();
        AccountSessionMetrics.get().tick();
    }

    public static void toggle() {
        enabled = !enabled;
        quietTicks = 0;
        lastTargetReset = System.currentTimeMillis();
    }

    public static boolean isEnabled() { return enabled; }
    public static String status() { return enabled ? "Compatibility ON" : "Compatibility OFF"; }
}