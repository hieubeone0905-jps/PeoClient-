package com.peoclient.diagnostic;

import com.peoclient.PeoClient;
import com.peoclient.modules.AntiVipProMaxModule;
import net.minecraft.class_2338;
import java.nio.file.Path;
import java.util.Date;

public final class KickDiagnostics {
    private static final KickDiagnostics instance = new KickDiagnostics();
    private KickDiagnostics() {}
    public static KickDiagnostics get() { return instance; }

    public void generateReport() {
        if (!DiagnosticConfig.get().isAutoLogOnDisconnect() && !DiagnosticConfig.get().isAutoLogOnKick()) return;
        String account = AccountSessionMetrics.get().getAccountName();
        Path logFile = KickLogManager.get().createLogFile(account);
        KickLogManager.get().writeLog(logFile, buildReport());
        DiagnosticRecorder.get().record("DISCONNECT_REPORT", logFile.toString());
    }

    public String buildReport() {
        DisconnectContext ctx = DisconnectContext.getLast();
        StringBuilder sb = new StringBuilder(16384);
        sb.append("===== PEOCLIENT DISCONNECT REPORT =====\n");
        sb.append("Timestamp: ").append(new Date()).append("\n\n");
        sb.append("--- CONNECTION ---\n");
        sb.append("Account: ").append(AccountSessionMetrics.get().getAccountName()).append('\n');
        sb.append("Server: ").append(ClientConnectionMonitor.get().getServerAddress()).append('\n');
        sb.append("State: ").append(ClientConnectionMonitor.get().getState()).append('\n');
        sb.append("Reason: ").append(KickReasonRecorder.get().getLastKickReason()).append("\n\n");

        if (ctx != null) {
            sb.append("--- DISCONNECT CONTEXT ---\n");
            sb.append("Dimension: ").append(ctx.getDimension()).append('\n');
            sb.append(String.format("Position: %.3f %.3f %.3f%n", ctx.getX(), ctx.getY(), ctx.getZ()));
            sb.append(String.format("Velocity: %.4f %.4f %.4f%n", ctx.getVelocityX(), ctx.getVelocityY(), ctx.getVelocityZ()));
            sb.append("Rotation: ").append(ctx.getYaw()).append(" / ").append(ctx.getPitch()).append('\n');
            sb.append("OnGround: ").append(ctx.isOnGround()).append('\n');
            sb.append("Slot: ").append(ctx.getSelectedSlot()).append('\n');
            sb.append("Health: ").append(ctx.getHealth()).append(" Food: ").append(ctx.getFood()).append('\n');
            sb.append("Ping: ").append(ctx.getPing()).append("ms\n");
            sb.append("Nuker enabled: ").append(ctx.isNukerEnabled()).append('\n');
            sb.append("Nuker state: ").append(ctx.getNukerState()).append('\n');
            sb.append("Target: ").append(ctx.getTargetPosition()).append(" / ").append(ctx.getTargetBlock()).append("\n\n");
        }

        sb.append("--- SESSION ---\n");
        sb.append("Duration: ").append(AccountSessionMetrics.get().getSessionDurationMs() / 1000).append("s\n");
        sb.append("Ticks: ").append(AccountSessionMetrics.get().getTotalTicks()).append('\n');
        sb.append("Attempts: ").append(AccountSessionMetrics.get().getBreakAttempts()).append('\n');
        sb.append("Successes: ").append(AccountSessionMetrics.get().getBreakSuccesses()).append('\n');
        sb.append("Failures: ").append(AccountSessionMetrics.get().getBreakFailures()).append('\n');
        sb.append("Recoveries: ").append(AccountSessionMetrics.get().getRecoveries()).append("\n\n");

        sb.append("--- NUKER SETTINGS ---\n");
        sb.append("Mode: ").append(PeoClient.CFG.nukerMode).append('\n');
        sb.append("Multi: ").append(PeoClient.CFG.nukerMulti).append('\n');
        sb.append("Cooldown: ").append(PeoClient.CFG.nukerCooldown).append('\n');
        sb.append("Range: ").append(PeoClient.CFG.nukerRange).append('\n');
        sb.append("Shape: ").append(PeoClient.CFG.nukerShape).append('\n');
        sb.append("Sort: ").append(PeoClient.CFG.nukerSort).append('\n');
        sb.append("Filter: ").append(PeoClient.CFG.nukerFilter).append('\n');
        sb.append("Raycast: ").append(PeoClient.CFG.nukerRaycast).append('\n');
        sb.append("Rotate: ").append(PeoClient.CFG.nukerRotate).append("\n\n");

        sb.append("--- ANTIVIPROMAX ---\n");
        sb.append("Enabled: ").append(AntiVipProMaxModule.isEnabled()).append('\n');
        sb.append("Grim: ").append(AntiVipProMaxModule.isGrimMode()).append('\n');
        sb.append("Vulcan: ").append(AntiVipProMaxModule.isVulcanMode()).append('\n');
        sb.append("Intensity: ").append(AntiVipProMaxModule.getIntensity()).append('\n');
        sb.append("Auto Adjust: ").append(AntiVipProMaxModule.isAutoAdjust()).append("\n\n");

        class_2338 target = BreakStateTracker.get().getCurrentTarget();
        sb.append("--- BREAK STATE ---\n");
        sb.append("State: ").append(BreakStateTracker.get().getState()).append('\n');
        sb.append("Target: ").append(target).append('\n');
        sb.append("State ticks: ").append(BreakStateTracker.get().getStateDurationTicks()).append("\n\n");

        sb.append("--- LATENCY ---\n");
        sb.append("Last: ").append(LatencyMetrics.get().getLastPing()).append("ms\n");
        sb.append("Average: ").append(String.format("%.2f", LatencyMetrics.get().getAveragePing())).append("ms\n");
        sb.append("Min: ").append(LatencyMetrics.get().getMinPing()).append("ms\n");
        sb.append("Max: ").append(LatencyMetrics.get().getMaxPing()).append("ms\n\n");

        sb.append("--- TICK METRICS ---\n");
        sb.append("Avg: ").append(String.format("%.2f", TickMetrics.get().getAverageTickMs())).append("ms\n");
        sb.append("Min: ").append(String.format("%.2f", TickMetrics.get().getMinTickMs())).append("ms\n");
        sb.append("Max: ").append(String.format("%.2f", TickMetrics.get().getMaxTickMs())).append("ms\n\n");

        sb.append("--- PRE-DISCONNECT STATE ---\n");
        for (PreDisconnectSnapshot.StateSnapshot x : PreDisconnectSnapshot.get().getAll()) {
            sb.append(x.timestamp).append(" tick=").append(x.clientTick)
              .append(" pos=").append(String.format("%.2f %.2f %.2f", x.x, x.y, x.z))
              .append(" state=").append(x.nukerState)
              .append(" target=").append(x.target)
              .append(" ping=").append(x.ping).append('\n');
        }

        sb.append("\n--- PRE-KICK EVENTS ---\n");
        for (PreKickSnapshot.SnapshotEntry e : PreKickSnapshot.get().getSnapshot())
            sb.append(new Date(e.timestamp)).append(" ").append(e.event).append('\n');

        sb.append("\n--- TIMELINE ---\n");
        for (DiagnosticEvent e : DiagnosticTimeline.get().getRecentEvents()) {
            sb.append(e.getTimestamp()).append(" [").append(e.getCategory()).append("] ")
              .append(e.getEventType()).append(" ").append(e.getMessage()).append('\n');
        }

        // === Thông tin server (từ các nguồn có sẵn) ===
        sb.append("\n--- SERVER INFO ---\n");
        sb.append("Address: ").append(ClientConnectionMonitor.get().getServerAddress()).append('\n');
        sb.append("Connection duration: ").append((System.currentTimeMillis() - ClientConnectionMonitor.get().getConnectedAt()) / 1000).append("s\n");
        sb.append("Account: ").append(AccountSessionMetrics.get().getAccountName()).append('\n');

        // Thêm thông tin từ diagnostic khác nếu có
        sb.append("\n--- DIAGNOSTIC SUMMARY ---\n");
        sb.append("Total break attempts: ").append(AccountSessionMetrics.get().getBreakAttempts()).append('\n');
        sb.append("Total successes: ").append(AccountSessionMetrics.get().getBreakSuccesses()).append('\n');
        sb.append("Total failures: ").append(AccountSessionMetrics.get().getBreakFailures()).append('\n');
        sb.append("Recoveries: ").append(AccountSessionMetrics.get().getRecoveries()).append('\n');
        sb.append("Ping (last): ").append(LatencyMetrics.get().getLastPing()).append("ms\n");

        sb.append("\n===== END REPORT =====\n");
        return sb.toString();
    }
}