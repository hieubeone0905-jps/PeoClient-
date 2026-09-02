package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.BreakFailureReason;
import com.peoclient.diagnostic.DiagnosticEvent;
import com.peoclient.diagnostic.DiagnosticTimeline;
import net.minecraft.class_2338;
import net.minecraft.class_310;

/**
 * Tracks Nuker state and coordinates recovery that is already owned by NukerLogic.
 * It never inserts delays or changes Nuker strength.
 */
public final class RecoveryManager {
    private static final RecoveryManager INSTANCE = new RecoveryManager();
    private volatile State state = State.NORMAL;
    private volatile class_2338 currentTarget;
    private volatile int stagnantTicks;
    private volatile int recoveryAttempts;
    private volatile long lastRecoveryTime;

    private RecoveryManager() {}
    public static RecoveryManager get() { return INSTANCE; }

    public enum State {
        NORMAL, BREAKING, PROGRESSING, STAGNANT, FAILED, RECOVERY, RETRY, TARGET_CHANGED
    }

    public synchronized void onTargetChanged(class_2338 target) {
        if (target == null || target.equals(currentTarget)) return;
        currentTarget = target;
        stagnantTicks = 0;
        state = State.TARGET_CHANGED;
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.TARGET, "SELECTED", target.toString());
    }

    public synchronized void onBreakAttempt(class_2338 target) {
        currentTarget = target;
        state = State.BREAKING;
        stagnantTicks = 0;
    }

    public synchronized void onProgress(class_310 mc, class_2338 target, float progress, boolean isAir) {
        if (!PeoClient.CFG.nuker) { reset(); return; }
        if (target != null && !target.equals(currentTarget)) onTargetChanged(target);
        if (target == null) { state = State.NORMAL; return; }
        if (isAir || progress >= 1.0f) {
            state = State.NORMAL;
            stagnantTicks = 0;
            recoveryAttempts = 0;
            return;
        }
        if (progress > 0.0f) {
            state = State.PROGRESSING;
            stagnantTicks = 0;
        } else {
            stagnantTicks++;
            if (stagnantTicks >= 1) state = State.STAGNANT;
        }
    }

    public synchronized void onFailure(class_2338 target, BreakFailureReason reason) {
        currentTarget = target;
        state = State.FAILED;
        stagnantTicks = 0;
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.BREAK, "CLASSIFIED_FAILURE",
                (reason == null ? BreakFailureReason.UNKNOWN : reason).name());
    }

    /** Called after NukerLogic has performed its own vanilla-safe recovery. */
    public synchronized void onRecovery(class_2338 target, String reason) {
        currentTarget = target;
        state = State.RECOVERY;
        recoveryAttempts++;
        lastRecoveryTime = System.currentTimeMillis();
        stagnantTicks = 0;
        DiagnosticTimeline.get().record(DiagnosticEvent.Category.NUKER, "RECOVERY",
                reason == null ? "Nuker recovery" : reason);
        state = State.RETRY;
    }

    public synchronized State getState() { return state; }
    public synchronized class_2338 getCurrentTarget() { return currentTarget; }
    public synchronized int getStagnantTicks() { return stagnantTicks; }
    public synchronized int getRecoveryAttempts() { return recoveryAttempts; }
    public synchronized long getLastRecoveryTime() { return lastRecoveryTime; }

    public synchronized void reset() {
        state = State.NORMAL;
        currentTarget = null;
        stagnantTicks = 0;
        recoveryAttempts = 0;
        lastRecoveryTime = 0L;
    }
}
