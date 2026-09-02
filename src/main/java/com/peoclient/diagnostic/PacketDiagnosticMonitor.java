package com.peoclient.diagnostic;

import net.minecraft.class_2596;
import net.minecraft.class_310;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class PacketDiagnosticMonitor {
    private static final PacketDiagnosticMonitor instance = new PacketDiagnosticMonitor();
    private final ConcurrentLinkedQueue<PacketRecord> records = new ConcurrentLinkedQueue<>();
    private final AtomicLong packetCounter = new AtomicLong(0);
    private static final int MAX_RECORDS = 2000;

    private PacketDiagnosticMonitor() {}

    public static PacketDiagnosticMonitor get() { return instance; }

    public void recordPacket(Object packet) {
        if (!DiagnosticConfig.get().isRecordPackets()) return;
        if (!(packet instanceof class_2596)) return;
        String name = packet.getClass().getSimpleName();
        if (!name.startsWith("class_")) {
            // only record if it's a network packet (intermediary names start with class_)
            records.add(new PacketRecord(System.currentTimeMillis(), class_310.method_1551().field_1724 != null ? DiagnosticUtil.clientTick() : -1, name));
            if (records.size() > MAX_RECORDS) {
                records.poll();
            }
        }
    }

    public ConcurrentLinkedQueue<PacketRecord> getRecentRecords(int limit) {
        ConcurrentLinkedQueue<PacketRecord> copy = new ConcurrentLinkedQueue<>(records);
        while (copy.size() > limit) copy.poll();
        return copy;
    }

    public void clear() { records.clear(); }

    public static class PacketRecord {
        public final long timestamp;
        public final int tick;
        public final String packetName;
        public PacketRecord(long ts, int tick, String name) {
            this.timestamp = ts; this.tick = tick; this.packetName = name;
        }
    }
}