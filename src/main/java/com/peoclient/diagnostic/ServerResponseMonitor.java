package com.peoclient.diagnostic;

import net.minecraft.class_2338;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ServerResponseMonitor {
    private static final ServerResponseMonitor instance = new ServerResponseMonitor();
    private final ConcurrentLinkedQueue<ServerResponse> responses = new ConcurrentLinkedQueue<>();
    private static final int MAX_RESPONSES = 200;

    private ServerResponseMonitor() {}

    public static ServerResponseMonitor get() { return instance; }

    public void recordBlockUpdate(class_2338 pos, boolean becameAir) {
        if (!DiagnosticConfig.get().isRecordServerResponses()) return;
        responses.add(new ServerResponse(System.currentTimeMillis(), "BLOCK_UPDATE", pos, becameAir ? "AIR" : "CHANGED", null));
        trim();
    }

    public void recordPositionCorrection(double dx, double dy, double dz) {
        if (!DiagnosticConfig.get().isRecordServerResponses()) return;
        responses.add(new ServerResponse(System.currentTimeMillis(), "POSITION_CORRECTION", null, String.format("Δ%.3f,%.3f,%.3f", dx, dy, dz), null));
        trim();
    }

    public void recordInventoryCorrection() {
        if (!DiagnosticConfig.get().isRecordServerResponses()) return;
        responses.add(new ServerResponse(System.currentTimeMillis(), "INVENTORY_CORRECTION", null, null, null));
        trim();
    }

    public void recordDisconnect(String reason) {
        responses.add(new ServerResponse(System.currentTimeMillis(), "DISCONNECT", null, reason, null));
        trim();
    }

    private void trim() { while (responses.size() > MAX_RESPONSES) responses.poll(); }

    public ConcurrentLinkedQueue<ServerResponse> getResponses() { return new ConcurrentLinkedQueue<>(responses); }
    public void clear() { responses.clear(); }

    public static class ServerResponse {
        public final long timestamp;
        public final String type;
        public final class_2338 pos;
        public final String detail;
        public final Object extra;
        public ServerResponse(long ts, String type, class_2338 pos, String detail, Object extra) {
            this.timestamp = ts; this.type = type; this.pos = pos; this.detail = detail; this.extra = extra;
        }
    }
}