package com.peoclient.diagnostic;

import net.minecraft.class_310;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;

/**
 * Snapshot đầy đủ của client tại thời điểm disconnect.
 * Immutable, không giữ reference tới PlayerEntity.
 */
public final class DisconnectContext {
    private final long timestamp;
    private final int clientTick;
    private final String accountName;
    private final String serverAddress;
    private final String dimension;
    private final double x, y, z;
    private final double velocityX, velocityY, velocityZ;
    private final float yaw, pitch;
    private final boolean onGround;
    private final int selectedSlot;
    private final float health;
    private final int food;
    private final String gamemode;
    private final int ping;
    private final boolean nukerEnabled;
    private final String nukerState;
    private final class_2338 targetPosition;
    private final String targetBlock;
    private final int breakAttempts, breakSuccess, breakFailures, recoveries;
    private final long lastBreakTime, lastSuccessTime, lastFailureTime;

    private static DisconnectContext last = null;

    private DisconnectContext(Builder b) {
        this.timestamp = b.timestamp;
        this.clientTick = b.clientTick;
        this.accountName = b.accountName;
        this.serverAddress = b.serverAddress;
        this.dimension = b.dimension;
        this.x = b.x; this.y = b.y; this.z = b.z;
        this.velocityX = b.velocityX; this.velocityY = b.velocityY; this.velocityZ = b.velocityZ;
        this.yaw = b.yaw; this.pitch = b.pitch;
        this.onGround = b.onGround;
        this.selectedSlot = b.selectedSlot;
        this.health = b.health;
        this.food = b.food;
        this.gamemode = b.gamemode;
        this.ping = b.ping;
        this.nukerEnabled = b.nukerEnabled;
        this.nukerState = b.nukerState;
        this.targetPosition = b.targetPosition;
        this.targetBlock = b.targetBlock;
        this.breakAttempts = b.breakAttempts;
        this.breakSuccess = b.breakSuccess;
        this.breakFailures = b.breakFailures;
        this.recoveries = b.recoveries;
        this.lastBreakTime = b.lastBreakTime;
        this.lastSuccessTime = b.lastSuccessTime;
        this.lastFailureTime = b.lastFailureTime;
    }

    public static DisconnectContext capture(class_310 mc) {
        Builder b = new Builder();
        b.timestamp = System.currentTimeMillis();
        b.clientTick = mc.field_1724 != null ? mc.field_1724.field_6216 : -1;
        b.accountName = mc.method_1548() != null ? mc.method_1548().method_1676() : "UNKNOWN";
        b.serverAddress = mc.field_1724 != null && mc.field_1724.field_6214 != null
                ? mc.field_1724.field_6214.method_10498() : "UNKNOWN";
        b.dimension = mc.field_1687 != null && mc.field_1687.method_27986() != null
                ? mc.field_1687.method_27986().toString() : "UNKNOWN";

        if (mc.field_1724 != null) {
            class_243 pos = mc.field_1724.method_23317_();
            b.x = pos.field_1352; b.y = pos.field_1351; b.z = pos.field_1350;
            class_243 vel = mc.field_1724.method_23309_();
            b.velocityX = vel.field_1352; b.velocityY = vel.field_1351; b.velocityZ = vel.field_1350;
            b.yaw = mc.field_1724.method_36454();
            b.pitch = mc.field_1724.method_36455();
            b.onGround = mc.field_1724.field_6228;
            b.selectedSlot = mc.field_1724.method_31548().field_7461;
            b.health = mc.field_1724.method_6078();
            b.food = mc.field_1724.method_6084();
            b.gamemode = mc.field_1761 != null && mc.field_1761.method_2917() != null
                    ? mc.field_1761.method_2917().toString() : "UNKNOWN";
            b.ping = mc.field_1724.field_6214 != null ? mc.field_1724.field_6214.method_11028() : -1;
        } else {
            b.x = b.y = b.z = 0;
            b.velocityX = b.velocityY = b.velocityZ = 0;
            b.yaw = b.pitch = 0;
            b.onGround = false;
            b.selectedSlot = 0;
            b.health = 0;
            b.food = 0;
            b.gamemode = "UNKNOWN";
            b.ping = -1;
        }

        b.nukerEnabled = com.peoclient.PeoClient.CFG.nuker;
        b.nukerState = com.peoclient.diagnostic.BreakStateTracker.get().getState().name();
        b.targetPosition = com.peoclient.diagnostic.BreakStateTracker.get().getCurrentTarget();
        b.targetBlock = b.targetPosition != null ? "minecraft:" + (mc.field_1687 != null ? mc.field_1687.method_8320(b.targetPosition).method_26204().toString() : "UNKNOWN") : "NONE";

        b.breakAttempts = com.peoclient.diagnostic.AccountSessionMetrics.get().getBreakAttempts();
        b.breakSuccess = com.peoclient.diagnostic.AccountSessionMetrics.get().getBreakSuccesses();
        b.breakFailures = com.peoclient.diagnostic.AccountSessionMetrics.get().getBreakFailures();
        b.recoveries = com.peoclient.diagnostic.AccountSessionMetrics.get().getRecoveries();
        b.lastBreakTime = 0;
        b.lastSuccessTime = 0;
        b.lastFailureTime = 0;

        return new DisconnectContext(b);
    }

    public static DisconnectContext getLast() { return last; }
    public static void setLast(DisconnectContext ctx) { last = ctx; }
    public static void clear() { last = null; }

    // Getters
    public long getTimestamp() { return timestamp; }
    public int getClientTick() { return clientTick; }
    public String getAccountName() { return accountName; }
    public String getServerAddress() { return serverAddress; }
    public String getDimension() { return dimension; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getVelocityZ() { return velocityZ; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public boolean isOnGround() { return onGround; }
    public int getSelectedSlot() { return selectedSlot; }
    public float getHealth() { return health; }
    public int getFood() { return food; }
    public String getGamemode() { return gamemode; }
    public int getPing() { return ping; }
    public boolean isNukerEnabled() { return nukerEnabled; }
    public String getNukerState() { return nukerState; }
    public class_2338 getTargetPosition() { return targetPosition; }
    public String getTargetBlock() { return targetBlock; }
    public int getBreakAttempts() { return breakAttempts; }
    public int getBreakSuccess() { return breakSuccess; }
    public int getBreakFailures() { return breakFailures; }
    public int getRecoveries() { return recoveries; }
    public long getLastBreakTime() { return lastBreakTime; }
    public long getLastSuccessTime() { return lastSuccessTime; }
    public long getLastFailureTime() { return lastFailureTime; }

    private static class Builder {
        long timestamp; int clientTick; String accountName, serverAddress, dimension;
        double x, y, z, velocityX, velocityY, velocityZ;
        float yaw, pitch; boolean onGround; int selectedSlot; float health; int food;
        String gamemode; int ping; boolean nukerEnabled; String nukerState;
        class_2338 targetPosition; String targetBlock;
        int breakAttempts, breakSuccess, breakFailures, recoveries;
        long lastBreakTime, lastSuccessTime, lastFailureTime;
    }
}