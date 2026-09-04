package com.peoclient.nuker.optimize;

/**
 * Load-smoothing controller for Nuker.
 *
 * Keeps the configured maximum batch as the normal operating level and
 * periodically uses half of that batch for one tick. This is intended to
 * smooth client/server workload without changing the configured range, target
 * selection, rotation or the maximum Nuker capability.
 */
public final class NukerPacingController {
    private static final NukerPacingController INSTANCE = new NukerPacingController();

    // Four full-load ticks, then one half-load tick.
    private static final int FULL_TICKS = 4;
    private static final int CYCLE_TICKS = 5;
    private long tickCounter;
    private long totalFullTicks;
    private long totalHalfTicks;

    private NukerPacingController() {}

    public static NukerPacingController get() { return INSTANCE; }

    /**
     * Kept for compatibility with existing call sites. Load smoothing is
     * applied to the batch size rather than denying a break start.
     */
    public boolean allowBreakStart() {
        return true;
    }

    /** Record one Nuker processing tick. */
    public void tick() {
        tickCounter++;
        if (isHalfLoadTick()) totalHalfTicks++;
        else totalFullTicks++;
    }

    /**
     * Returns the batch to process for this tick. The maximum returned value is
     * exactly the caller's configured maximum.
     */
    public int adjustBatch(int maxBatch) {
        int safeMax = Math.max(1, maxBatch);
        if (isHalfLoadTick()) return Math.max(1, (safeMax + 1) / 2);
        return safeMax;
    }

    private boolean isHalfLoadTick() {
        return tickCounter > 0 && ((tickCounter - 1) % CYCLE_TICKS) == FULL_TICKS;
    }

    public boolean isHalfLoadMode() { return isHalfLoadTick(); }
    public long getTickCounter() { return tickCounter; }
    public long getTotalFullTicks() { return totalFullTicks; }
    public long getTotalHalfTicks() { return totalHalfTicks; }

    public void reset() {
        tickCounter = 0L;
        totalFullTicks = 0L;
        totalHalfTicks = 0L;
    }
}
