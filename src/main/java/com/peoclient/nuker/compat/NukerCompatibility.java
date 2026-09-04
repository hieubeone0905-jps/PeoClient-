package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import com.peoclient.modules.UpLevelVipProMax;
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
        // UpLevelVipProMax owns the interaction while its handled level GUI is open.
        // Do not let Nuker issue a competing block interaction in that window.
        if (UpLevelVipProMax.isBusy()) return;

        TickMetrics.get().recordTickStart();
        // Never gate, sleep, delay or skip NukerLogic here.
        PeoClient.NukerLogic.tick(mc);
        // WorldSync is intentionally observational/recovery-only and never gates throughput.
        NukerWorldSync.tick(mc);
        LatencyMetrics.get().updatePing();
        AccountSessionMetrics.get().tick();
        TickMetrics.get().recordTickEnd();
    }

    public static void toggle() { enabled = !enabled; }
    public static boolean isEnabled() { return enabled; }
    public static String status() { return enabled ? "Compatibility ON" : "Compatibility OFF"; }
}
