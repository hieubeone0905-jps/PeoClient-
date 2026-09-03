package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import com.peoclient.modules.AntiVipProMaxModule;

/** Read-only health summary for Nuker + AntiVipProMax coexistence. */
public final class NukerIntegrationStatus {
    private NukerIntegrationStatus() {}

    public static boolean isHealthy() {
        if (!PeoClient.CFG.nuker) return true;
        return !NukerWorldSync.isWatched(PeoClient.NukerLogic.getCurrentTarget());
    }

    public static String summary() {
        return String.format("Nuker=%s AntiVip=%s WorldSync=%d",
                PeoClient.CFG.nuker ? "ON" : "OFF",
                AntiVipProMaxModule.isEnabled() ? "ON" : "OFF",
                NukerWorldSync.getStaleRecoveries());
    }
}
