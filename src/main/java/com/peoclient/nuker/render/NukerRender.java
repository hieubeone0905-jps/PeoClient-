package com.peoclient.nuker.render;

public final class NukerRender {
    private static int pendingWorldChanges = 0;

    public static void addWorldChange() {
        pendingWorldChanges++;
    }

    public static int getPendingWorldChanges() {
        return pendingWorldChanges;
    }

    public static void resetPending() {
        pendingWorldChanges = 0;
    }

    public static void flush() {
        // Tùy chỉnh
    }
}