package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import net.minecraft.class_310;

/**
 * Server-compatibility controller for Nuker.
 *
 * This intentionally uses normal Minecraft interaction APIs. It does not spoof
 * position, inject movement packets, or attempt to defeat an anti-cheat.
 */
public final class NukerCompatibility {
    private static boolean enabled = true;
    private static int quietTicks = 0;
    private static long lastTargetReset = 0L;
    private static int invalidTicks = 0;

    private NukerCompatibility() {}

    public static void tick(class_310 mc) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) return;
        if (!PeoClient.CFG.nuker) return;

        // Keep the controller on the client tick; never create a background thread.
        if (!enabled) {
            PeoClient.NukerLogic.tick(mc);
            return;
        }

        quietTicks = Math.max(0, quietTicks - 1);
        if (quietTicks > 0) return;

        // Conservative pacing only: the normal interaction manager remains the
        // source of truth. If the player is not in a valid world state, pause
        // briefly rather than creating conflicting break requests.
        if (mc.field_1724.method_33571() == null) {
            if (++invalidTicks >= 2) {
                PeoClient.NukerLogic.resetState();
                invalidTicks = 0;
            }
            quietTicks = 1;
            return;
        }
        invalidTicks = 0;

        // Let the existing NukerLogic own the real block-breaking state.
        // Compatibility is deliberately conservative: no synthetic movement/rotation packets.
        PeoClient.NukerLogic.tick(mc);
    }

    public static void toggle() {
        enabled = !enabled;
        quietTicks = 0;
        lastTargetReset = System.currentTimeMillis();
        invalidTicks = 0;
    }

    public static boolean isEnabled() { return enabled; }

    public static String status() {
        return enabled ? "Compatibility ON" : "Compatibility OFF";
    }
}
