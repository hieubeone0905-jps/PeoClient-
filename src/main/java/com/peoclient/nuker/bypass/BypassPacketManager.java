package com.peoclient.nuker.bypass;

import net.minecraft.class_2338;
import net.minecraft.class_2350;

/**
 * Legacy compatibility API.
 *
 * Packet injection was removed from the Nuker compatibility layer.  The
 * vanilla interaction manager is now the only producer of movement/break
 * packets, which prevents competing packet sequences and desync.
 */
public final class BypassPacketManager {
    private BypassPacketManager() {}

    public static void sendRotation(float yaw, float pitch, boolean onGround) {
        // Intentionally empty. Rotation is owned by Minecraft/NukerLogic.
    }

    public static void sendPosition(double x, double y, double z, boolean onGround) {
        // Intentionally empty. Position is owned by Minecraft.
    }

    public static void sendBlockAction(class_2338 pos, class_2350 side) {
        // Intentionally empty. Block actions are owned by InteractionManager.
    }

    public static void sendResetPacket() {
        // Intentionally empty.
    }

    public static void clearQueue() {
        // Retained for source compatibility.
    }
}
