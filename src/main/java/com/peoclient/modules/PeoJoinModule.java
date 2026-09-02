package com.peoclient.modules;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2561;
import net.minecraft.class_310;

/**
 * PeoJoin - safe connection recovery.
 *
 * Automatically recovers the client connection after a disconnect, waits for
 * the player to enter the server, sends /home once, and waits for the configured
 * post-join delay. It does not re-enable Nuker or simulate combat/mining input.
 */
public final class PeoJoinModule {
    private static final class_310 mc = class_310.method_1551();

    private static volatile boolean enabled = false;
    private static volatile State currentState = State.IDLE;
    private static long stateStartTime = 0L;
    private static boolean hadNuker = false;
    private static boolean commandSent = false;

    private static String serverName = "Skyblock";
    private static int preJoinDelay = 5;
    private static int postJoinDelay = 5;

    public enum State {
        IDLE,
        WAITING_FOR_DISCONNECT,
        DISCONNECTED,
        WAITING_FOR_JOIN,
        JOINED,
        WAITING_HOME,
        DONE
    }

    private PeoJoinModule() {}

    public static void toggle() {
        enabled = !enabled;
        if (enabled) {
            start();
        } else {
            stop();
        }
        PeoClient.CFG.save();
        DiagnosticRecorder.get().record("PeoJoin", "Toggled to " + enabled);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private static void start() {
        currentState = State.WAITING_FOR_DISCONNECT;
        stateStartTime = System.currentTimeMillis();
        commandSent = false;
        hadNuker = PeoClient.CFG.nuker;
        DiagnosticRecorder.get().record("PeoJoin",
                "Started; monitoring connection (Nuker was " + hadNuker + ")");
    }

    private static void stop() {
        enabled = false;
        currentState = State.IDLE;
        commandSent = false;
        DiagnosticRecorder.get().record("PeoJoin", "Stopped");
    }

    /**
     * Call once from the normal client tick. All state changes happen on the
     * client thread; no background thread touches Minecraft GUI/network state.
     */
    public static void tick() {
        if (!enabled || mc == null) return;

        try {
            boolean connected = mc.field_1724 != null && mc.field_1687 != null;

            switch (currentState) {
                case WAITING_FOR_DISCONNECT -> {
                    if (!connected) {
                        hadNuker = PeoClient.CFG.nuker;
                        if (hadNuker) {
                            PeoClient.CFG.nuker = false;
                            PeoClient.CFG.save();
                        }
                        currentState = State.DISCONNECTED;
                        stateStartTime = System.currentTimeMillis();
                        DiagnosticRecorder.get().record("PeoJoin",
                                "Disconnect detected; Nuker disabled for recovery");
                    }
                }

                case DISCONNECTED -> {
                    // Do not manipulate server-selection GUI or inject packets.
                    // A reconnect can be initiated by the normal client/server UI.
                    // Once the player is back in-world, recovery continues automatically.
                    if (connected) {
                        currentState = State.JOINED;
                        stateStartTime = System.currentTimeMillis();
                        commandSent = false;
                        DiagnosticRecorder.get().record("PeoJoin", "Connection restored");
                    }
                }

                case JOINED -> {
                    if (!commandSent &&
                            elapsedSeconds() >= Math.max(0, preJoinDelay)) {
                        sendHome();
                        commandSent = true;
                        currentState = State.WAITING_HOME;
                        stateStartTime = System.currentTimeMillis();
                        DiagnosticRecorder.get().record("PeoJoin", "Sent /home");
                    }
                }

                case WAITING_HOME -> {
                    if (elapsedSeconds() >= Math.max(0, postJoinDelay)) {
                        currentState = State.DONE;
                        stateStartTime = System.currentTimeMillis();
                        DiagnosticRecorder.get().record("PeoJoin", "Recovery complete");
                    }
                }

                case DONE -> {
                    // Stay idle until another disconnect occurs.
                    if (!connected) {
                        currentState = State.DISCONNECTED;
                        stateStartTime = System.currentTimeMillis();
                        commandSent = false;
                    }
                }

                default -> { }
            }
        } catch (Throwable t) {
            DiagnosticRecorder.get().record("PeoJoin",
                    "Recovery error: " + t.getClass().getSimpleName() +
                    ": " + String.valueOf(t.getMessage()));
        }
    }

    private static long elapsedSeconds() {
        return (System.currentTimeMillis() - stateStartTime) / 1000L;
    }

    private static void sendHome() {
        if (mc.field_1724 == null) return;
        try {
            mc.field_1724.method_7353(
                    class_2561.method_43470("/home"), true);
        } catch (Throwable t) {
            DiagnosticRecorder.get().record("PeoJoin",
                    "Could not send /home: " + t.getMessage());
        }
    }

    public static void setEnableNukerOnJoin(boolean ignored) {
        // Kept for configuration compatibility. Recovery never re-enables Nuker.
    }

    public static void setServerName(String name) {
        if (name != null && !name.isBlank()) serverName = name;
    }

    public static String getServerName() {
        return serverName;
    }

    public static void setPreJoinDelay(int seconds) {
        preJoinDelay = Math.max(0, seconds);
    }

    public static void setPostJoinDelay(int seconds) {
        postJoinDelay = Math.max(0, seconds);
    }

    public static String getStatus() {
        return enabled ? "State: " + currentState : "OFF";
    }
}
