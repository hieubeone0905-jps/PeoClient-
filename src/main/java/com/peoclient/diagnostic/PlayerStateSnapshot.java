package com.peoclient.diagnostic;

import net.minecraft.class_310;
import net.minecraft.class_243;

/**
 * Immutable snapshot trạng thái player.
 */
public final class PlayerStateSnapshot {
    private final long timestamp;
    private final int clientTick;
    private final double x, y, z;
    private final double velocityX, velocityY, velocityZ;
    private final float yaw, pitch;
    private final boolean onGround;
    private final boolean sprinting;
    private final boolean sneaking;
    private final int selectedSlot;
    private final float health;
    private final int food;
    private final String gamemode;

    private PlayerStateSnapshot(long ts, int tick, double x, double y, double z,
                                double vx, double vy, double vz, float yaw, float pitch,
                                boolean onGround, boolean sprinting, boolean sneaking,
                                int slot, float health, int food, String gamemode) {
        this.timestamp = ts; this.clientTick = tick;
        this.x = x; this.y = y; this.z = z;
        this.velocityX = vx; this.velocityY = vy; this.velocityZ = vz;
        this.yaw = yaw; this.pitch = pitch;
        this.onGround = onGround; this.sprinting = sprinting; this.sneaking = sneaking;
        this.selectedSlot = slot; this.health = health; this.food = food;
        this.gamemode = gamemode;
    }

    public static PlayerStateSnapshot capture(class_310 mc) {
        if (mc.field_1724 == null) {
            return new PlayerStateSnapshot(System.currentTimeMillis(), -1, 0,0,0, 0,0,0, 0,0, false, false, false, 0, 0, 0, "UNKNOWN");
        }
        class_243 pos = mc.field_1724.method_23317_();
        class_243 vel = mc.field_1724.method_23309_();
        String gm = mc.field_1761 != null && mc.field_1761.method_2917() != null
                ? mc.field_1761.method_2917().toString() : "UNKNOWN";
        return new PlayerStateSnapshot(
                System.currentTimeMillis(),
                mc.field_1724.field_6216,
                pos.field_1352, pos.field_1351, pos.field_1350,
                vel.field_1352, vel.field_1351, vel.field_1350,
                mc.field_1724.method_36454(),
                mc.field_1724.method_36455(),
                mc.field_1724.field_6228,
                mc.field_1724.method_24654(),
                mc.field_1724.method_23905(),
                mc.field_1724.method_31548().field_7461,
                mc.field_1724.method_6078(),
                mc.field_1724.method_6084(),
                gm
        );
    }

    // Getters
    public long getTimestamp() { return timestamp; }
    public int getClientTick() { return clientTick; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getVelocityZ() { return velocityZ; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public boolean isOnGround() { return onGround; }
    public boolean isSprinting() { return sprinting; }
    public boolean isSneaking() { return sneaking; }
    public int getSelectedSlot() { return selectedSlot; }
    public float getHealth() { return health; }
    public int getFood() { return food; }
    public String getGamemode() { return gamemode; }
}