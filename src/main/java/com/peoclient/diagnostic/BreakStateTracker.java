package com.peoclient.diagnostic;

import net.minecraft.class_2338;

public final class BreakStateTracker {
    private static final BreakStateTracker instance = new BreakStateTracker();
    private volatile State currentState = State.IDLE;
    private volatile class_2338 currentTarget;
    private volatile long stateStartTime = 0;
    private volatile int stateDurationTicks = 0;

    private BreakStateTracker() {}

    public static BreakStateTracker get() { return instance; }

    public enum State {
        IDLE, TARGETING, BREAKING, WAITING_SERVER, SUCCESS, FAILURE, RECOVERY
    }

    public synchronized void transition(State newState, class_2338 target) {
        if (currentState != newState) {
            long now = System.currentTimeMillis();
            if (stateStartTime > 0) {
                stateDurationTicks = (int)((now - stateStartTime) / 50);
            }
            currentState = newState;
            currentTarget = target;
            stateStartTime = now;
            stateDurationTicks = 0;
        }
    }

    public synchronized State getState() { return currentState; }
    public synchronized class_2338 getCurrentTarget() { return currentTarget; }
    public synchronized int getStateDurationTicks() { return stateDurationTicks; }
    public synchronized long getStateStartTime() { return stateStartTime; }
}