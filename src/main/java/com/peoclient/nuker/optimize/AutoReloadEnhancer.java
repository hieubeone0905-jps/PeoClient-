package com.peoclient.nuker.optimize;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_2338;

/**
 * Compatibility facade for the new optimization layer.
 * Delegates ghost-block recovery to the existing AutoBlockReload engine so
 * there is only one reload queue and one recovery producer.
 */
public final class AutoReloadEnhancer {
    private static boolean enabled;

    private AutoReloadEnhancer() {}

    public static void start() {
        if (enabled) return;
        enabled = true;
        if (PeoClient.CFG.autoBlockReload && !AutoBlockReload.isActive()) {
            AutoBlockReload.start();
        }
        DiagnosticRecorder.get().record("AutoReloadEnhancer", "Started (delegating to AutoBlockReload)");
    }

    public static void stop() {
        enabled = false;
        DiagnosticRecorder.get().record("AutoReloadEnhancer", "Stopped");
    }

    public static boolean isEnabled() {
        return enabled && PeoClient.CFG.nuker;
    }

    public static void tick() {
        if (!isEnabled()) return;
        if (PeoClient.CFG.autoBlockReload) {
            if (!AutoBlockReload.isActive()) AutoBlockReload.start();
        } else {
            return;
        }
        // Do not call AutoBlockReload.tick() here: NukerLogic already owns
        // that tick, preventing duplicate reload attempts.
    }

    public static int getQueueSize() {
        return AutoBlockReload.getQueueSize();
    }

    public static void queueReload(class_2338 pos) {
        if (pos != null && isEnabled()) AutoBlockReload.queueReload(pos);
    }

    public static void clearQueue() {
        AutoBlockReload.clearQueue();
    }
}
