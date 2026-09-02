package com.peoclient.diagnostic;

import net.minecraft.class_2338;
import net.minecraft.class_310;

/**
 * Event diagnostic chuẩn, immutable.
 */
public final class DiagnosticEvent {
    private final long timestamp;
    private final int clientTick;
    private final Category category;
    private final String eventType;
    private final String message;
    private final class_2338 position;
    private final class_2338 targetBlock;
    private final String server;
    private final int ping;
    private final String nukerState;

    public enum Category {
        CONNECTION, DISCONNECT, NUKER, BREAK, TARGET, WORLD, PLAYER, PACKET,
        LATENCY, TICK, SERVER, INVENTORY, ERROR
    }

    private DiagnosticEvent(Builder b) {
        this.timestamp = b.timestamp;
        this.clientTick = b.clientTick;
        this.category = b.category;
        this.eventType = b.eventType;
        this.message = b.message;
        this.position = b.position;
        this.targetBlock = b.targetBlock;
        this.server = b.server;
        this.ping = b.ping;
        this.nukerState = b.nukerState;
    }

    public static class Builder {
        long timestamp = System.currentTimeMillis();
        int clientTick = -1;
        Category category;
        String eventType;
        String message;
        class_2338 position;
        class_2338 targetBlock;
        String server;
        int ping = -1;
        String nukerState;

        public Builder timestamp(long ts) { this.timestamp = ts; return this; }
        public Builder clientTick(int tick) { this.clientTick = tick; return this; }
        public Builder category(Category cat) { this.category = cat; return this; }
        public Builder eventType(String type) { this.eventType = type; return this; }
        public Builder message(String msg) { this.message = msg; return this; }
        public Builder position(class_2338 pos) { this.position = pos; return this; }
        public Builder targetBlock(class_2338 pos) { this.targetBlock = pos; return this; }
        public Builder server(String s) { this.server = s; return this; }
        public Builder ping(int p) { this.ping = p; return this; }
        public Builder nukerState(String state) { this.nukerState = state; return this; }

        public DiagnosticEvent build() {
            if (category == null) category = Category.ERROR;
            if (clientTick == -1 && class_310.method_1551().field_1724 != null)
                clientTick = DiagnosticUtil.clientTick();
            return new DiagnosticEvent(this);
        }
    }

    // Getters
    public long getTimestamp() { return timestamp; }
    public int getClientTick() { return clientTick; }
    public Category getCategory() { return category; }
    public String getEventType() { return eventType; }
    public String getMessage() { return message; }
    public class_2338 getPosition() { return position; }
    public class_2338 getTargetBlock() { return targetBlock; }
    public String getServer() { return server; }
    public int getPing() { return ping; }
    public String getNukerState() { return nukerState; }
}