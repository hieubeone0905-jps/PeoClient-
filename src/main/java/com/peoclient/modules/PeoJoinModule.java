package com.peoclient.modules;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_1268;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_7923;

/**
 * PeoJoin - optional Skyblock recovery helper.
 *
 * When enabled together with Nuker, this module is passive. It watches for the
 * user/server recovery sequence: Nuker is turned OFF (normally with the
 * configured Nuker key), then the module opens the hub compass, clicks the
 * diamond-pickaxe Skyblock entry, waits for the Skyblock world, sends /home,
 * waits again, and restores Nuker.
 *
 * It never changes Nuker range, multi, cooldown, targeting or break speed.
 */
public final class PeoJoinModule {
    private static final class_310 mc = class_310.method_1551();

    private static volatile boolean enabled = false;
    private static volatile State currentState = State.IDLE;
    private static long stateStartTime = 0L;
    private static boolean hadNuker = false;
    private static boolean commandSent = false;
    private static int previousHotbar = -1;
    private static int guiSlot = -1;
    private static boolean hubReturnArmed = false;
    private static boolean lastCompassPresent = false;
    private static long nukerOnSince = 0L;
    private static Object lastWorldInstance = null;
    private static double lastPlayerX = Double.NaN;
    private static double lastPlayerY = Double.NaN;
    private static double lastPlayerZ = Double.NaN;
    private static boolean hadMiningWorld = false;
    private static boolean sawActiveMiningWorld = false;
    private static boolean disconnectRecoveryArmed = false;
    private static long lastNukerOnChange = 0L;
    private static int stableHubTicks = 0;
    private static boolean hasLeftHub = false;
    private static boolean sawNoCompassWhileNukerOn = false;
    private static int hubProfileTicks = 0;
    private static long lastRecoveryTrigger = 0L;
    private static boolean compassRightClickSent = false;
    private static final long HUB_WAIT_MILLIS = 3000L;
    // Give the server one tick to receive the selected-slot change before the right-click.
    private static final long COMPASS_EQUIP_WAIT_MILLIS = 100L;

    private static String serverName = "Skyblock";
    private static int preJoinDelay = 5;
    private static int postJoinDelay = 5;

    private static final String COMPASS_ID = "minecraft:compass";
    private static final String SKYBLOCK_ICON_ID = "minecraft:diamond_pickaxe";
    // Server-specific Hub layout: the compass is ALWAYS hotbar slot 5 (index 4).
    private static final int HUB_COMPASS_SLOT = 4;

    public enum State {
        IDLE,
        WAITING_FOR_TRIGGER,
        WAITING_FOR_HUB,
        OPENING_SERVER_MENU,
        WAITING_FOR_SERVER_MENU,
        CLICKING_SKYBLOCK,
        WAITING_FOR_SKYBLOCK,
        WAITING_HOME,
        DONE
    }

    private PeoJoinModule() {}

    public static void toggle() {
        enabled = !enabled;
        if (enabled) start();
        else stop();
        PeoClient.CFG.save();
        DiagnosticRecorder.get().record("PeoJoin", "Toggled to " + enabled);
    }

    public static boolean isEnabled() { return enabled; }

    private static void start() {
        currentState = State.WAITING_FOR_TRIGGER;
        stateStartTime = System.currentTimeMillis();
        commandSent = false;
        guiSlot = -1;
        previousHotbar = -1;
        lastCompassPresent = isFixedHubCompassPresent();
        hubReturnArmed = PeoClient.CFG.nuker;
        nukerOnSince = PeoClient.CFG.nuker ? System.currentTimeMillis() : 0L;
        hadNuker = PeoClient.CFG.nuker;
        lastWorldInstance = mc.field_1687;
        lastPlayerX = mc.field_1724 != null ? mc.field_1724.method_23317() : Double.NaN;
        lastPlayerY = mc.field_1724 != null ? mc.field_1724.method_23318() : Double.NaN;
        lastPlayerZ = mc.field_1724 != null ? mc.field_1724.method_23321() : Double.NaN;
        hadMiningWorld = PeoClient.CFG.nuker && mc.field_1687 != null;
        sawActiveMiningWorld = hadMiningWorld;
        disconnectRecoveryArmed = false;
        lastNukerOnChange = System.currentTimeMillis();
        stableHubTicks = 0;
        hasLeftHub = !isHubProfile();
        sawNoCompassWhileNukerOn = findHotbarItem(COMPASS_ID) < 0;
        hubProfileTicks = 0;
        lastRecoveryTrigger = 0L;
        compassRightClickSent = false;
        DiagnosticRecorder.get().record("PeoJoin",
                "Started; passive until Nuker is turned OFF (Nuker was " + hadNuker + ")");
    }

