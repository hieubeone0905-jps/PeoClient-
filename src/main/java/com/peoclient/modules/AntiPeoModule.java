package com.peoclient.modules;

/**
 * AntiPeo is a local compatibility/safety governor for automated actions.
 *
 * It does not spoof packets, bypass anti-cheat, or alter server-side state.
 * When enabled it spaces automated inventory/Nuker actions so that the client
 * does not burst many interactions in the same few ticks.
 */
public final class AntiPeoModule {
    private static boolean enabled;
    private static int actionDelayTicks = 3;
    private static int gateTicks;
    private static int consecutiveActions;

    private AntiPeoModule() {}

    public static void toggle() {
        enabled = !enabled;
        gateTicks = 0;
        consecutiveActions = 0;
    }

    public static boolean isEnabled() { return enabled; }

    public static int getActionDelayTicks() {
        return enabled ? Math.max(2, Math.min(20, actionDelayTicks)) : 0;
    }

    public static void setActionDelayTicks(int ticks) {
        actionDelayTicks = Math.max(2, Math.min(20, ticks));
    }

    /** Called once per client tick. */
    public static void tick() {
        if (!enabled) {
            gateTicks = 0;
            consecutiveActions = 0;
            return;
        }
        if (gateTicks > 0) gateTicks--;
        if (gateTicks == 0) consecutiveActions = 0;
    }

    /** True when an automated interaction may be issued this tick. */
    public static boolean canAct() {
        return !enabled || gateTicks <= 0;
    }

    /** Call immediately after an automated interaction has been issued. */
    public static void onAction() {
        if (!enabled) return;
        consecutiveActions++;
        // Keep a predictable minimum spacing and add one tick after repeated
        // actions to avoid a burst when a server is slow to acknowledge state.
        int delay = getActionDelayTicks();
        if (consecutiveActions >= 4) delay = Math.min(20, delay + 1);
        gateTicks = Math.max(gateTicks, delay);
    }

    public static String getStatus() {
        if (!enabled) return "OFF";
        return "SAFE / " + getActionDelayTicks() + "t";
    }
}
