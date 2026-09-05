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

    /**
     * Luôn trả về maxBatch (không giảm tốc độ), chỉ thêm random 0-1 để tránh pattern.
     */
    public int adjustBatch(int maxBatch) {
        int noise = (int)(Math.random() * 2); // 0 hoặc 1
        int result = maxBatch - noise; // có thể giảm 1 block ngẫu nhiên
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