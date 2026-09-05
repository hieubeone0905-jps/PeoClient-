package com.peoclient.nuker.optimize;

import java.util.Random;

/**
 * Load-smoothing và tạo ngẫu nhiên cho Nuker.
 * Không làm giảm tốc độ trung bình, nhưng phá vỡ pattern đều đặn.
 */
public final class NukerPacingController {
    private static final NukerPacingController INSTANCE = new NukerPacingController();
    private static final Random RANDOM = new Random();

    private long tickCounter;
    private long totalFullTicks;
    private long totalHalfTicks;

    private NukerPacingController() {}

    public static NukerPacingController get() { return INSTANCE; }

    public void tick() {
        tickCounter++;
    }

    /**
     * Điều chỉnh batch để tạo ngẫu nhiên:
     * - 40% số tick: batch = 100%
     * - 30% số tick: batch = 80-95%
     * - 20% số tick: batch = 60-80%
     * - 10% số tick: batch = 40-60% (pause giả)
     * Trung bình vẫn giữ ~85-90% hiệu suất.
     */
    public int adjustBatch(int maxBatch) {
        int safeMax = Math.max(1, maxBatch);
        double roll = RANDOM.nextDouble();

        double factor;
        if (roll < 0.40) {
            factor = 1.0; // 100% - đào mạnh
        } else if (roll < 0.70) {
            factor = 0.80 + RANDOM.nextDouble() * 0.15; // 80-95%
        } else if (roll < 0.90) {
            factor = 0.60 + RANDOM.nextDouble() * 0.20; // 60-80%
        } else {
            factor = 0.40 + RANDOM.nextDouble() * 0.20; // 40-60% (pause nhẹ)
        }

        int result = (int) Math.round(safeMax * factor);
        return Math.max(1, Math.min(safeMax, result));
    }

    public boolean isHalfLoadMode() {
        return RANDOM.nextDouble() < 0.15; // 15% số tick coi như "half load"
    }

    public long getTickCounter() { return tickCounter; }
    public long getTotalFullTicks() { return totalFullTicks; }
    public long getTotalHalfTicks() { return totalHalfTicks; }

    public void reset() {
        tickCounter = 0L;
        totalFullTicks = 0L;
        totalHalfTicks = 0L;
    }
}