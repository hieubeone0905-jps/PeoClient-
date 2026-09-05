package com.peoclient.nuker.optimize;

public final class NukerPacingController {
    private static final NukerPacingController INSTANCE = new NukerPacingController();
    private long tickCounter;
    private long totalFullTicks;
    private long totalHalfTicks;

    private NukerPacingController() {}

    public static NukerPacingController get() { return INSTANCE; }

    public void tick() {
        tickCounter++;
    }

    public int adjustBatch(int maxBatch) {
        int noise = (int)(Math.random() * 2);
        int result = maxBatch - noise;
        return Math.max(1, result);
    }

    public boolean isHalfLoadMode() { return false; }
    public long getTickCounter() { return tickCounter; }
    public long getTotalFullTicks() { return totalFullTicks; }
    public long getTotalHalfTicks() { return totalHalfTicks; }

    public void reset() {
        tickCounter = 0L;
        totalFullTicks = 0L;
        totalHalfTicks = 0L;
    }
}