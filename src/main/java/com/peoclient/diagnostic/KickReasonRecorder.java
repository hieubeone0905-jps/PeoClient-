package com.peoclient.diagnostic;

import net.minecraft.class_2561;
import net.minecraft.class_9812;

public final class KickReasonRecorder {
    private static final KickReasonRecorder instance = new KickReasonRecorder();
    private volatile String lastKickReason = null;
    private volatile long kickTimestamp = 0;
    private volatile boolean wasKicked = false;

    private KickReasonRecorder() {}
    public static KickReasonRecorder get() { return instance; }

    public void recordKick(class_2561 reason) {
        recordReason(reason != null ? reason.getString() : "Unknown reason");
    }

    public void recordKick(class_9812 info) {
        class_2561 reason = info != null ? info.comp_2853() : null;
        recordKick(reason);
    }

    public void recordReason(String reason) {
        lastKickReason = reason == null || reason.isBlank() ? "Unknown reason" : reason;
        kickTimestamp = System.currentTimeMillis();
        wasKicked = true;
        String account = "unknown";
        String server = "unknown";
        try {
            net.minecraft.class_310 client = net.minecraft.class_310.method_1551();
            if (client.method_1548() != null) account = client.method_1548().method_1676();
            if (client.method_1562() != null && client.method_1562().method_45734() != null) {
                server = client.method_1562().method_45734().toString();
            }
        } catch (Throwable ignored) {}
        String detail = "ACCOUNT=" + account + " SERVER=" + server + " REASON=" + lastKickReason;
        DiagnosticRecorder.get().record("DISCONNECT", detail);
        DiagnosticRecorder.get().record("ACCOUNT_KICK", detail);
        ServerResponseMonitor.get().recordDisconnect(lastKickReason);
    }

    public String getLastKickReason() { return lastKickReason; }
    public long getKickTimestamp() { return kickTimestamp; }
    public boolean wasKicked() { return wasKicked; }
    public void reset() { wasKicked = false; lastKickReason = null; kickTimestamp = 0; }
}
