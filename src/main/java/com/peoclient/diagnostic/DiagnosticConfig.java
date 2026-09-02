package com.peoclient.diagnostic;

public final class DiagnosticConfig {
    private static DiagnosticConfig instance;
    private boolean enabled = true;
    private boolean recordPackets = true;
    private boolean recordBreakEvents = true;
    private boolean recordTickMetrics = true;
    private boolean recordLatency = true;
    private boolean recordServerResponses = true;
    private boolean recordTargetHistory = true;
    private boolean recordStateTransitions = true;
    private int preKickBufferSeconds = 30;
    private int logRetentionDays = 7;
    private boolean autoLogOnKick = true;
    private boolean autoLogOnDisconnect = true;

    private DiagnosticConfig() {}

    public static DiagnosticConfig get() {
        if (instance == null) instance = new DiagnosticConfig();
        return instance;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isRecordPackets() { return recordPackets && enabled; }
    public boolean isRecordBreakEvents() { return recordBreakEvents && enabled; }
    public boolean isRecordTickMetrics() { return recordTickMetrics && enabled; }
    public boolean isRecordLatency() { return recordLatency && enabled; }
    public boolean isRecordServerResponses() { return recordServerResponses && enabled; }
    public boolean isRecordTargetHistory() { return recordTargetHistory && enabled; }
    public boolean isRecordStateTransitions() { return recordStateTransitions && enabled; }
    public int getPreKickBufferSeconds() { return preKickBufferSeconds; }
    public int getLogRetentionDays() { return logRetentionDays; }
    public boolean isAutoLogOnKick() { return autoLogOnKick && enabled; }
    public boolean isAutoLogOnDisconnect() { return autoLogOnDisconnect && enabled; }

    public void setPreKickBufferSeconds(int seconds) { this.preKickBufferSeconds = Math.max(5, Math.min(120, seconds)); }
    public void setLogRetentionDays(int days) { this.logRetentionDays = Math.max(1, days); }
    public void setAutoLogOnKick(boolean auto) { this.autoLogOnKick = auto; }
    public void setAutoLogOnDisconnect(boolean auto) { this.autoLogOnDisconnect = auto; }
}