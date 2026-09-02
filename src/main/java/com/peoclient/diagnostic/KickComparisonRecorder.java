package com.peoclient.diagnostic;

import java.util.concurrent.ConcurrentHashMap;

/** Stores immutable per-account session snapshots for later comparison. */
public final class KickComparisonRecorder {
    private static final KickComparisonRecorder instance = new KickComparisonRecorder();
    private final ConcurrentHashMap<String, AccountSessionSnapshot> sessions = new ConcurrentHashMap<>();

    private KickComparisonRecorder() {}
    public static KickComparisonRecorder get() { return instance; }

    public void recordSession() {
        AccountSessionSnapshot snapshot = AccountSessionSnapshot.fromCurrent();
        sessions.put(snapshot.getAccount(), snapshot);
    }

    public AccountSessionSnapshot getSnapshotForAccount(String account) { return sessions.get(account); }
    public void clear() { sessions.clear(); }
}