    private static void stop() {
        enabled = false;
        currentState = State.IDLE;
        commandSent = false;
        guiSlot = -1;
        previousHotbar = -1;
        hubReturnArmed = false;
        lastCompassPresent = false;
        nukerOnSince = 0L;
        lastWorldInstance = null;
        lastPlayerX = lastPlayerY = lastPlayerZ = Double.NaN;
        hadMiningWorld = false;
        sawActiveMiningWorld = false;
        disconnectRecoveryArmed = false;
        stableHubTicks = 0;
        hasLeftHub = false;
        sawNoCompassWhileNukerOn = false;
        hubProfileTicks = 0;
        lastRecoveryTrigger = 0L;
        compassRightClickSent = false;
        DiagnosticRecorder.get().record("PeoJoin", "Stopped");
    }

    /** Called once from the normal client tick. */
    public static void tick() {
        if (!enabled || mc == null) return;

        try {
            boolean connected = mc.field_1724 != null && mc.field_1687 != null;

            // Automatic kick-to-hub trigger: while Nuker is actively running,
            // watch for the hub compass appearing in the hotbar after it was
            // absent during the mining session. This is the server-side hub
            // return signal used by the requested recovery flow.
            if (currentState == State.WAITING_FOR_TRIGGER && connected) {
                detectHubReturnAndDisableNuker();
            }

            switch (currentState) {
                case WAITING_FOR_TRIGGER -> {
                    // PeoJoin does nothing while Nuker is running. A transition
                    // ON -> OFF is the explicit recovery trigger requested by the user.
                    if (hadNuker && !PeoClient.CFG.nuker) {
                        currentState = connected ? State.WAITING_FOR_HUB : State.WAITING_FOR_HUB;
                        stateStartTime = System.currentTimeMillis();
                        commandSent = false;
                        guiSlot = -1;
                        DiagnosticRecorder.get().record("PeoJoin",
                                "Nuker OFF detected; starting hub -> Skyblock recovery");
                    } else if (PeoClient.CFG.nuker) {
                        hadNuker = true;
                        hubReturnArmed = true;
                        if (nukerOnSince == 0L) {
                            nukerOnSince = System.currentTimeMillis();
                            lastWorldInstance = mc.field_1687;
                            lastPlayerX = mc.field_1724 != null ? mc.field_1724.method_23317() : Double.NaN;
                            lastPlayerY = mc.field_1724 != null ? mc.field_1724.method_23318() : Double.NaN;
                            lastPlayerZ = mc.field_1724 != null ? mc.field_1724.method_23321() : Double.NaN;
                            hadMiningWorld = mc.field_1687 != null;
                            sawActiveMiningWorld = hadMiningWorld;
                        }
                    }
                }

                case WAITING_FOR_HUB -> {
                    // The user requested a deliberate 3 second pause after the
                    // return to Hub before touching the compass.
                    if (!connected) return;
                    if (elapsedMillis() < HUB_WAIT_MILLIS) return;
                    if (findHotbarItem(COMPASS_ID) >= 0 && mc.field_1755 == null) {
                        currentState = State.OPENING_SERVER_MENU;
                        stateStartTime = System.currentTimeMillis();
                        compassRightClickSent = false;
                    }
                }

                case OPENING_SERVER_MENU -> {
                    if (!connected || mc.field_1755 != null) {
                        if (mc.field_1755 != null) {
                            currentState = State.WAITING_FOR_SERVER_MENU;
                            stateStartTime = System.currentTimeMillis();
                        }
                        return;
                    }
                    // First equip the compass in the fixed slot 5. Do NOT right-click
                    // in the same tick: the server must receive the held-item change
                    // before the interaction, otherwise the server can ignore it.
                    if (equipHubCompass()) {
                        currentState = State.WAITING_FOR_SERVER_MENU;
                        stateStartTime = System.currentTimeMillis();
                        compassRightClickSent = false;
                        DiagnosticRecorder.get().record("PeoJoin",
                                "Compass equipped in fixed hotbar slot 5; waiting before right-click");
                    }
                }

                case WAITING_FOR_SERVER_MENU -> {
                    if (!connected) return;
                    // Immediately after equipping the compass, this state is used to
                    // perform exactly one normal right-click. Only do it when the
                    // compass is actually in the player's main hand.
                    if (mc.field_1755 == null && !compassRightClickSent
                            && elapsedMillis() >= COMPASS_EQUIP_WAIT_MILLIS) {
                        if (isCompassInMainHand() && rightClickHubCompass()) {
                            compassRightClickSent = true;
                            DiagnosticRecorder.get().record("PeoJoin",
                                    "Right-clicked held compass to open hub server GUI");
                        } else if (!isFixedHubCompassPresent()) {
                            currentState = State.WAITING_FOR_HUB;
                            stateStartTime = System.currentTimeMillis();
                            return;
                        }
                    }
                    if (mc.field_1755 != null) {
                        int slot = findHandledScreenItem(SKYBLOCK_ICON_ID);
                        if (slot >= 0) {
                            guiSlot = slot;
                            currentState = State.CLICKING_SKYBLOCK;
                            stateStartTime = System.currentTimeMillis();
                        } else if (elapsedSeconds() >= 8) {
                            // The server menu may be a little different. Retry the
                            // compass action after a short timeout rather than spam-clicking.
                            closeScreen();
                            currentState = State.WAITING_FOR_HUB;
                            stateStartTime = System.currentTimeMillis();
                            compassRightClickSent = false;
                        }
                    } else if (elapsedSeconds() >= 3) {
                        currentState = State.WAITING_FOR_HUB;
                        stateStartTime = System.currentTimeMillis();
                    }
                }

                case CLICKING_SKYBLOCK -> {
                    if (!connected || mc.field_1755 == null) return;
                    if (guiSlot >= 0 && clickHandledScreenSlot(guiSlot)) {
                        currentState = State.WAITING_FOR_SKYBLOCK;
                        stateStartTime = System.currentTimeMillis();
                        guiSlot = -1;
                        DiagnosticRecorder.get().record("PeoJoin",
                                "Clicked diamond-pickaxe Skyblock entry; waiting 5s");
                    } else {
                        currentState = State.WAITING_FOR_SERVER_MENU;
                        stateStartTime = System.currentTimeMillis();
                    }
                }

                case WAITING_FOR_SKYBLOCK -> {
                    if (!connected) return;
                    if (elapsedSeconds() >= Math.max(0, preJoinDelay)) {
                        sendHome();
                        commandSent = true;
                        currentState = State.WAITING_HOME;
                        stateStartTime = System.currentTimeMillis();
                        DiagnosticRecorder.get().record("PeoJoin", "Sent /home after Skyblock wait");
                    }
                }

                case WAITING_HOME -> {
                    if (!connected) return;
                    if (elapsedSeconds() >= Math.max(0, postJoinDelay)) {
                        enableNukerDirectly();
                        hadNuker = true;
                        currentState = State.DONE;
                        stateStartTime = System.currentTimeMillis();
                        DiagnosticRecorder.get().record("PeoJoin", "Recovery complete; Nuker re-enabled");
                    }
                }

                case DONE -> {
                    // Stay passive until the next explicit Nuker OFF transition.
                    if (PeoClient.CFG.nuker) hadNuker = true;
                    else {
                        hadNuker = true;
                        currentState = State.WAITING_FOR_TRIGGER;
                        stateStartTime = System.currentTimeMillis();
                    }
                }

                case IDLE -> { }
            }
        } catch (Throwable t) {
            DiagnosticRecorder.get().record("PeoJoin",
                    "Recovery error: " + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
        }
    }


    /**
     * Detect a return to the hub while Nuker is still ON, then perform the
     * same logical action as pressing the Nuker key: turn Nuker OFF and start
     * PeoJoin recovery immediately. This avoids requiring a manual key press.
     */
    private static void detectHubReturnAndDisableNuker() {
        if (!hubReturnArmed || !PeoClient.CFG.nuker || mc.field_1724 == null || mc.field_1687 == null) return;

        // This server has a fixed Hub layout: compass is ALWAYS slot 5.
        // Do not scan the hotbar and do not require a previous "no compass"
        // observation. A kick back to Hub can keep the same ClientWorld, so
        // the fixed slot is the reliable server-specific signal.
        if (!isFixedHubCompassPresent()) return;

        // Avoid firing immediately if PeoJoin was enabled while already in Hub.
        // Nuker must have been genuinely active for at least 8 seconds first.
        if (nukerOnSince <= 0L || System.currentTimeMillis() - nukerOnSince < 8000L) return;
        if (System.currentTimeMillis() - lastRecoveryTrigger < 10000L) return;

        // Require the compass to be stable for 3 ticks before triggering.
        stableHubTicks++;
        if (stableHubTicks < 3) return;
        stableHubTicks = 0;
        lastRecoveryTrigger = System.currentTimeMillis();

        // Turn Nuker OFF directly. This is intentionally independent of M.
        PeoClient.CFG.nuker = false;
        com.peoclient.diagnostic.NukerSessionRecorder.get().endSession();
        PeoClient.CFG.save();

        currentState = State.WAITING_FOR_HUB;
        stateStartTime = System.currentTimeMillis();
        commandSent = false;
        guiSlot = -1;
        hubReturnArmed = false;
        disconnectRecoveryArmed = false;
        hadNuker = true;
        DiagnosticRecorder.get().record("PeoJoin",
                "HUB DETECTED: fixed compass slot 5; Nuker forced OFF; waiting 3s before right-click");
    }

    private static boolean isHubProfile() {
        return isFixedHubCompassPresent();
    }

    private static boolean isFixedHubCompassPresent() {
        if (mc.field_1724 == null) return false;
        var inv = mc.field_1724.method_31548();
        class_1799 stack = inv.method_5438(HUB_COMPASS_SLOT);
        return !stack.method_7960() && COMPASS_ID.equals(itemId(stack));
    }

    /**
     * Called by the disconnect observer. A hub transfer may briefly tear down
     * the play connection without taking the user to the multiplayer screen.
     * Keep PeoJoin armed so the next hub world can finish the recovery flow.
     */
    public static void onDisconnectObserved() {
        if (!enabled || !PeoClient.CFG.nuker) return;
        disconnectRecoveryArmed = true;
        lastWorldInstance = null;
        lastPlayerX = lastPlayerY = lastPlayerZ = Double.NaN;
        hadMiningWorld = true;
        sawActiveMiningWorld = true;
        DiagnosticRecorder.get().record("PeoJoin",
                "Disconnect/transfer observed while Nuker ON; waiting for hub return");
    }

    private static boolean equipHubCompass() {
        if (mc.field_1724 == null) return false;
        // The compass is immovable and permanently occupies hotbar slot 5.
        // Slot indexes are zero-based, so slot 5 is index 4.
        if (!isFixedHubCompassPresent()) return false;
        var inv = mc.field_1724.method_31548();
        previousHotbar = inv.field_7545;
        if (previousHotbar != HUB_COMPASS_SLOT) {
            inv.method_61496(HUB_COMPASS_SLOT);
        }
        return isCompassInMainHand();
    }

    private static boolean isCompassInMainHand() {
        if (mc.field_1724 == null) return false;
        class_1799 held = mc.field_1724.method_6047();
        return !held.method_7960() && COMPASS_ID.equals(itemId(held));
    }

    private static boolean rightClickHubCompass() {
        if (mc.field_1724 == null || mc.field_1761 == null) return false;
        if (!isCompassInMainHand()) return false;
        try {
            // Exactly the normal client interaction used for right-clicking the
            // selected item in the player's hand. No keybind or alternate GUI path.
            mc.field_1761.method_2919(mc.field_1724, class_1268.field_5808);
            return true;
        } catch (Throwable t) {
            DiagnosticRecorder.get().record("PeoJoin",
                    "Compass right-click failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    private static int findHotbarItem(String wantedId) {
        if (mc.field_1724 == null) return -1;
        // The Hub compass is server-fixed to slot 5. Keep this method compatible
        // with existing callers but never search other slots for the compass.
        if (COMPASS_ID.equals(wantedId)) return isFixedHubCompassPresent() ? HUB_COMPASS_SLOT : -1;
        var inv = mc.field_1724.method_31548();
        for (int slot = 0; slot < 9; slot++) {
            class_1799 stack = inv.method_5438(slot);
            if (!stack.method_7960() && wantedId.equals(itemId(stack))) return slot;
        }
        return -1;
    }

    private static int findHandledScreenItem(String wantedId) {
        if (mc.field_1724 == null || mc.field_1724.field_7512 == null) return -1;
        var handler = mc.field_1724.field_7512;
        for (int i = 0; i < 100; i++) {
            try {
                class_1799 stack = handler.method_7611(i).method_7677();
                if (!stack.method_7960() && wantedId.equals(itemId(stack))) return i;
            } catch (Throwable ignored) {
                break;
            }
        }
        return -1;
    }

    private static boolean clickHandledScreenSlot(int slot) {
        if (mc.field_1724 == null || mc.field_1761 == null || mc.field_1724.field_7512 == null) return false;
        if (mc.field_1755 == null) return false;
        try {
            mc.field_1761.method_2906(
                    mc.field_1724.field_7512.field_7763,
                    slot,
                    0,
                    class_1713.field_7790,
                    mc.field_1724);
            return true;
        } catch (Throwable t) {
            DiagnosticRecorder.get().record("PeoJoin", "Skyblock GUI click failed: " + t.getMessage());
            return false;
        }
    }

    private static void closeScreen() {
        try {
            mc.method_1507(null);
        } catch (Throwable ignored) { }
    }

    private static long elapsedMillis() {
        return System.currentTimeMillis() - stateStartTime;
    }

    private static long elapsedSeconds() {
        return elapsedMillis() / 1000L;
    }

    private static void enableNukerDirectly() {
        if (!PeoClient.CFG.nuker) {
            PeoClient.CFG.nuker = true;
            com.peoclient.diagnostic.NukerSessionRecorder.get().startSession();
            PeoClient.CFG.save();
            DiagnosticRecorder.get().record("PeoJoin", "Nuker forced ON directly (equivalent to M toggle)");
        }
    }

    private static void sendHome() {
        if (mc.field_1724 == null) return;
        try {
            // method_7353 only displays a local HUD message; it does NOT send
            // a command to the server. Use the 1.21.4 ClientPlayerEntity
            // sendCommand mapping so /home actually reaches the server.
            if (mc.method_1562() != null) {
                mc.method_1562().method_45730("home");
            }
        } catch (Throwable t) {
            DiagnosticRecorder.get().record("PeoJoin", "Could not send /home: " + t.getMessage());
        }
    }

    private static String itemId(class_1799 stack) {
        return class_7923.field_41178.method_10221(stack.method_7909()).toString();
    }

    public static void setEnableNukerOnJoin(boolean ignored) {
        // Kept for configuration compatibility. PeoJoin re-enables Nuker only
        // after the complete requested sequence has finished.
    }

    public static void setServerName(String name) {
        if (name != null && !name.isBlank()) serverName = name;
    }

    public static String getServerName() { return serverName; }

    public static void setPreJoinDelay(int seconds) { preJoinDelay = Math.max(0, seconds); }
    public static void setPostJoinDelay(int seconds) { postJoinDelay = Math.max(0, seconds); }

    public static String getStatus() {
        return enabled ? "State: " + currentState : "OFF";
    }
}
