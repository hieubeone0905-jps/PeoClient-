package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_310;

/** Local-only diagnostics. Never injects, spoofs, or alters Nuker behavior. */
public final class SafeCompatibilityDiagnostics {
    private static long ticks;
    private static boolean lastNuker;

    private SafeCompatibilityDiagnostics() {}

    public static void tick() {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null) return;
        ticks++;
        boolean nuker = PeoClient.CFG.nuker;
        if (nuker != lastNuker) {
            lastNuker = nuker;
            DiagnosticRecorder.get().record("NukerCompatibility",
                    "Nuker state changed: " + nuker + ", tick=" + ticks);
        }
    }
}
