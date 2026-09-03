package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;

/**
 * Client-side stability monitor. It deliberately does not spoof movement,
 * rotate the player automatically, or inject synthetic packets.
 */
public final class ClientStabilizer {
    private static final class_310 mc = class_310.method_1551();
    private static boolean enabled;
    private static int tickCounter;
    private static float lastYaw;
    private static float lastPitch;
    private static double lastTargetDistance = -1.0;
    private static int rotationMaxStep = 22;
    private static double positionJitter = 0.00008;
    private static int packetDelay = 2;

    private ClientStabilizer() {}

    public static void start() {
        enabled = true;
        tickCounter = 0;
        lastTargetDistance = -1.0;
        if (mc.field_1724 != null) {
            lastYaw = mc.field_1724.method_36454();
            lastPitch = mc.field_1724.method_36455();
        }
        DiagnosticRecorder.get().record("ClientStabilizer", "Started (client stability monitoring)");
    }

    public static void stop() {
        enabled = false;
        tickCounter = 0;
        lastTargetDistance = -1.0;
        DiagnosticRecorder.get().record("ClientStabilizer", "Stopped");
    }

    public static boolean isEnabled() {
        return enabled && PeoClient.CFG.nuker;
    }

    public static void tick() {
        if (!isEnabled() || mc.field_1724 == null || mc.field_1687 == null) return;
        tickCounter++;

        float yaw = mc.field_1724.method_36454();
        float pitch = mc.field_1724.method_36455();
        float yawDelta = Math.abs(class_3532.method_15393(yaw - lastYaw));
        float pitchDelta = Math.abs(pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;

        class_2338 target = PeoClient.NukerLogic.getCurrentTarget();
        if (target != null) {
            class_243 eye = mc.field_1724.method_33571();
            lastTargetDistance = eye.method_1022(class_243.method_24953(target));
        } else {
            lastTargetDistance = -1.0;
        }

        // Periodic diagnostics only; no packet or camera manipulation.
        if (tickCounter % 40 == 0 && (yawDelta > rotationMaxStep || pitchDelta > rotationMaxStep)) {
            DiagnosticRecorder.get().record("ClientStabilizer",
                    "Large local rotation delta: yaw=" + yawDelta + ", pitch=" + pitchDelta);
        }
    }

    public static void setRotationStep(int step) {
        rotationMaxStep = Math.max(1, Math.min(30, step));
    }

    public static void setPositionJitter(double jitter) {
        // Kept for config/UI compatibility. It is not used to move/spoof the player.
        positionJitter = Math.max(0.00001, Math.min(0.001, jitter));
    }

    public static void setPacketDelay(int delay) {
        // Kept for config/UI compatibility. No packet queue is used by this module.
        packetDelay = Math.max(1, Math.min(5, delay));
    }

    public static int getRotationStep() { return rotationMaxStep; }
    public static double getPositionJitter() { return positionJitter; }
    public static int getPacketDelay() { return packetDelay; }
    public static double getLastTargetDistance() { return lastTargetDistance; }
}
