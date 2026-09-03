package com.peoclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.peoclient.inventory.InventoryCleaner;
import com.peoclient.modules.AntiVipProMaxModule;
import com.peoclient.modules.PeoJoinModule;
import com.peoclient.nuker.compat.NukerCompatibility;
import com.peoclient.nuker.compat.SafeCompatibilityDiagnostics;
import com.peoclient.nuker.compat.NukerAreaLimiter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.class_1268;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_2404;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3675;
import net.minecraft.class_3965;
import net.minecraft.class_7923;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PeoClient implements ClientModInitializer {
    public static final Config CFG = new Config();

    public static class_304 menuKey, xrayKey, nukerKey, fullbrightKey, cleanerKey, nukerBypassKey;
    public static final Map<String, class_304> MODULE_KEYS = new LinkedHashMap<>();
    private static String originalUsername = "Player";
    private int saveTick;
    private boolean networkConfigured;

    @Override
    public void onInitializeClient() {
        originalUsername = class_310.method_1551().method_1548().method_1676();
        CFG.load();

        // Diagnostic systems are observation-only and are initialized before the client tick.
        com.peoclient.diagnostic.DiagnosticRecorder.get().init();
        com.peoclient.diagnostic.KickLogManager.get().init();
        com.peoclient.diagnostic.KickLogManager.get().cleanupOldLogs(com.peoclient.diagnostic.DiagnosticConfig.get().getLogRetentionDays());
        com.peoclient.diagnostic.DisconnectListener.register();
        com.peoclient.diagnostic.DiagnosticHealthMonitor.get().checkHealth();

        menuKey = key("PeoClient Hub", GLFW.GLFW_KEY_RIGHT_SHIFT);
        registerModuleKeys();
        PeoJoinModule.setEnableNukerOnJoin(true);
        xrayKey = MODULE_KEYS.get("X-Ray");
        nukerKey = MODULE_KEYS.get("Nuker [Multi]");
        fullbrightKey = MODULE_KEYS.get("Fullbright");
        cleanerKey = MODULE_KEYS.get("InventoryCleaner");
        nukerBypassKey = key("PeoClient Nuker Compatibility", GLFW.GLFW_KEY_B);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        NukerAreaLimiter.registerRender();
        HudRenderCallback.EVENT.register((draw, delta) -> Hud.render(draw));
    }

    private class_304 key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new class_304(name, class_3675.class_307.field_1668, code, "PeoClient"));
    }

    private void registerModuleKeys() {
        for (String module : PoeScreen.MODULES) {
            int defaultCode = switch (module) {
                case "Fullbright" -> GLFW.GLFW_KEY_F;
                case "InventoryCleaner" -> GLFW.GLFW_KEY_I;
                case "Nuker [Multi]" -> GLFW.GLFW_KEY_N;
                case "X-Ray" -> GLFW.GLFW_KEY_X;
                case "AntiVipProMax" -> GLFW.GLFW_KEY_C;
                case "PeoJoin" -> GLFW.GLFW_KEY_P;
                default -> GLFW.GLFW_KEY_UNKNOWN;
            };
            Integer stored = CFG.keybinds.get(module);
            int code = stored != null ? stored : defaultCode;
            MODULE_KEYS.put(module, key("PeoClient " + module, code));
        }
    }

    public static boolean setModuleKey(String module, int keyCode) {
        class_304 key = MODULE_KEYS.get(module);
        if (key == null) return false;
        key.method_1422(class_3675.class_307.field_1668.method_1447(keyCode));
        // Rebuild the static key-code lookup immediately. Without this call,
        // Minecraft can keep the old binding until the next client restart.
        class_304.method_1426();
        CFG.keybinds.put(module, keyCode);
        class_310.method_1551().field_1690.method_1640();
        CFG.save();
        return true;
    }

    public static int getModuleKeyCode(String module) {
        Integer stored = CFG.keybinds.get(module);
        if (stored != null) return stored;
        return switch (module) {
            case "Fullbright" -> GLFW.GLFW_KEY_F;
            case "InventoryCleaner" -> GLFW.GLFW_KEY_I;
            case "Nuker [Multi]" -> GLFW.GLFW_KEY_N;
            case "X-Ray" -> GLFW.GLFW_KEY_X;
            case "AntiVipProMax" -> GLFW.GLFW_KEY_C;
                case "PeoJoin" -> GLFW.GLFW_KEY_P;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }

    private void tick(class_310 mc) {
        if (!networkConfigured) {
            networkConfigured = true;
            if (CFG.usernameOverride != null && !CFG.usernameOverride.isBlank()) setUsernameOverride(CFG.usernameOverride);
            applyProxySettings(mc);
        }

        while (menuKey.method_1436()) mc.method_1507(new PoeScreen());

        for (Map.Entry<String, class_304> entry : MODULE_KEYS.entrySet()) {
            while (entry.getValue().method_1436()) toggleModuleByName(entry.getKey(), mc);
        }
        while (nukerBypassKey.method_1436()) {
            NukerCompatibility.toggle();
            if (mc.field_1724 != null) mc.field_1724.method_7353(class_2561.method_43470("Nuker Compatibility: " + (NukerCompatibility.isEnabled() ? "ON" : "OFF")), true);
        }

        // PeoJoin runs before the in-world early return so it can observe disconnects.
        PeoJoinModule.tick();

        if (mc.field_1724 == null || mc.field_1687 == null) {
            if (++saveTick >= 100) {
                saveTick = 0;
                CFG.save();
            }
            return;
        }

        FullbrightLogic.tick(mc);

        // Targeted client-side render resync for server block updates.

        NukerAreaLimiter.tick(mc, CFG.nukerRangeHighlight, CFG.nukerRange);
        if (CFG.nuker) {
            if (!com.peoclient.diagnostic.NukerSessionRecorder.get().isActive()) {
                com.peoclient.diagnostic.NukerSessionRecorder.get().startSession();
            }
            NukerCompatibility.tick(mc);
        } else if (com.peoclient.diagnostic.NukerSessionRecorder.get().isActive()) {
            com.peoclient.diagnostic.NukerSessionRecorder.get().endSession();
        }
        SafeCompatibilityDiagnostics.tick();
        AntiVipProMaxModule.tick();
        if (com.peoclient.diagnostic.DiagnosticUtil.clientTick() % 20 == 0) {
            com.peoclient.diagnostic.PreDisconnectSnapshot.get().record(mc);
            com.peoclient.diagnostic.LatencyMetrics.get().updatePing();
        }
        if (CFG.cleaner) InventoryCleaner.tick(mc);

        // Keep PeoClient diagnostic output synchronized to disk while in-game.
        if (com.peoclient.diagnostic.DiagnosticConfig.get().isEnabled()) {
            com.peoclient.diagnostic.DiagnosticRecorder.get().flush();
        }

        if (++saveTick >= 100) {
            saveTick = 0;
            CFG.save();
        }
    }

    public static void toggleModuleByName(String module, class_310 mc) {
        switch (module) {
            case "X-Ray" -> toggleXray(mc);
            case "Nuker [Multi]" -> {
                CFG.nuker = !CFG.nuker;
                if (CFG.nuker) com.peoclient.diagnostic.NukerSessionRecorder.get().startSession();
                else com.peoclient.diagnostic.NukerSessionRecorder.get().endSession();
            }
            case "Fullbright" -> toggleFullbright(mc);
            case "InventoryCleaner" -> CFG.cleaner = !CFG.cleaner;
            case "AntiVipProMax" -> AntiVipProMaxModule.toggle();
            case "PeoJoin" -> PeoJoinModule.toggle();
            default -> { /* reserved for modules that are not implemented yet */ }
        }
        CFG.save();
    }

    public static void toggleXray(class_310 mc) {
        CFG.xray = !CFG.xray;
        // Wurst-style X-Ray: when enabled, render only blocks selected in the X-Ray filter.
        // Keep the legacy flag synchronized for old configs/UI, but do not use it
        // as the render gate; the mixin now checks CFG.xrayBlocks directly.
        CFG.xraySkyOnly = CFG.xray;
        if (mc.field_1687 != null) {
            reload(mc);
            // A second rebuild ensures already-built chunk meshes are discarded
            // after the visibility rule changes. X-Ray is the only feature that
            // needs this extra render rebuild.
            reload(mc);
        }
        CFG.save();
    }

    public static void toggleFullbright(class_310 mc) {
        CFG.fullbright = !CFG.fullbright;
        if (!CFG.fullbright) FullbrightLogic.restore(mc);
        reload(mc);
    }

    public static void reload(class_310 mc) {
        if (mc.field_1769 != null) mc.field_1769.method_3279();
    }

    public static boolean isXrayBlock(class_2248 block) {
        return CFG.xrayBlocks.contains(class_7923.field_41175.method_10221(block).toString());
    }

    public static final class Config {
        public boolean xray = false, nuker = false, fullbright = false, cleaner = false;
        public boolean antiVipProMax = false;
        public boolean antiVipProMaxGrim = true;
        public boolean antiVipProMaxVulcan = true;
        public int antiVipProMaxIntensity = 5;
        public boolean antiVipProMaxAutoAdjust = true;
        public boolean antiVipProMaxAutoRecovery = true;
        public Map<String, Integer> keybinds = new LinkedHashMap<>();

        // Account/network settings.  A username override only changes the client-side
        // display/session value; it does not authenticate a different online account.
        public String usernameOverride = "";
        public boolean randomProxy = false;
        public List<String> proxyList = new ArrayList<>();
        public List<String> savedAccounts = new ArrayList<>();

        // Wurst/LiquidBounce-style X-Ray settings.
        // Wurst-style X-Ray settings: block list, exposed-only and opacity.
        // Fluids are included in the target list, matching Wurst's default X-Ray list.
        public boolean xrayFullBright = true;
        /** Performance mode: suppress all ordinary terrain block geometry so the sky remains. */
        public boolean xraySkyOnly = true;
        public boolean xrayExposedOnly = false;
        public boolean xrayFluids = true;
        public int xrayBackgroundOpacity = 0;

        public int xrayPresetVersion = 0;
        public Set<String> xrayBlocks = new LinkedHashSet<>(Arrays.asList(
                "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
                "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
                "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
                "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
                "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
                "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
                "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                "minecraft:nether_quartz_ore", "minecraft:nether_gold_ore",
                "minecraft:ancient_debris"
        ));

        // BleachHack-inspired Nuker settings.
        public String nukerMode = "Normal";
        public int nukerMulti = 2;
        public int nukerCooldown = 0;
        public String nukerShape = "Sphere";
        public double nukerRange = 4.2;
        public String nukerSort = "Closest";
        public boolean nukerFilter = false;
        public boolean nukerWhitelist = false;
        public String nukerFilterIds = "";
        public boolean nukerRaycast = true;
        public boolean nukerFlatten = false;
        public boolean nukerRotate = true;
        public boolean nukerNoParticles = false;
        public boolean nukerHighlight = false;
        public String nukerHighlightMode = "Opacity";
        public String nukerHighlightColor = "255,128,128";
        public boolean nukerRangeHighlight = false;
        public double nukerRangeWidth = 3.0;
        public String nukerRangeColor = "255,0,0";

        // Wurst Fullbright settings.
        public String fullbrightMethod = "Gamma";
        public boolean fullbrightFade = true;
        public double fullbrightDefaultBrightness = 0.5;
        public double fullbrightBrightness = 16.0;

        // LiquidBounce-inspired InventoryCleaner settings.
        public boolean cleanerGreedy = true;
        public boolean cleanerMergeStacks = true;
        public boolean cleanerTouchHotbar = false;
        /** When enabled, only items in cleanerDropFilter are discarded. */
        public boolean cleanerFilterOnly = false;
        public Set<String> cleanerDropFilter = new LinkedHashSet<>();
        public int cleanerActionDelay = 0;
        // Keep at least a small server acknowledgement window so fast disposal
        // does not outrun the server and create client/server ghost items.
        public int cleanerAckTimeout = 1;
        public int maxBlocks = 512, maxArrows = 128, maxThrowables = 64, maxFoods = 200;
        public int maxWaterBuckets = 2, maxLavaBuckets = 2, maxMilkBuckets = 2;
        public Set<String> cleanerBlacklistSet = new LinkedHashSet<>();
        public String itemsBlacklist = "";
        public String offHandItem = "SHIELD";
        public String[] slotItems = {
                "WEAPON", "BOW", "PICKAXE", "AXE", "NONE",
                "POTION", "FOOD", "BLOCK", "BLOCK"
        };

        private Path path() {
            return class_310.method_1551().field_1697.toPath()
                    .resolve("config/peoclient.json");
        }

        public void load() {
            try {
                Path p = path();
                if (!Files.exists(p)) {
                    save();
                    return;
                }

                Config c = new Gson().fromJson(Files.readString(p), Config.class);
                if (c == null) return;

                // Copy nullable collections/arrays safely so old configs remain usable.
                xrayBlocks = c.xrayBlocks != null ? c.xrayBlocks : xrayBlocks;
                // Existing configs from v1 used a broad Wurst-style block list.
                // Upgrade that list once to the requested all-ores preset.
                if (c.xrayPresetVersion < 2) {
                    xrayBlocks = defaultOreBlocks();
                    xrayPresetVersion = 2;
                } else {
                    xrayPresetVersion = c.xrayPresetVersion;
                }
                cleanerBlacklistSet = c.cleanerBlacklistSet != null
                        ? c.cleanerBlacklistSet : cleanerBlacklistSet;
                slotItems = c.slotItems != null ? c.slotItems : slotItems;

                xray = c.xray; nuker = c.nuker; fullbright = c.fullbright; cleaner = c.cleaner;
                antiVipProMax = c.antiVipProMax;
                antiVipProMaxGrim = c.antiVipProMaxGrim;
                antiVipProMaxVulcan = c.antiVipProMaxVulcan;
                antiVipProMaxIntensity = Math.max(1, Math.min(10, c.antiVipProMaxIntensity));
                antiVipProMaxAutoAdjust = c.antiVipProMaxAutoAdjust;
                antiVipProMaxAutoRecovery = c.antiVipProMaxAutoRecovery;
                keybinds = c.keybinds != null ? new LinkedHashMap<>(c.keybinds) : new LinkedHashMap<>();
                usernameOverride = c.usernameOverride != null ? c.usernameOverride : "";
                randomProxy = c.randomProxy;
                proxyList = c.proxyList != null ? c.proxyList : proxyList;
                savedAccounts = c.savedAccounts != null ? c.savedAccounts : savedAccounts;
                xrayFullBright = c.xrayFullBright;
                xraySkyOnly = c.xraySkyOnly;
                xrayExposedOnly = c.xrayExposedOnly;
                xrayFluids = c.xrayFluids;
                xrayBackgroundOpacity = c.xrayBackgroundOpacity;

                nukerMode = c.nukerMode; nukerMulti = c.nukerMulti;
                nukerCooldown = c.nukerCooldown; nukerShape = c.nukerShape;
                nukerRange = c.nukerRange; nukerSort = c.nukerSort;
                nukerFilter = c.nukerFilter; nukerWhitelist = c.nukerWhitelist;
                nukerFilterIds = c.nukerFilterIds; nukerRaycast = c.nukerRaycast;
                nukerFlatten = c.nukerFlatten; nukerRotate = c.nukerRotate;
                nukerNoParticles = c.nukerNoParticles;
                nukerHighlight = c.nukerHighlight;
                nukerHighlightMode = c.nukerHighlightMode;
                nukerHighlightColor = c.nukerHighlightColor;
                nukerRangeHighlight = c.nukerRangeHighlight;
                nukerRangeWidth = c.nukerRangeWidth;
                nukerRangeColor = c.nukerRangeColor;

                fullbrightMethod = c.fullbrightMethod;
                fullbrightFade = c.fullbrightFade;
                fullbrightDefaultBrightness = c.fullbrightDefaultBrightness;
                fullbrightBrightness = c.fullbrightBrightness;

                cleanerGreedy = c.cleanerGreedy;
                cleanerMergeStacks = c.cleanerMergeStacks;
                cleanerTouchHotbar = c.cleanerTouchHotbar;
                cleanerFilterOnly = c.cleanerFilterOnly;
                cleanerDropFilter = c.cleanerDropFilter != null
                        ? new LinkedHashSet<>(c.cleanerDropFilter) : new LinkedHashSet<>();
                cleanerActionDelay = c.cleanerActionDelay;
                cleanerAckTimeout = c.cleanerAckTimeout;
                maxBlocks = c.maxBlocks; maxArrows = c.maxArrows;
                maxThrowables = c.maxThrowables; maxFoods = c.maxFoods;
                maxWaterBuckets = c.maxWaterBuckets; maxLavaBuckets = c.maxLavaBuckets;
                maxMilkBuckets = c.maxMilkBuckets;
                itemsBlacklist = c.itemsBlacklist; offHandItem = c.offHandItem;

                // Normalize missing values from older PeoClient config files so the
                // settings screen and module logic never dereference nulls.
                if (keybinds == null) keybinds = new LinkedHashMap<>();
                if (nukerMode == null) nukerMode = "Normal";
                if (nukerShape == null) nukerShape = "Cube";
                if (nukerSort == null) nukerSort = "Closest";
                if (nukerFilterIds == null) nukerFilterIds = "";
                if (nukerHighlightMode == null) nukerHighlightMode = "Opacity";
                if (nukerHighlightColor == null) nukerHighlightColor = "255,128,128";
                if (nukerRangeColor == null) nukerRangeColor = "255,0,0";
                if (fullbrightMethod == null) fullbrightMethod = "Gamma";
                if (itemsBlacklist == null) itemsBlacklist = "";
                if (offHandItem == null) offHandItem = "SHIELD";
                if (usernameOverride == null) usernameOverride = "";
                if (proxyList == null) proxyList = new ArrayList<>();
                if (savedAccounts == null) savedAccounts = new ArrayList<>();
                savedAccounts.removeIf(v -> v == null || v.isBlank());
                if (savedAccounts.size() > 20) savedAccounts = new ArrayList<>(savedAccounts.subList(0, 20));
                if (cleanerDropFilter == null) cleanerDropFilter = new LinkedHashSet<>();
                if (cleanerAckTimeout < 1) cleanerAckTimeout = 1;
                if (xrayBlocks == null || xrayBlocks.isEmpty()) xrayBlocks = defaultOreBlocks();
            } catch (Exception ignored) {
            }
        }

        private static Set<String> defaultOreBlocks() {
            return new LinkedHashSet<>(Arrays.asList(
                    "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
                    "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
                    "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
                    "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                    "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
                    "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
                    "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
                    "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                    "minecraft:nether_quartz_ore", "minecraft:nether_gold_ore",
                    "minecraft:ancient_debris"
            ));
        }

        public void save() {
            try {
                Path p = path();
                Files.createDirectories(p.getParent());
                Files.writeString(
                        p,
                        new GsonBuilder().setPrettyPrinting().create().toJson(this));
            } catch (IOException ignored) {
            }
        }
    }

    public static String getDisplayUsername() {
        class_310 mc = class_310.method_1551();
        String override = CFG.usernameOverride == null ? "" : CFG.usernameOverride.trim();
        if (!override.isEmpty()) return override;
        return originalUsername;
    }

    public static void setUsernameOverride(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.length() > 16) clean = clean.substring(0, 16);
        CFG.usernameOverride = clean;
        try {
            ((com.peoclient.mixin.SessionAccessor) (Object) class_310.method_1551().method_1548())
                    .peo$setUsername(clean.isEmpty() ? originalUsername : clean);
        } catch (Throwable ignored) {
        }
        CFG.save();
    }

    public static void applyProxySettings(class_310 mc) {
        if (!CFG.randomProxy || CFG.proxyList == null || CFG.proxyList.isEmpty()) return;
        java.net.Proxy proxy = java.net.Proxy.NO_PROXY;
        {
            String entry = CFG.proxyList.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(CFG.proxyList.size()));
            try {
                String value = entry.trim();
                boolean socks = value.toLowerCase(java.util.Locale.ROOT).startsWith("socks5://")
                        || value.toLowerCase(java.util.Locale.ROOT).startsWith("socks4://");
                int protoEnd = value.indexOf("://");
                if (protoEnd >= 0) value = value.substring(protoEnd + 3);
                int colon = value.lastIndexOf(':');
                if (colon > 0) {
                    String host = value.substring(0, colon);
                    int port = Integer.parseInt(value.substring(colon + 1));
                    proxy = new java.net.Proxy(java.net.Proxy.Type.SOCKS,
                            new java.net.InetSocketAddress(host, port));
                }
            } catch (Exception ignored) {
                proxy = java.net.Proxy.NO_PROXY;
            }
        }
        try {
            ((com.peoclient.mixin.MinecraftClientAccessor) (Object) mc).peo$setNetworkProxy(proxy);
        } catch (Throwable ignored) {
        }
    }

    public static final class FullbrightLogic {
        private static double originalGamma = 1.0;
        private static boolean captured;

        private FullbrightLogic() {}

        public static void tick(class_310 mc) {
            if (mc.field_1724 == null) return;

            boolean gammaActive = CFG.fullbright && "Gamma".equalsIgnoreCase(CFG.fullbrightMethod);
            boolean nightVisionActive = CFG.fullbright && "Night Vision".equalsIgnoreCase(CFG.fullbrightMethod);
            boolean xrayBrightness = CFG.xray;
            boolean brightnessActive = gammaActive || nightVisionActive || xrayBrightness;

            if (brightnessActive && !captured) {
                originalGamma = mc.field_1690.method_42473().method_41753();
                captured = true;
            }

            // Wurst's Gamma method forces brightness to 1600%, with optional 0.5-step fade.
            if (gammaActive || xrayBrightness) {
                double old = mc.field_1690.method_42473().method_41753();
                double next = CFG.fullbrightFade && Math.abs(old - 16.0) > 0.5
                        ? old + (old < 16.0 ? 0.5 : -0.5)
                        : 16.0;
                GammaUtil.forceSet(mc, next);
                if (mc.field_1724.method_6059(class_1294.field_5925))
                    mc.field_1724.method_6016(class_1294.field_5925);
                return;
            }

            // Keep the alternate method available, but do not touch inventory or server state.
            if (nightVisionActive) {
                mc.field_1724.method_6092(new class_1293(
                        class_1294.field_5925, 20, 0, false, false, false));
                if (captured) GammaUtil.forceSet(mc, originalGamma);
                return;
            }

            restore(mc);
        }

        public static void restore(class_310 mc) {
            if (mc.field_1724 != null && mc.field_1724.method_6059(class_1294.field_5925))
                mc.field_1724.method_6016(class_1294.field_5925);
            if (captured) {
                GammaUtil.forceSet(mc, class_3532.method_15350(originalGamma, 0.0, 16.0));
                captured = false;
            }
        }
    }

    public static final class GammaUtil {
        private GammaUtil() {}

        public static void forceSet(class_310 mc, double value) {
            try {
                ((com.peoclient.mixin.SimpleOptionAccessor) (Object) mc.field_1690.method_42473())
                        .peo$setValue(value);
            } catch (Throwable ignored) {
                // Fallback keeps vanilla-safe behaviour if the accessor is unavailable.
                mc.field_1690.method_42473().method_41748(class_3532.method_15350(value, 0.0, 1.0));
            }
        }
    }

    public static final class NukerLogic {
        private static final List<class_2338> renderBlocks = new ArrayList<>();
        private static final List<Target> queue = new ArrayList<>();
        private static int cooldown;
        private static class_2338 breakingPos;
        private static class_2350 breakingSide;
        private static int stagnantTicks;
        private static float lastBreakingProgress;
        private static class_2338 progressPos;
        private static long diagnosticTargetTime;
        private static long diagnosticAttemptTime;
        private static long diagnosticInteractionTime;
        // Added from the supplied Nuker refinement: prevent duplicate local
        // interactions and temporarily pause the local queue if it grows too large.
        private static final java.util.Set<class_2338> pendingBlocks = new java.util.HashSet<>();
        private static boolean isPaused;
        private static int pauseTicks;
        private static final int PAUSE_THRESHOLD = 200;
        private static final int PAUSE_DURATION_TICKS = 10;

        private NukerLogic() {}

        public static void tick(class_310 mc) {
            if (mc.field_1761 == null || mc.field_1724 == null || mc.field_1687 == null
                    || mc.field_1755 != null) return;

            renderBlocks.clear();
            // Restore the old Nuker "Multi" strength/queue setting while keeping
            // the ghost-block fix: Multi controls how many valid targets are kept
            // ready, but only ONE vanilla breaking state is active at a time.
            // The active target is then updated every tick like holding left-click.
            int batch = class_3532.method_15340(CFG.nukerMulti, 1, 10);
            int estimatedPending = queue.size();
            if (estimatedPending > PAUSE_THRESHOLD && !isPaused) {
                isPaused = true;
                pauseTicks = 0;
                com.peoclient.diagnostic.DiagnosticRecorder.get().record(
                        "NukerThrottle", "Paused due to high target queue: " + estimatedPending);
            }
            if (isPaused) {
                pauseTicks++;
                if (pauseTicks >= PAUSE_DURATION_TICKS) {
                    isPaused = false;
                    pauseTicks = 0;
                } else {
                    return;
                }
            }

            // Restore configurable cooldown from the previous Nuker behaviour.
            // It applies between completed targets and never interrupts an active
            // vanilla breaking state.
            if (cooldown > 0) {
                cooldown--;
                if (breakingPos != null) renderBlocks.add(breakingPos);
                return;
            }

            // -----------------------------------------------------------------
            // 1) Maintain the current vanilla breaking target.
            // -----------------------------------------------------------------
            if (breakingPos != null) {
                class_2680 activeState = mc.field_1687.method_8320(breakingPos);

                // A server/world update has already made the target air. Do not
                // force another renderer refresh; vanilla ClientWorld state and
                // WorldRenderer are authoritative for the visual result.
                if (activeState.method_26215()) {
                    long successNow = System.currentTimeMillis();
                    if (diagnosticAttemptTime > 0) {
                        com.peoclient.diagnostic.NukerTimingMetrics.get().recordAttemptToSuccess(
                                successNow - diagnosticAttemptTime);
                    }
                    com.peoclient.diagnostic.WorldStateMonitor.get().recordCheck(breakingPos);
                    com.peoclient.diagnostic.BreakEventRecorder.get().recordSuccess(
                            breakingPos, diagnosticAttemptTime > 0 ? successNow - diagnosticAttemptTime : 0);
                    com.peoclient.diagnostic.AccountSessionMetrics.get().recordBreakSuccess();
                    com.peoclient.diagnostic.NukerSessionRecorder.get().recordSuccess(
                            diagnosticAttemptTime > 0 ? successNow - diagnosticAttemptTime : 0);
                    com.peoclient.diagnostic.BreakStateTracker.get().transition(
                            com.peoclient.diagnostic.BreakStateTracker.State.SUCCESS, breakingPos);
                    com.peoclient.diagnostic.PreKickSnapshot.get().record("BREAK_SUCCESS: " + breakingPos);

                    queue.removeIf(t -> t.pos.equals(breakingPos));
                    pendingBlocks.remove(breakingPos);
                    if (CFG.nukerCooldown > 0) cooldown = CFG.nukerCooldown;
                    breakingPos = null;
                    breakingSide = null;
                    stagnantTicks = 0;
                    lastBreakingProgress = 0.0f;
                    progressPos = null;
                    diagnosticAttemptTime = 0L;
                    diagnosticInteractionTime = 0L;
                    if (cooldown > 0) return;
                } else {
                    // This is the key change: while the target remains active,
                    // continuously feed the normal Minecraft interaction manager
                    // just like vanilla does while the left button is held.
                    if (CFG.nukerRotate) rotateTo(mc, breakingPos);

                    if (com.peoclient.modules.AntiVipProMaxModule.isEnabled()) {
                        com.peoclient.nuker.bypass.NukerBypassEngine.onBreakProgress(
                                getBreakingProgress(), breakingPos);
                    }

                    boolean continueBreaking = mc.field_1761.method_2902(breakingPos,
                            breakingSide == null ? class_2350.field_11036 : breakingSide);
                    renderBlocks.add(breakingPos);

                    float progress = getBreakingProgress();
                    if (!breakingPos.equals(progressPos) || progress > lastBreakingProgress + 0.001f) {
                        stagnantTicks = 0;
                    } else {
                        stagnantTicks++;
                    }
                    progressPos = breakingPos;
                    lastBreakingProgress = progress;

                    // If the interaction manager reports that this breaking state
                    // has ended, let the next tick verify the actual world state.
                    // We do not locally set the block to air.
                    if (!continueBreaking) {
                        stagnantTicks = Math.max(stagnantTicks, 1);
                    }

                    // Recovery only cancels a genuinely stale vanilla state. It
                    // does not perform a renderer reload or create a replacement
                    // block state.
                    if (stagnantTicks >= 8) {
                        if (com.peoclient.modules.AntiVipProMaxModule.isEnabled()) {
                            com.peoclient.nuker.bypass.NukerBypassEngine.onRecovery();
                        }
                        com.peoclient.diagnostic.NukerSessionRecorder.get().recordRecovery();
                        com.peoclient.diagnostic.BreakEventRecorder.get().recordRecovery(
                                breakingPos, "Stale vanilla breaking state");
                        com.peoclient.diagnostic.AccountSessionMetrics.get().recordRecovery();
                        com.peoclient.diagnostic.BreakStateTracker.get().transition(
                                com.peoclient.diagnostic.BreakStateTracker.State.RECOVERY, breakingPos);
                        com.peoclient.diagnostic.PreKickSnapshot.get().record(
                                "RECOVERY: " + breakingPos);
                        mc.field_1761.method_2925();
                        pendingBlocks.remove(breakingPos);
                        breakingPos = null;
                        breakingSide = null;
                        stagnantTicks = 0;
                        lastBreakingProgress = 0.0f;
                        progressPos = null;
                    }

                    // Do not select another target in the same tick. Vanilla keeps
                    // one breaking target alive across ticks.
                    return;
                }
            }

            // AntiVipProMax observes the existing vanilla break state; it does
            // not create another packet producer here.
            if (com.peoclient.modules.AntiVipProMaxModule.isEnabled()) {
                com.peoclient.nuker.bypass.NukerBypassEngine.onNukerTick(false, false);
            }

            // -----------------------------------------------------------------
            // 2) Acquire one new target only when there is no active target.
            // -----------------------------------------------------------------
            if (queue.isEmpty()) {
                queue.addAll(collect(mc));
                queue.sort(comparator(mc));
            }

            // Restore the old Multi/SurvMulti queue depth. This preserves the
            // setting's "strength" as target throughput without creating multiple
            // simultaneous breaking states (which was the source of ghost blocks).
            if ("Multi".equalsIgnoreCase(CFG.nukerMode)
                    || "SurvMulti".equalsIgnoreCase(CFG.nukerMode)) {
                if (queue.size() > batch) queue.subList(batch, queue.size()).clear();
            } else if (queue.size() > 1) {
                queue.subList(1, queue.size()).clear();
            }

            // Remove invalid targets without starting them.
            while (!queue.isEmpty() && !isValidTarget(mc, queue.get(0))) {
                pendingBlocks.remove(queue.get(0).pos);
                queue.remove(0);
            }
            if (queue.isEmpty()) return;

            Target target = queue.get(0);
            if (pendingBlocks.contains(target.pos)) {
                queue.remove(0);
                return;
            }

            class_2680 state = mc.field_1687.method_8320(target.pos);
            float delta = state.method_26165(mc.field_1724, mc.field_1687, target.pos);
            if (delta <= 0) {
                queue.remove(0);
                return;
            }

            if (CFG.nukerRotate) rotateTo(mc, target.pos);

            diagnosticTargetTime = System.currentTimeMillis();
            if (com.peoclient.modules.AntiVipProMaxModule.isEnabled()) {
                com.peoclient.nuker.bypass.NukerBypassEngine.onTargetSelected(target.pos);
                com.peoclient.nuker.bypass.NukerBypassEngine.onBreakAttempt(target.pos, target.side);
            }
            com.peoclient.diagnostic.BreakStateTracker.get().transition(
                    com.peoclient.diagnostic.BreakStateTracker.State.TARGETING, target.pos);
            com.peoclient.diagnostic.TargetHistory.get().recordTarget(
                    target.pos,
                    mc.field_1724.method_33571().method_1022(class_243.method_24953(target.pos)),
                    0);
            com.peoclient.diagnostic.WorldStateMonitor.get().recordTarget(target.pos);
            com.peoclient.diagnostic.NukerSessionRecorder.get().recordTarget(target.pos);

            // Start the same interaction vanilla starts when the left mouse button
            // is first pressed. Crucially, do NOT immediately cancel the target
            // and do NOT force a local air state.
            if (!mc.field_1761.method_2910(target.pos, target.side)) {
                if (com.peoclient.modules.AntiVipProMaxModule.isEnabled()) {
                    com.peoclient.nuker.bypass.NukerBypassEngine.onBreakFailure(
                            target.pos, com.peoclient.diagnostic.BreakFailureReason.INTERACTION_FAIL);
                }
                com.peoclient.diagnostic.BreakEventRecorder.get().recordFailure(
                        target.pos, com.peoclient.diagnostic.BreakFailureReason.INTERACTION_FAIL.name());
                com.peoclient.diagnostic.AccountSessionMetrics.get().recordBreakFailure();
                com.peoclient.diagnostic.NukerSessionRecorder.get().recordFailure();
                com.peoclient.diagnostic.BreakStateTracker.get().transition(
                        com.peoclient.diagnostic.BreakStateTracker.State.FAILURE, target.pos);
                com.peoclient.diagnostic.PreKickSnapshot.get().record(
                        "BREAK_FAILURE: " + target.pos);
                queue.remove(0);
                return;
            }

            diagnosticAttemptTime = System.currentTimeMillis();
            diagnosticInteractionTime = diagnosticAttemptTime;
            if (diagnosticTargetTime > 0) {
                com.peoclient.diagnostic.NukerTimingMetrics.get().recordTargetToAttempt(
                        diagnosticAttemptTime - diagnosticTargetTime);
            }
            com.peoclient.diagnostic.BreakEventRecorder.get().recordStart(target.pos, CFG.nukerRange);
            com.peoclient.diagnostic.AccountSessionMetrics.get().recordBreakAttempt();
            com.peoclient.diagnostic.NukerSessionRecorder.get().recordAttempt();
            com.peoclient.diagnostic.BreakStateTracker.get().transition(
                    com.peoclient.diagnostic.BreakStateTracker.State.BREAKING, target.pos);
            com.peoclient.diagnostic.PreKickSnapshot.get().record(
                    "BREAK_ATTEMPT: " + target.pos);

            breakingPos = target.pos.method_10062();
            breakingSide = target.side;
            pendingBlocks.add(target.pos);
            renderBlocks.add(target.pos);

            // Do one progress update on the start tick, then the branch above will
            // continue it on every subsequent tick until the world actually changes.
            mc.field_1761.method_2902(breakingPos, breakingSide);
            mc.field_1724.method_6104(class_1268.field_5808);
        }

        public static void resetState() {
            class_310 mc = class_310.method_1551();
            if (mc.field_1761 != null && breakingPos != null) {
                mc.field_1761.method_2925();
            }
            queue.clear();
            pendingBlocks.clear();
            isPaused = false;
            pauseTicks = 0;
            renderBlocks.clear();
            breakingPos = null;
            breakingSide = null;
            stagnantTicks = 0;
            lastBreakingProgress = 0.0f;
            progressPos = null;
        }

        public static List<class_2338> getRenderBlocks() {
            return new ArrayList<>(renderBlocks);
        }

        public static float getBreakingProgress() {
            class_310 mc = class_310.method_1551();
            if (mc.field_1761 == null) return 0.0f;
            return class_3532.method_15363(mc.field_1761.method_51888() / 10.0f, 0.0f, 1.0f);
        }

        private static List<Target> collect(class_310 mc) {
            double range = class_3532.method_15350(CFG.nukerRange, 0.0, 15.0);
            int r = class_3532.method_15384(range);
            class_2338 center = class_2338.method_49638(mc.field_1724.method_33571());
            List<Target> out = new ArrayList<>();

            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        class_2338 pos = center.method_10069(x, y, z);
                        if (!NukerAreaLimiter.contains(pos)) continue;
                        if (CFG.nukerFlatten && pos.method_10264() < mc.field_1724.method_31478() - 1) continue;

                        double distance = "Cube".equalsIgnoreCase(CFG.nukerShape)
                                ? Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z))
                                : mc.field_1724.method_33571().method_1022(class_243.method_24953(pos));
                        if (distance > range + 0.25) continue;

                        class_2680 state = mc.field_1687.method_8320(pos);
                        if (state.method_26215() || state.method_26204() instanceof class_2404) continue;
                        if (CFG.nukerFilter && !passesFilter(state.method_26204())) continue;

                        class_2350 side = bestSide(mc, pos);
                        if (CFG.nukerRaycast && side == null) continue;
                        if (side == null) side = class_2350.field_11036;
                        out.add(new Target(pos, side));
                    }
                }
            }
            return out;
        }

        private static boolean isValidTarget(class_310 mc, Target target) {
            if (target == null || target.pos == null || mc.field_1724 == null || mc.field_1687 == null) {
                return false;
            }

            class_2338 pos = target.pos;
            double range = class_3532.method_15350(CFG.nukerRange, 0.0, 15.0);
            class_243 eye = mc.field_1724.method_33571();
            class_2338 center = class_2338.method_49638(mc.field_1724.method_33571());
            double distance = "Cube".equalsIgnoreCase(CFG.nukerShape)
                    ? Math.max(
                        Math.max(Math.abs(pos.method_10263() - center.method_10263()),
                                 Math.abs(pos.method_10264() - center.method_10264())),
                        Math.abs(pos.method_10260() - center.method_10260()))
                    : eye.method_1022(class_243.method_24953(pos));

            if (distance > range + 0.25) return false;
            if (!NukerAreaLimiter.contains(pos)) return false;
            if (CFG.nukerFlatten && pos.method_10264() < mc.field_1724.method_31478() - 1) return false;

            class_2680 state = mc.field_1687.method_8320(pos);
            if (state.method_26215() || state.method_26204() instanceof class_2404) return false;
            if (CFG.nukerFilter && !passesFilter(state.method_26204())) return false;

            if (CFG.nukerRaycast) {
                class_2350 side = bestSide(mc, pos);
                if (side == null) return false;
            }

            return state.method_26165(mc.field_1724, mc.field_1687, pos) > 0.0f;
        }

        private static boolean passesFilter(class_2248 block) {
            Set<String> filter = new LinkedHashSet<>();
            String raw = CFG.nukerFilterIds == null ? "" : CFG.nukerFilterIds;
            for (String s : raw.split("[,\\n\\s]+")) {
                if (!s.isBlank()) {
                    String normalized = s.trim().toLowerCase(Locale.ROOT);
                    if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
                    filter.add(normalized);
                }
            }

            String blockId = class_7923.field_41175.method_10221(block).toString().toLowerCase(Locale.ROOT);
            if (filter.isEmpty()) return !CFG.nukerWhitelist;
            boolean contains = filter.contains(blockId);
            return CFG.nukerWhitelist ? contains : !contains;
        }

        private static Comparator<Target> comparator(class_310 mc) {
            Comparator<Target> keepUnder = Comparator.comparing(
                    t -> t.pos.equals(class_2338.method_49638(mc.field_1724.method_19538()).method_10074()));
            Comparator<Target> distance = Comparator.comparingDouble(
                    t -> mc.field_1724.method_33571().method_1022(class_243.method_24953(t.pos)));
            Comparator<Target> hardness = Comparator.comparingDouble(
                    t -> mc.field_1687.method_8320(t.pos).method_26214(mc.field_1687, t.pos));
            Comparator<Target> result = switch (CFG.nukerSort) {
                case "Furthest" -> distance.reversed();
                case "Softest" -> hardness;
                case "Hardest" -> hardness.reversed();
                default -> distance;
            };
            return keepUnder.thenComparing(result);
        }

        private static class_2350 bestSide(class_310 mc, class_2338 pos) {
            // BleachHack-style face selection: try every face and raycast to a point
            // on that face instead of raycasting only to the block center. This is
            // especially important when mining from above/below or around corners.
            class_243 eye = mc.field_1724.method_33571();
            for (class_2350 side : class_2350.values()) {
                class_2338 neighbour = pos.method_10093(side);
                if (!mc.field_1687.method_8320(neighbour).method_26234(mc.field_1687, neighbour)) {
                    class_243 face = class_243.method_24953(pos).method_1031(
                            side.method_10148() * 0.49,
                            side.method_10164() * 0.49,
                            side.method_10165() * 0.49);
                    try {
                        class_3965 hit = mc.field_1687.method_17742(new net.minecraft.class_3959(
                                eye, face,
                                net.minecraft.class_3959.class_3960.field_17559,
                                net.minecraft.class_3959.class_242.field_1348,
                                mc.field_1724));
                        if (hit.method_17783() == class_239.class_240.field_1332 && hit.method_17777().equals(pos)) {
                            return side;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (CFG.nukerRaycast) return null;
            class_243 toPlayer = eye.method_1020(class_243.method_24953(pos));
            return class_2350.method_10142(toPlayer.field_1352, toPlayer.field_1351, toPlayer.field_1350);
        }

        private static boolean rotateTo(class_310 mc, class_2338 pos) {
            class_243 v = class_243.method_24953(pos).method_1020(mc.field_1724.method_33571());
            double horizontal = Math.sqrt(v.field_1352 * v.field_1352 + v.field_1350 * v.field_1350);
            float yaw = (float) (Math.toDegrees(Math.atan2(v.field_1350, v.field_1352)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(v.field_1351, horizontal));
            float yawDelta = class_3532.method_15393(yaw - mc.field_1724.method_36454());
            float pitchDelta = pitch - mc.field_1724.method_36455();
            boolean changed = Math.abs(yawDelta) > 2.0f || Math.abs(pitchDelta) > 2.0f;
            mc.field_1724.method_36456(yaw);
            mc.field_1724.method_36457(class_3532.method_15363(pitch, -90, 90));
            return changed;
        }

        private record Target(class_2338 pos, class_2350 side) {}
    }

    public static final class Hud {
        private Hud() {}

        public static void render(net.minecraft.class_332 d) {
            class_310 mc = class_310.method_1551();
            if (mc.field_1724 == null) return;

            // Wurst-style compact HUD: logo at top-left, active modules directly underneath.
            var title = class_2561.method_43470("PeoClient 1.21.4 V1")
                    .method_27694(style -> style.method_10982(true));
            d.method_51439(mc.field_1772, title, 10, 8, 0xFFFFFFFF, false);

            int y = 24;
            if (CFG.xray) y = active(d, mc, "X-Ray", y);
            if (CFG.fullbright) y = active(d, mc, "Fullbright", y);
            if (CFG.nuker) y = active(d, mc, "Nuker [" + CFG.nukerMode + "]", y);
            if (NukerCompatibility.isEnabled()) y = active(d, mc, "Nuker Compatibility", y);
            if (NukerAreaLimiter.isLocked()) y = active(d, mc, CFG.nukerRangeHighlight ? "Nuker Area [VISIBLE]" : "Nuker Area [LOCKED]", y);
            if (CFG.cleaner) y = active(d, mc, "InventoryCleaner", y);
            if (AntiVipProMaxModule.isEnabled()) y = active(d, mc, "AntiVipProMax", y);
        }

        private static int active(net.minecraft.class_332 d, class_310 mc, String name, int y) {
            d.method_51439(mc.field_1772,
                    class_2561.method_43470(name).method_27694(style -> style.method_10982(true)),
                    10, y, 0xFFFFFFFF, false);
            return y + 14;
        }
    }}
