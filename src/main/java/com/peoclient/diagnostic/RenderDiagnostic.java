package com.peoclient.diagnostic;

import net.minecraft.class_2338;
import net.minecraft.class_2680;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary render/world-state diagnostic for the Nuker ghost-block issue.
 * This class is observational only: it never changes world state or packets.
 */
public final class RenderDiagnostic {
    private static final Logger LOG = LoggerFactory.getLogger("PeoClient-RenderDiagnostic");

    private RenderDiagnostic() {}

    public static void logNukerBefore(class_2338 pos, class_2680 oldState) {
        LOG.info("[PeoRenderDebug] NUKER_BEFORE pos={} old={}", pos, oldState);
    }

    public static void logNukerAfter(class_2338 pos, class_2680 oldState, class_2680 actualState) {
        LOG.info("[PeoRenderDebug] NUKER_AFTER pos={} old={} actual={} changed={}",
                pos, oldState, actualState, oldState != null && !oldState.equals(actualState));
    }
}
