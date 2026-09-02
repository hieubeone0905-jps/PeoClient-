package com.peoclient.diagnostic;

import net.minecraft.class_310;

/** Owns session boundaries; does not alter Nuker behavior. */
public final class SessionResetManager {
    private static final SessionResetManager instance = new SessionResetManager();
    private volatile boolean sessionActive;

    private SessionResetManager() {}
    public static SessionResetManager get() { return instance; }

    public void onConnect() {
        if (sessionActive) return;
        sessionActive = true;
        AccountSessionMetrics.get().startSession();
        ClientConnectionMonitor.get().onConnected();
        KickReasonRecorder.get().reset();
        DiagnosticRecorder.get().record("Session", "Started on " + ClientConnectionMonitor.get().getServerAddress());
        if (com.peoclient.PeoClient.CFG.nuker) NukerSessionRecorder.get().startSession();
    }

    /** Called after the disconnect reason has been captured. */
    public void finalizeDisconnect() {
        if (!sessionActive) return;
        try {
            class_310 mc = class_310.method_1551();
            DisconnectContext ctx = DisconnectContext.capture(mc);
            DisconnectContext.setLast(ctx);
            if (mc.field_1724 != null) PreDisconnectSnapshot.get().record(mc);
            NukerSessionRecorder.get().endSession();
            AccountSessionMetrics.get().recordKick();
            if (DiagnosticConfig.get().isAutoLogOnDisconnect() || DiagnosticConfig.get().isAutoLogOnKick()) KickDiagnostics.get().generateReport();
            KickComparisonRecorder.get().recordSession();
            DiagnosticRecorder.get().record("Session", "Ended");
        } catch (Throwable t) {
            DiagnosticRecorder.get().record("Session", "Finalize error: " + t.getClass().getSimpleName());
        } finally {
            sessionActive = false;
        }
    }

    public void onDisconnect() { finalizeDisconnect(); }
    public void onNukerEnable() { if (sessionActive) NukerSessionRecorder.get().startSession(); }
    public void onNukerDisable() { NukerSessionRecorder.get().endSession(); }
    public void onDimensionChange(String oldDim, String newDim) {
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.WORLD, "DIMENSION_CHANGE", oldDim + " -> " + newDim);
    }

    public void reset() {
        sessionActive = false;
        NukerSessionRecorder.get().reset();
        BreakEventRecorder.get().clear();
        TargetHistory.get().clear();
        ServerResponseMonitor.get().clear();
        PreKickSnapshot.get().clear();
        PreDisconnectSnapshot.get().clear();
        DiagnosticTimeline.get().clear();
        DisconnectContext.clear();
        KickReasonRecorder.get().reset();
    }

    public boolean isSessionActive() { return sessionActive; }
}
