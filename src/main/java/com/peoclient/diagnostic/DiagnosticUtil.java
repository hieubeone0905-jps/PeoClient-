package com.peoclient.diagnostic;

import net.minecraft.class_310;

/** Small mapping-safe helpers shared by diagnostic snapshots. */
public final class DiagnosticUtil {
    private static int clientTick;
    private DiagnosticUtil() {}

    public static int nextTick() { return ++clientTick; }
    public static int clientTick() { return clientTick; }

    public static String serverAddress(class_310 mc) {
        if (mc == null) return "UNKNOWN";
        try {
            if (mc.method_1558() != null && mc.method_1558().field_3761 != null) {
                return mc.method_1558().field_3761;
            }
        } catch (Throwable ignored) {}
        return "UNKNOWN";
    }

    public static int ping(class_310 mc) {
        if (mc == null) return -1;
        try {
            if (mc.method_1558() != null && mc.method_1558().field_3758 >= 0) {
                return (int)Math.min(Integer.MAX_VALUE, mc.method_1558().field_3758);
            }
        } catch (Throwable ignored) {}
        return -1;
    }
}
