package com.peoclient.diagnostic;

import net.minecraft.class_310;

/**
 * Theo dõi lifecycle connection phía client.
 */
public final class ClientConnectionMonitor {
    private static final ClientConnectionMonitor instance = new ClientConnectionMonitor();
    private volatile State currentState = State.DISCONNECTED;
    private volatile String serverAddress = "";
    private volatile long connectionStart = 0;
    private volatile long connectedAt = 0;
    private volatile long disconnectAt = 0;
    private volatile String account = "";

    public enum State { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING }

    private ClientConnectionMonitor() {}

    public static ClientConnectionMonitor get() { return instance; }

    public void onConnecting(String address) {
        serverAddress = address;
        connectionStart = System.currentTimeMillis();
        currentState = State.CONNECTING;
        account = class_310.method_1551().method_1548() != null ? class_310.method_1551().method_1548().method_1676() : "UNKNOWN";
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.CONNECTION, "CONNECTING", address);
    }

    public void onConnected() {
        class_310 mc = class_310.method_1551();
        if ((serverAddress == null || serverAddress.isBlank()) && mc.field_1724 != null) {
            serverAddress = DiagnosticUtil.serverAddress(mc);
        }
        if (account == null || account.isBlank()) {
            account = mc.method_1548() != null ? mc.method_1548().method_1676() : "UNKNOWN";
        }
        connectedAt = System.currentTimeMillis();
        currentState = State.CONNECTED;
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.CONNECTION, "CONNECTED", "Connected to " + serverAddress);
    }

    public void onDisconnecting() {
        currentState = State.DISCONNECTING;
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.CONNECTION, "DISCONNECTING", "Disconnecting from " + serverAddress);
    }

    public void onDisconnected(String reason) {
        disconnectAt = System.currentTimeMillis();
        currentState = State.DISCONNECTED;
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.DISCONNECT, "DISCONNECTED", reason != null ? reason : "No reason");
    }

    public State getState() { return currentState; }
    public String getServerAddress() { return serverAddress; }
    public long getConnectionStart() { return connectionStart; }
    public long getConnectedAt() { return connectedAt; }
    public long getDisconnectAt() { return disconnectAt; }
    public String getAccount() { return account; }

    public void reset() {
        currentState = State.DISCONNECTED;
        serverAddress = "";
        connectionStart = 0;
        connectedAt = 0;
        disconnectAt = 0;
        account = "";
    }
}