package com.peoclient.diagnostic;

import net.minecraft.class_310;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ring buffer lưu trạng thái trước disconnect (tập trung vào STATE, không chỉ event).
 * Mặc định 30 giây, dung lượng tự điều chỉnh.
 */
public final class PreDisconnectSnapshot {
    private static final PreDisconnectSnapshot instance = new PreDisconnectSnapshot();
    private final ConcurrentLinkedQueue<StateSnapshot> ring = new ConcurrentLinkedQueue<>();
    private final AtomicInteger maxEntries = new AtomicInteger(600); // 30s * 20 ticks

    private PreDisconnectSnapshot() {}

    public static PreDisconnectSnapshot get() { return instance; }

    private void updateCapacity() {
        int seconds = DiagnosticConfig.get().getPreKickBufferSeconds();
        maxEntries.set(Math.max(10, seconds * 20));
    }

    public void record(class_310 mc) {
        updateCapacity();
        StateSnapshot snap = StateSnapshot.capture(mc);
        ring.add(snap);
        while (ring.size() > maxEntries.get()) {
            ring.poll();
        }
    }

    public void record() {
        record(class_310.method_1551());
    }

    public ConcurrentLinkedQueue<StateSnapshot> getRecentSnapshots(int seconds) {
        int limit = Math.max(10, seconds * 20);
        ConcurrentLinkedQueue<StateSnapshot> result = new ConcurrentLinkedQueue<>();
        Object[] values = ring.toArray();
        int start = Math.max(0, values.length - limit);
        for (int i = start; i < values.length; i++) result.add((StateSnapshot) values[i]);
        return result;
    }

    public ConcurrentLinkedQueue<StateSnapshot> getAll() {
        return new ConcurrentLinkedQueue<>(ring);
    }

    public void clear() { ring.clear(); }
    public int size() { return ring.size(); }

    public static class StateSnapshot {
        public final long timestamp;
        public final int clientTick;
        public final double x, y, z;
        public final float yaw, pitch;
        public final boolean onGround;
        public final int ping;
        public final String nukerState;
        public final class_2338 target;
        public final float health;
        public final int food;
        public final int breakAttempts;
        public final int breakSuccesses;
        public final int breakFailures;
        public final int recoveries;

        private StateSnapshot(long ts, int tick, double x, double y, double z, float yaw, float pitch,
                              boolean onGround, int ping, String nukerState, class_2338 target,
                              float health, int food, int attempts, int successes, int failures, int rec) {
            this.timestamp = ts; this.clientTick = tick; this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch; this.onGround = onGround; this.ping = ping;
            this.nukerState = nukerState; this.target = target; this.health = health; this.food = food;
            this.breakAttempts = attempts; this.breakSuccesses = successes;
            this.breakFailures = failures; this.recoveries = rec;
        }

        public static StateSnapshot capture(class_310 mc) {
            long ts = System.currentTimeMillis();
            int tick = mc.field_1724 != null ? DiagnosticUtil.clientTick() : -1;
            double x = 0, y = 0, z = 0;
            float yaw = 0, pitch = 0;
            boolean onGround = false;
            int ping = -1;
            String nukerState = "IDLE";
            class_2338 target = null;
            float health = 0;
            int food = 0;
            int attempts = 0, successes = 0, failures = 0, rec = 0;

            if (mc.field_1724 != null) {
                class_243 pos = mc.field_1724.method_19538();
                x = pos.field_1352; y = pos.field_1351; z = pos.field_1350;
                yaw = mc.field_1724.method_36454();
                pitch = mc.field_1724.method_36455();
                onGround = mc.field_1724.method_24828();
                ping = DiagnosticUtil.ping(mc);
                health = mc.field_1724.method_6032();
                food = mc.field_1724.method_7344().method_7586();
            }
            nukerState = BreakStateTracker.get().getState().name();
            target = BreakStateTracker.get().getCurrentTarget();
            attempts = AccountSessionMetrics.get().getBreakAttempts();
            successes = AccountSessionMetrics.get().getBreakSuccesses();
            failures = AccountSessionMetrics.get().getBreakFailures();
            rec = AccountSessionMetrics.get().getRecoveries();

            return new StateSnapshot(ts, tick, x, y, z, yaw, pitch, onGround, ping,
                    nukerState, target, health, food, attempts, successes, failures, rec);
        }
    }
}