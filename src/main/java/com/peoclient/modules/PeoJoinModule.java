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
    private static boolean sawActiveMiningWorld = false;

    private static String serverName = "Skyblock";
    private static int preJoinDelay = 5;
    private static int postJoinDelay = 5;

    private static final String COMPASS_ID = "minecraft:compass";
    private static final String SKYBLOCK_ICON_ID = "minecraft:diamond_pickaxe";

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
        lastCompassPresent = findHotbarItem(COMPASS_ID) >= 0;
        hubReturnArmed = PeoClient.CFG.nuker;
        nukerOnSince = PeoClient.CFG.nuker ? System.currentTimeMillis() : 0L;
        hadNuker = PeoClient.CFG.nuker;
        lastWorldInstance = mc.field_1687;
        sawActiveMiningWorld = PeoClient.CFG.nuker && mc.field_1687 != null && findHotbarItem(COMPASS_ID) < 0;
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
        sawActiveMiningWorld = false;
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
                        if (nukerOnSince == 0L) nukerOnSince = System.currentTimeMillis();
                    }
                }

                case WAITING_FOR_HUB -> {
                    // A normal kick-to-hub keeps the player connected. If the
                    // connection is briefly absent, simply wait for it to return.
                    if (!connected) return;
                    if (findHotbarItem(COMPASS_ID) >= 0 && mc.field_1755 == null) {
                        currentState = State.OPENING_SERVER_MENU;
                        stateStartTime = System.currentTimeMillis();
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
                    if (openCompass()) {
                        currentState = State.WAITING_FOR_SERVER_MENU;
                        stateStartTime = System.currentTimeMillis();
                        DiagnosticRecorder.get().record("PeoJoin", "Opened hub compass");
                    }
                }

                case WAITING_FOR_SERVER_MENU -> {
                    if (!connected) return;
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
                        PeoClient.CFG.nuker = true;
                        PeoClient.CFG.save();
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
        if (!hubReturnArmed || !PeoClient.CFG.nuker || mc.field_1724 == null) return;
        if (nukerOnSince == 0L) nukerOnSince = System.currentTimeMillis();

        boolean compassPresent = findHotbarItem(COMPASS_ID) >= 0;
        boolean compassAppeared = compassPresent && !lastCompassPresent;
        lastCompassPresent = compassPresent;

        Object worldNow = mc.field_1687;
        boolean reconnectedWorld = worldNow != null && lastWorldInstance == null && sawActiveMiningWorld;
        boolean worldChanged = worldNow != null && lastWorldInstance != null && worldNow != lastWorldInstance;
        if (worldNow != null) lastWorldInstance = worldNow;
        else if (PeoClient.CFG.nuker) {
            // A real kick/disconnect can briefly clear the client world before
            // the hub world is created again. Keep the recovery detector armed.
            lastWorldInstance = null;
            sawActiveMiningWorld = true;
        }

        // If the Nuker session started in a normal mining world (no hub
        // compass), remember that fact. A later compass appearance is then a
        // strong hub-return signal.
        if (PeoClient.CFG.nuker && !compassPresent && mc.field_1687 != null) {
            sawActiveMiningWorld = true;
        }

        // Some servers keep the compass in the hotbar even outside the hub.
        // In that case the compass itself does not transition. Replacing the
        // ClientWorld when the server sends the player back to hub is an
        // additional signal, but only after we have actually seen a mining
        // world, and never during the first few seconds after enabling.
        boolean validWorldReturn = (worldChanged || reconnectedWorld) && sawActiveMiningWorld && compassPresent;
        boolean validCompassReturn = compassAppeared && sawActiveMiningWorld;

        if ((!validCompassReturn && !validWorldReturn)
                || System.currentTimeMillis() - nukerOnSince < 3000L) return;

        // Mimic the configured Nuker-key toggle so all existing Nuker shutdown
        // bookkeeping (session recorder, bypass/compatibility cleanup) remains
        // intact. PeoJoin then owns the recovery sequence.
        PeoClient.toggleModuleByName("Nuker [Multi]", mc);
        currentState = State.WAITING_FOR_HUB;
        stateStartTime = System.currentTimeMillis();
        commandSent = false;
        guiSlot = -1;
        hubReturnArmed = false;
        hadNuker = true;
        DiagnosticRecorder.get().record("PeoJoin",
                "Hub return detected (compass/world transition); Nuker auto-OFF, starting recovery");
    }

    private static boolean openCompass() {
        if (mc.field_1724 == null || mc.field_1761 == null) return false;
        int slot = findHotbarItem(COMPASS_ID);
        if (slot < 0) return false;

        var inv = mc.field_1724.method_31548();
        previousHotbar = inv.field_7545;
        if (previousHotbar != slot) inv.method_61496(slot);

        // Same normal client interaction path as a real right-click on the
        // selected compass. No raw packet injection is used.
        mc.field_1761.method_2919(mc.field_1724, class_1268.field_5808);
        return true;
    }

    private static int findHotbarItem(String wantedId) {
        if (mc.field_1724 == null) return -1;
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

    private static long elapsedSeconds() {
        return (System.currentTimeMillis() - stateStartTime) / 1000L;
    }

    private static void sendHome() {
        if (mc.field_1724 == null) return;
        try {
            // method_7353 only displays a local HUD message; it does NOT send
            // a command to the server. Use the 1.21.4 ClientPlayerEntity
            // sendCommand mapping so /home actually reaches the server.
            mc.field_1724.method_44099("home");
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
