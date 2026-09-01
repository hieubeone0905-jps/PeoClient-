// BypassKeyHandler.java
package com.peoclient.nuker;

import com.peoclient.PeoClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Keybind for toggle bypass mode
 */
public class BypassKeyHandler {
    private static final String CATEGORY = "PeoClient";
    private static KeyBinding bypassKey;
    private static boolean bypassToggled = false;
    
    public static void register() {
        bypassKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Toggle Nuker Bypass", 
                InputUtil.Type.KEYSYM, 
                GLFW.GLFW_KEY_B, 
                CATEGORY)
        );
    }
    
    public static void tick() {
        if (bypassKey == null) return;
        
        while (bypassKey.wasPressed()) {
            NukerBypassConfig config = NukerBypassConfig.get();
            config.bypassEnabled = !config.bypassEnabled;
            bypassToggled = config.bypassEnabled;
            
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                    net.minecraft.text.Text.literal(
                        "§7[§ePeoClient§7] Nuker Bypass " + 
                        (config.bypassEnabled ? "§aENABLED" : "§cDISABLED")
                    ),
                    false
                );
            }
            
            if (!config.bypassEnabled) {
                NukerBypassManager.reset();
            }
        }
    }
    
    public static boolean isBypassToggled() {
        return bypassToggled;
    }
}