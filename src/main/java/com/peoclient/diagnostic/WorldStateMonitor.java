package com.peoclient.diagnostic;

import net.minecraft.class_310;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_2248;
import net.minecraft.class_7923;
import net.minecraft.class_243;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Theo dõi trạng thái block mục tiêu, chỉ đọc world.
 */
public final class WorldStateMonitor {
    private static final WorldStateMonitor instance = new WorldStateMonitor();
    private final ConcurrentLinkedQueue<TargetState> history = new ConcurrentLinkedQueue<>();
    private static final int MAX = 200;

    private WorldStateMonitor() {}

    public static WorldStateMonitor get() { return instance; }

    public void recordTarget(class_2338 pos) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1687 == null) return;
        class_2680 state = mc.field_1687.method_8320(pos);
        String blockId = state.method_26204() != null ? class_7923.field_41175.method_10221(state.method_26204()).toString() : "UNKNOWN";
        double dist = mc.field_1724 != null ? mc.field_1724.method_33571().method_1022(class_243.method_24953(pos)) : -1;
        history.add(new TargetState(System.currentTimeMillis(), mc.field_1724 != null ? DiagnosticUtil.clientTick() : -1, pos, blockId, state.method_26215(), state.method_26214(mc.field_1687, pos), dist));
        trim();
    }

    public void recordCheck(class_2338 pos) {
        // Called after break attempt to verify state
        class_310 mc = class_310.method_1551();
        if (mc.field_1687 == null) return;
        class_2680 state = mc.field_1687.method_8320(pos);
        String blockId = state.method_26204() != null ? class_7923.field_41175.method_10221(state.method_26204()).toString() : "UNKNOWN";
        double dist = mc.field_1724 != null ? mc.field_1724.method_33571().method_1022(class_243.method_24953(pos)) : -1;
        history.add(new TargetState(System.currentTimeMillis(), mc.field_1724 != null ? DiagnosticUtil.clientTick() : -1, pos, blockId, state.method_26215(), state.method_26214(mc.field_1687, pos), dist, "POST_BREAK"));
        trim();
    }

    private void trim() { while (history.size() > MAX) history.poll(); }

    public ConcurrentLinkedQueue<TargetState> getHistory() { return new ConcurrentLinkedQueue<>(history); }
    public void clear() { history.clear(); }

    public static class TargetState {
        public final long timestamp;
        public final int clientTick;
        public final class_2338 pos;
        public final String blockId;
        public final boolean isAir;
        public final float hardness;
        public final double distance;
        public final String context;
        public TargetState(long ts, int tick, class_2338 p, String id, boolean air, float hard, double dist) {
            this(ts, tick, p, id, air, hard, dist, "TARGET");
        }
        public TargetState(long ts, int tick, class_2338 p, String id, boolean air, float hard, double dist, String ctx) {
            this.timestamp = ts; this.clientTick = tick; this.pos = p; this.blockId = id;
            this.isAir = air; this.hardness = hard; this.distance = dist; this.context = ctx;
        }
    }
}