// NukerBypassConfig.java
package com.peoclient.nuker;

/**
 * Bypass configuration settings - extend PeoClient.Config
 */
public final class NukerBypassConfig {
    public boolean bypassEnabled = true;
    public boolean useRotationSmooth = true;
    public boolean usePositionDesync = true;
    public boolean usePacketInjection = true;
    public boolean useEntitySpoof = false;
    public int rotationStep = 30;
    public double positionJitter = 0.0005;
    public int packetDelay = 25;
    
    private static NukerBypassConfig instance;
    
    public static NukerBypassConfig get() {
        if (instance == null) {
            instance = new NukerBypassConfig();
        }
        return instance;
    }
    
    public static void save() {
        // Save to config file
    }
}