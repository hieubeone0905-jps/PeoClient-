package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.AccountSessionMetrics;
import com.peoclient.diagnostic.LatencyMetrics;
import com.peoclient.diagnostic.TickMetrics;
import net.minecraft.class_310;

/**
 * Compatibility wrapper that never suppresses a Nuker tick. The actual Nuker
 * remains fully responsible for its configured range/multi/cooldown behaviour.
 */
public final class NukerCompatibility {
    private static boolean enabled = true;

    private NukerCompatibility() {}

    public static void tick(class_310 mc) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) return;
        if (!PeoClient.CFG.nuker) return;

        TickMetrics.get().recordTickStart();
        // Never gate, sleep, delay or skip NukerLogic here.
        // NukerLogic is the single owner of block-breaking state.
        // Do not call WorldSync here again: NukerLogic already performs its
        // observational sync once per client tick.
        PeoClient.NukerLogic.tick(mc);
        LatencyMetrics.get().updatePing();
        AccountSessionMetrics.get().tick();
        TickMetrics.get().recordTickEnd();
    }

    public static void toggle() { enabled = !enabled; }
    public static boolean isEnabled() { return enabled; }
    public static String status() { return enabled ? "Compatibility ON" : "Compatibility OFF"; }
}
