// NukerBypassIntegration.java
package com.peoclient.nuker;

import com.peoclient.PeoClient;
import net.minecraft.client.MinecraftClient;

/**
 * Integration hook for existing PeoClient NukerLogic
 * Replace NukerLogic.tick() call with this integration
 */
public final class NukerBypassIntegration {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static int tickCounter = 0;
    
    /**
     * Call this instead of NukerLogic.tick() when bypass is enabled
     */
    public static void tick() {
        if (mc.player == null || mc.world == null) return;
        
        tickCounter++;
        
        // Check if bypass should be used
        if (PeoClient.CFG.nuker && NukerBypassConfig.get().bypassEnabled) {
            // Use bypass for all blocks except maybe bedrock/obsidian
            if (shouldUseBypass()) {
                NukerBypassManager.tick();
                return;
            }
        }
        
        // Fallback to normal NukerLogic
        if (tickCounter % 2 == 0) {
            com.peoclient.PeoClient.NukerLogic.tick(mc);
        }
    }
    
    private static boolean shouldUseBypass() {
        // Use bypass for most blocks
        // Can add conditions for specific block types
        return true;
    }
}