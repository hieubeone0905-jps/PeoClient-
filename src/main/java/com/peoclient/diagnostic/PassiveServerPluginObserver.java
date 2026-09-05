package com.peoclient.diagnostic;

import net.minecraft.class_310;
import net.minecraft.class_8710;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Passive server/plugin fingerprint collector.
 *
 * It does NOT send commands, chat messages, plugin queries, or any packets.
 * It only records CustomPayload identifiers that the server already sends to
 * this client. These identifiers can reveal plugin/proxy integrations, but
 * they are not a guaranteed complete list of installed server plugins.
 */
public final class PassiveServerPluginObserver {
    private static final PassiveServerPluginObserver INSTANCE = new PassiveServerPluginObserver();
    private final Set<String> observedIds = ConcurrentHashMap.newKeySet();

    private PassiveServerPluginObserver() {}

    public static PassiveServerPluginObserver get() {
        return INSTANCE;
    }

    public void observe(class_8710 payload) {
        if (payload == null) return;

        try {
            String id = String.valueOf(payload.method_56479().comp_2242());
            if (id == null || id.isBlank()) return;

            // Avoid flooding the diagnostic file with repeated payloads.
            if (observedIds.add(id)) {
                String account = "unknown";
                String server = "unknown";
                try {
                    class_310 client = class_310.method_1551();
                    if (client.method_1548() != null) {
                        account = client.method_1548().method_1676();
                    }
                    if (client.method_1562() != null && client.method_1562().method_45734() != null) {
                        server = client.method_1562().method_45734().toString();
                    }
                } catch (Throwable ignored) {}

                String kind = classify(id);
                DiagnosticRecorder.get().record(
                        "SERVER_PLUGIN_OBSERVER",
                        "ACCOUNT=" + account
                                + " SERVER=" + server
                                + " OBSERVED_CHANNEL=" + id
                                + " CLASS=" + payload.getClass().getName()
                                + " FINGERPRINT=" + kind);
            }
        } catch (Throwable ignored) {
            // Passive diagnostics must never affect packet handling.
        }
    }

    public void reset() {
        observedIds.clear();
    }

    private static String classify(String id) {
        String lower = id.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("minecraft:brand") || lower.endsWith(":brand")) return "BRAND";
        if (lower.startsWith("bungeecord:") || lower.startsWith("bungeecord")) return "BUNGEECORD";
        if (lower.startsWith("velocity:")) return "VELOCITY";
        if (lower.contains("forge")) return "FORGE_INTEGRATION";
        if (lower.contains("fml")) return "FML_INTEGRATION";
        if (lower.contains("via")) return "VIA_INTEGRATION";
        return "CUSTOM_CHANNEL";
    }
}
