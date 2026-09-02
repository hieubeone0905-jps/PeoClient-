package com.peoclient.diagnostic;

public enum BreakFailureReason {
    NOT_TARGET,
    OUT_OF_RANGE,
    RAYCAST_FAIL,
    INTERACTION_FAIL,
    BLOCK_NOT_CHANGED,
    PLAYER_MOVED,
    TARGET_CHANGED,
    SERVER_TIMEOUT,
    RECOVERY_TRIGGERED,
    UNKNOWN
}