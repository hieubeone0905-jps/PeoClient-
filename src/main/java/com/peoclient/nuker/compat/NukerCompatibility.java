package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import net.minecraft.client.MinecraftClient;

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

    private NukerCompatibility() {}

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (!PeoClient.CFG.nuker) return;

        // Keep the controller on the client tick; never create a background thread.
        if (!enabled) {
            PeoClient.NukerLogic.tick(mc);
            return;
        }

        quietTicks = Math.max(0, quietTicks - 1);
        if (quietTicks > 0) return;

        // Let the existing NukerLogic own the real block-breaking state.
        // Compatibility is deliberately conservative: no synthetic movement/rotation packets.
        PeoClient.NukerLogic.tick(mc);
    }

    public static void toggle() {
        enabled = !enabled;
        quietTicks = 0;
        lastTargetReset = System.currentTimeMillis();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(
                    net.minecraft.text.Text.literal("Nuker Compatibility: " + (enabled ? "ON" : "OFF")),
                    true
            );
        }
    }

    public static boolean isEnabled() { return enabled; }

    public static String status() {
        return enabled ? "Compatibility ON" : "Compatibility OFF";
    }
}
