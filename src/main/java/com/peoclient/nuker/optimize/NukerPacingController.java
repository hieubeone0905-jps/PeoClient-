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
        if (isHalfLoadTick()) totalHalfTicks++;
        else totalFullTicks++;
    }

    public int adjustBatch(int maxBatch) {
        int cycle = (int)(tickCounter % 7);
        double factor = switch (cycle) {
            case 0 -> 1.0;
            case 1 -> 0.9;
            case 2 -> 0.75;
            case 3 -> 0.85;
            case 4 -> 0.9;
            case 5 -> 0.95;
            default -> 1.0;
        };
        // Thêm nhiễu ngẫu nhiên
        double noise = 0.92 + 0.16 * Math.random();
        int result = (int) Math.round(maxBatch * factor * noise);
        return Math.max(2, Math.min(maxBatch, result));
    }

    private boolean isHalfLoadTick() {
        return tickCounter > 0 && ((tickCounter - 1) % 5) == 3;
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