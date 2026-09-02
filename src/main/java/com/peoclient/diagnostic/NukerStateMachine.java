package com.peoclient.diagnostic;

import net.minecraft.class_2338;

public final class NukerStateMachine {
    private static final NukerStateMachine instance = new NukerStateMachine();
    private volatile State current = State.IDLE;
    private volatile class_2338 currentTarget;
    private volatile long stateEnterTime = 0;
    private volatile int stateDurationTicks = 0;

    private NukerStateMachine() {}

    public static NukerStateMachine get() { return instance; }

    public enum State {
        IDLE, TARGETING, BREAKING, WAITING_SERVER, SUCCESS, FAILURE, RECOVERY
    }

    public synchronized void transition(State newState, class_2338 target) {
        if (current != newState) {
            long now = System.currentTimeMillis();
            if (stateEnterTime > 0) {
                stateDurationTicks = (int)((now - stateEnterTime) / 50);
            }
            current = newState;
            currentTarget = target;
            stateEnterTime = now;
            stateDurationTicks = 0;
            // record for snapshot
            PreKickSnapshot.get().record("STATE: " + current + " target=" + (target != null ? target.toString() : "null"));
        }
    }

    public synchronized State getState() { return current; }
    public synchronized class_2338 getCurrentTarget() { return currentTarget; }
    public synchronized int getStateDurationTicks() { return stateDurationTicks; }
}