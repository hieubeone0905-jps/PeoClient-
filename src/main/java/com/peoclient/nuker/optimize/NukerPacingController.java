package com.peoclient.nuker.optimize;

import com.peoclient.diagnostic.LatencyMetrics;

/**
 * Adaptive pacing for Nuker's block-start actions.
 *
 * It deliberately leaves Nuker's range, multi, target selection and block
 * break mechanics untouched. Only sustained bursts of new break-start
 * requests are paced so the server is not continuously hit with an
 * unusually high action rate.
 */
public final class NukerPacingController {
    private static final NukerPacingController INSTANCE = new NukerPacingController();

    private static final long WINDOW_NS = 1_000_000_000L;
    private static final long MIN_PAUSE_NS = 250_000_000L;

    private long windowStartNs;
    private long pauseUntilNs;
    private int startsInWindow;
    private int burstStarts;
    private int hotWindows;
    private boolean sustainMode;
    private long lastStartNs;
    private long totalStarts;

    private NukerPacingController() {}

    public static NukerPacingController get() { return INSTANCE; }

    /**
     * Called immediately before creating a new client-side block breaking start
     * action. A denied start simply lets the current Minecraft tick end.
     */
    public boolean allowBreakStart() {
        long now = System.nanoTime();
        rollWindow(now);

        if (now < pauseUntilNs) return false;

        // Short burst pacing: allow four consecutive starts, then yield briefly.
        // This keeps the Nuker's configured range/multi/targeting intact while
        // preventing an uninterrupted stream of break-start work.
        int burstLimit = sustainMode ? 3 : 4;
        if (burstStarts >= burstLimit) {
            burstStarts = 0;
            pauseUntilNs = now + (sustainMode ? 140_000_000L : 90_000_000L);
            return false;
        }

        int ping = LatencyMetrics.get().getLastPing();
        if (ping < 0) ping = 50;

        int softLimit = sustainMode ? 7 : 10;
        int hardLimit = sustainMode ? 10 : 14;
        long pause = sustainMode ? 1_000_000_000L : 700_000_000L;

        if (ping >= 300) {
            softLimit -= 2;
            hardLimit -= 2;
            pause += 300_000_000L;
        } else if (ping >= 200) {
            softLimit -= 1;
            hardLimit -= 1;
            pause += 150_000_000L;
        }

        softLimit = Math.max(4, softLimit);
        hardLimit = Math.max(6, hardLimit);

        if (startsInWindow >= hardLimit) {
            pauseUntilNs = now + Math.max(MIN_PAUSE_NS, pause);
            hotWindows++;
            return false;
        }

        if (startsInWindow >= softLimit) {
            // Require the burst to stay hot across multiple windows before
            // entering sustain mode. This preserves short high-power bursts.
            if (startsInWindow > softLimit + 3) hotWindows++;
            if (hotWindows >= 3) sustainMode = true;
            pauseUntilNs = now + Math.max(MIN_PAUSE_NS, pause / 2);
            return false;
        }

        return true;
    }

    public void recordBreakStart() {
        long now = System.nanoTime();
        rollWindow(now);
        startsInWindow++;
        burstStarts++;
        totalStarts++;
        lastStartNs = now;
    }

    private void rollWindow(long now) {
        if (windowStartNs == 0L) {
            windowStartNs = now;
            return;
        }
        if (now - windowStartNs < WINDOW_NS) return;

        if (startsInWindow <= 6) {
            hotWindows = Math.max(0, hotWindows - 1);
            if (hotWindows == 0) sustainMode = false;
        }

        windowStartNs = now;
        startsInWindow = 0;
        burstStarts = 0;
    }

    public boolean isSustainMode() { return sustainMode; }
    public int getStartsInWindow() { return startsInWindow; }
    public long getLastStartNs() { return lastStartNs; }
    public long getTotalStarts() { return totalStarts; }
    public long getRemainingPauseMs() {
        long remaining = pauseUntilNs - System.nanoTime();
        return remaining <= 0L ? 0L : (remaining / 1_000_000L);
    }

    public void reset() {
        windowStartNs = 0L;
        pauseUntilNs = 0L;
        startsInWindow = 0;
        burstStarts = 0;
        hotWindows = 0;
        sustainMode = false;
        lastStartNs = 0L;
        totalStarts = 0L;
    }
}
