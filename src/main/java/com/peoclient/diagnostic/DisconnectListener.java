package com.peoclient.diagnostic;

import net.minecraft.class_9812;
import com.peoclient.modules.PeoJoinModule;

/** Client-side disconnect finalizer. It observes only; it never alters network state. */
public final class DisconnectListener {
    private static boolean registered = false;
    private static boolean handling = false;

    private DisconnectListener() {}

    public static void register() { registered = true; }
    public static boolean isRegistered() { return registered; }

    public static synchronized void onDisconnect(class_9812 info) {
        if (handling) return;
        handling = true;
        try {
            String reason = info != null && info.comp_2853() != null ? info.comp_2853().getString() : "Unknown reason";
            KickReasonRecorder.get().recordReason(reason);
            ClientConnectionMonitor.get().onDisconnected(reason);
            SessionResetManager.get().finalizeDisconnect();
            PeoJoinModule.onDisconnectObserved();
        } finally {
            handling = false;
        }
    }
}
