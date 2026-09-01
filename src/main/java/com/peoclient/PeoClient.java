package com.peoclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.peoclient.inventory.InventoryCleaner;
import com.peoclient.nuker.compat.NukerCompatibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PeoClient implements ClientModInitializer {
    public static final Config CFG = new Config();

    public static KeyBinding menuKey, xrayKey, nukerKey, fullbrightKey, cleanerKey, nukerBypassKey;
    public static final Map<String, KeyBinding> MODULE_KEYS = new LinkedHashMap<>();
    private static String originalUsername = "Player";
    private int saveTick;
    private boolean networkConfigured;

    @Override
    public void onInitializeClient() {
        originalUsername = MinecraftClient.getInstance().getSession().getUsername();
        CFG.load();

        menuKey = key("PeoClient Hub", GLFW.GLFW_KEY_RIGHT_SHIFT);
        registerModuleKeys();
        xrayKey = MODULE_KEYS.get("X-Ray");
        nukerKey = MODULE_KEYS.get("Nuker [Multi]");
        fullbrightKey = MODULE_KEYS.get("Fullbright");
        cleanerKey = MODULE_KEYS.get("InventoryCleaner");
        nukerBypassKey = key("PeoClient Nuker Compatibility", GLFW.GLFW_KEY_B);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((draw, delta) -> Hud.render(draw));
    }

    private KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(name, InputUtil.Type.KEYSYM, code, "PeoClient"));
    }

    private void registerModuleKeys() {
        for (String module : PoeScreen.MODULES) {
            int defaultCode = switch (module) {
                case "Fullbright" -> GLFW.GLFW_KEY_F;
                case "InventoryCleaner" -> GLFW.GLFW_KEY_I;
                case "Nuker [Multi]" -> GLFW.GLFW_KEY_N;
                case "X-Ray" -> GLFW.GLFW_KEY_X;
                default -> GLFW.GLFW_KEY_UNKNOWN;
            };
            Integer stored = CFG.keybinds.get(module);
            int code = stored != null ? stored : defaultCode;
            MODULE_KEYS.put(module, key("PeoClient " + module, code));
        }
    }

    public static boolean setModuleKey(String module, int keyCode) {
        KeyBinding key = MODULE_KEYS.get(module);
        if (key == null) return false;
        key.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(keyCode));
        CFG.keybinds.put(module, keyCode);
        MinecraftClient.getInstance().options.write();
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
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }

    private void tick(MinecraftClient mc) {
        if (!networkConfigured) {
            networkConfigured = true;
            if (CFG.usernameOverride != null && !CFG.usernameOverride.isBlank()) setUsernameOverride(CFG.usernameOverride);
            applyProxySettings(mc);
        }

        while (menuKey.wasPressed()) mc.setScreen(new PoeScreen());

        for (Map.Entry<String, KeyBinding> entry : MODULE_KEYS.entrySet()) {
            while (entry.getValue().wasPressed()) toggleModuleByName(entry.getKey(), mc);
        }
        while (nukerBypassKey.wasPressed()) NukerCompatibility.toggle();

        if (mc.player == null || mc.world == null) {
            if (++saveTick >= 100) {
                saveTick = 0;
                CFG.save();
            }
            return;
        }

        FullbrightLogic.tick(mc);

        if (CFG.nuker) NukerCompatibility.tick(mc);
        if (CFG.cleaner) InventoryCleaner.tick(mc);

        if (++saveTick >= 100) {
            saveTick = 0;
            CFG.save();
        }
    }

    public static void toggleModuleByName(String module, MinecraftClient mc) {
        switch (module) {
            case "X-Ray" -> toggleXray(mc);
            case "Nuker [Multi]" -> CFG.nuker = !CFG.nuker;
            case "Fullbright" -> toggleFullbright(mc);
            case "InventoryCleaner" -> CFG.cleaner = !CFG.cleaner;
            default -> { /* reserved for modules that are not implemented yet */ }
        }
        CFG.save();
    }

    public static void toggleXray(MinecraftClient mc) {
        CFG.xray = !CFG.xray;
        if (mc.world != null) reload(mc);
    }

    public static void toggleFullbright(MinecraftClient mc) {
        CFG.fullbright = !CFG.fullbright;
        if (!CFG.fullbright) FullbrightLogic.restore(mc);
        reload(mc);
    }

    public static void reload(MinecraftClient mc) {
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    public static boolean isXrayBlock(Block block) {
        return CFG.xrayBlocks.contains(Registries.BLOCK.getId(block).toString());
    }

    public static final class Config {
        public boolean xray = false, nuker = false, fullbright = false, cleaner = false;
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
        public boolean xrayExposedOnly = false;
        public boolean xrayFluids = true;
        public int xrayBackgroundOpacity = 0;

        public Set<String> xrayBlocks = new LinkedHashSet<>(Arrays.asList(
                "minecraft:amethyst_cluster", "minecraft:ancient_debris", "minecraft:anvil",
                "minecraft:beacon", "minecraft:bone_block", "minecraft:bookshelf",
                "minecraft:brewing_stand", "minecraft:budding_amethyst", "minecraft:chain_command_block",
                "minecraft:chest", "minecraft:coal_block", "minecraft:coal_ore", "minecraft:command_block",
                "minecraft:copper_ore", "minecraft:crafter", "minecraft:crafting_table",
                "minecraft:creaking_heart", "minecraft:decorated_pot", "minecraft:deepslate_coal_ore",
                "minecraft:deepslate_copper_ore", "minecraft:deepslate_diamond_ore",
                "minecraft:deepslate_emerald_ore", "minecraft:deepslate_gold_ore",
                "minecraft:deepslate_iron_ore", "minecraft:deepslate_lapis_ore",
                "minecraft:deepslate_redstone_ore", "minecraft:diamond_block", "minecraft:diamond_ore",
                "minecraft:dispenser", "minecraft:dropper", "minecraft:emerald_block",
                "minecraft:emerald_ore", "minecraft:enchanting_table", "minecraft:end_portal",
                "minecraft:end_portal_frame", "minecraft:ender_chest", "minecraft:furnace",
                "minecraft:glowstone", "minecraft:gold_block", "minecraft:gold_ore", "minecraft:hopper",
                "minecraft:iron_block", "minecraft:iron_ore", "minecraft:ladder", "minecraft:lapis_block",
                "minecraft:lapis_ore", "minecraft:lava", "minecraft:lodestone",
                "minecraft:mossy_cobblestone", "minecraft:nether_gold_ore", "minecraft:nether_portal",
                "minecraft:nether_quartz_ore", "minecraft:raw_copper_block", "minecraft:raw_gold_block",
                "minecraft:raw_iron_block", "minecraft:redstone_block", "minecraft:redstone_ore",
                "minecraft:repeating_command_block", "minecraft:sculk_catalyst", "minecraft:sculk_sensor",
                "minecraft:sculk_shrieker", "minecraft:spawner", "minecraft:suspicious_gravel",
                "minecraft:suspicious_sand", "minecraft:tnt", "minecraft:torch",
                "minecraft:trapped_chest", "minecraft:trial_spawner", "minecraft:vault",
                "minecraft:wall_torch", "minecraft:water"
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
        public int cleanerActionDelay = 0;
        public int cleanerAckTimeout = 30;
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
            return MinecraftClient.getInstance().runDirectory.toPath()
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
                cleanerBlacklistSet = c.cleanerBlacklistSet != null
                        ? c.cleanerBlacklistSet : cleanerBlacklistSet;
                slotItems = c.slotItems != null ? c.slotItems : slotItems;

                xray = c.xray; nuker = c.nuker; fullbright = c.fullbright; cleaner = c.cleaner;
                keybinds = c.keybinds != null ? new LinkedHashMap<>(c.keybinds) : new LinkedHashMap<>();
                usernameOverride = c.usernameOverride != null ? c.usernameOverride : "";
                randomProxy = c.randomProxy;
                proxyList = c.proxyList != null ? c.proxyList : proxyList;
                savedAccounts = c.savedAccounts != null ? c.savedAccounts : savedAccounts;
                xrayFullBright = c.xrayFullBright;
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
            } catch (Exception ignored) {
            }
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
        MinecraftClient mc = MinecraftClient.getInstance();
        String override = CFG.usernameOverride == null ? "" : CFG.usernameOverride.trim();
        if (!override.isEmpty()) return override;
        return originalUsername;
    }

    public static void setUsernameOverride(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.length() > 16) clean = clean.substring(0, 16);
        CFG.usernameOverride = clean;
        try {
            ((com.peoclient.mixin.SessionAccessor) (Object) MinecraftClient.getInstance().getSession())
                    .peo$setUsername(clean.isEmpty() ? originalUsername : clean);
        } catch (Throwable ignored) {
        }
        CFG.save();
    }

    public static void applyProxySettings(MinecraftClient mc) {
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

        public static void tick(MinecraftClient mc) {
            if (mc.player == null) return;

            boolean gammaActive = CFG.fullbright && "Gamma".equalsIgnoreCase(CFG.fullbrightMethod);
            boolean nightVisionActive = CFG.fullbright && "Night Vision".equalsIgnoreCase(CFG.fullbrightMethod);
            boolean xrayBrightness = CFG.xray;
            boolean brightnessActive = gammaActive || nightVisionActive || xrayBrightness;

            if (brightnessActive && !captured) {
                originalGamma = mc.options.getGamma().getValue();
                captured = true;
            }

            // Wurst's Gamma method forces brightness to 1600%, with optional 0.5-step fade.
            if (gammaActive || xrayBrightness) {
                double old = mc.options.getGamma().getValue();
                double next = CFG.fullbrightFade && Math.abs(old - 16.0) > 0.5
                        ? old + (old < 16.0 ? 0.5 : -0.5)
                        : 16.0;
                GammaUtil.forceSet(mc, next);
                if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION))
                    mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                return;
            }

            // Keep the alternate method available, but do not touch inventory or server state.
            if (nightVisionActive) {
                mc.player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NIGHT_VISION, 20, 0, false, false, false));
                if (captured) GammaUtil.forceSet(mc, originalGamma);
                return;
            }

            restore(mc);
        }

        public static void restore(MinecraftClient mc) {
            if (mc.player != null && mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION))
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            if (captured) {
                GammaUtil.forceSet(mc, MathHelper.clamp(originalGamma, 0.0, 16.0));
                captured = false;
            }
        }
    }

    public static final class GammaUtil {
        private GammaUtil() {}

        public static void forceSet(MinecraftClient mc, double value) {
            try {
                ((com.peoclient.mixin.SimpleOptionAccessor) (Object) mc.options.getGamma())
                        .peo$setValue(value);
            } catch (Throwable ignored) {
                // Fallback keeps vanilla-safe behaviour if the accessor is unavailable.
                mc.options.getGamma().setValue(MathHelper.clamp(value, 0.0, 1.0));
            }
        }
    }

    public static final class NukerLogic {
        private static final List<BlockPos> renderBlocks = new ArrayList<>();
        private static final List<Target> queue = new ArrayList<>();
        private static int cooldown;
        private static BlockPos breakingPos;
        private static Direction breakingSide;

        private NukerLogic() {}

        public static void tick(MinecraftClient mc) {
            if (mc.interactionManager == null || mc.player == null || mc.world == null
                    || mc.currentScreen != null) return;

            renderBlocks.clear();

            if (cooldown > 0) {
                cooldown--;
                if (breakingPos != null) renderBlocks.add(breakingPos);
                return;
            }

            // Build a fresh queue when the old one is exhausted or its active target became invalid.
            if (queue.isEmpty() || !activeTargetStillValid(mc)) {
                if (breakingPos != null) {
                    mc.interactionManager.cancelBlockBreaking();
                    breakingPos = null;
                    breakingSide = null;
                }
                queue.clear();
                queue.addAll(collect(mc));
                queue.sort(comparator(mc));
            }

            int batch = MathHelper.clamp(CFG.nukerMulti, 1, 10);
            if ("Multi".equalsIgnoreCase(CFG.nukerMode) || "SurvMulti".equalsIgnoreCase(CFG.nukerMode)) {
                // Multi/SurvMulti mean queue depth here; only one legitimate break state is active at a time.
                if (queue.size() > batch) queue.subList(batch, queue.size()).clear();
            } else if (!queue.isEmpty()) {
                queue.subList(1, queue.size()).clear();
            }

            while (!queue.isEmpty()) {
                Target target = queue.get(0);
                if (!isValidTarget(mc, target)) {
                    queue.remove(0);
                    continue;
                }

                BlockState state = mc.world.getBlockState(target.pos);
                float delta = state.calcBlockBreakingDelta(mc.player, mc.world, target.pos);
                if (delta <= 0) {
                    queue.remove(0);
                    continue;
                }

                if (CFG.nukerRotate && rotateTo(mc, target.pos)) {
                    cooldown = 1;
                    renderBlocks.add(target.pos);
                    return;
                }

                boolean newTarget = breakingPos == null || !breakingPos.equals(target.pos)
                        || breakingSide != target.side;
                if (newTarget) {
                    if (breakingPos != null) mc.interactionManager.cancelBlockBreaking();
                    if (!mc.interactionManager.attackBlock(target.pos, target.side)) {
                        queue.remove(0);
                        breakingPos = null;
                        breakingSide = null;
                        continue;
                    }
                    breakingPos = target.pos.toImmutable();
                    breakingSide = target.side;
                } else {
                    mc.interactionManager.updateBlockBreakingProgress(target.pos, target.side);
                }

                mc.player.swingHand(Hand.MAIN_HAND);
                renderBlocks.add(target.pos);

                if (mc.world.getBlockState(target.pos).isAir()) {
                    queue.remove(0);
                    breakingPos = null;
                    breakingSide = null;
                    // Advance immediately only after the world confirms the previous block is gone.
                    cooldown = Math.max(0, CFG.nukerCooldown);
                } else {
                    cooldown = Math.max(0, CFG.nukerCooldown);
                }
                return;
            }
        }

        private static boolean activeTargetStillValid(MinecraftClient mc) {
            if (breakingPos == null) return false;
            BlockState state = mc.world.getBlockState(breakingPos);
            return !state.isAir() && !(state.getBlock() instanceof FluidBlock)
                    && (!CFG.nukerFilter || passesFilter(state.getBlock()));
        }

        private static boolean isValidTarget(MinecraftClient mc, Target target) {
            BlockState state = mc.world.getBlockState(target.pos);
            if (state.isAir() || state.getBlock() instanceof FluidBlock) return false;
            if (CFG.nukerFlatten && target.pos.getY() < mc.player.getBlockY() - 1) return false;
            if (CFG.nukerFilter && !passesFilter(state.getBlock())) return false;
            if (CFG.nukerRaycast && target.side == null) return false;
            return mc.player.getEyePos().distanceTo(Vec3d.ofCenter(target.pos))
                    <= MathHelper.clamp(CFG.nukerRange, 1.0, 6.0) + 0.25;
        }

        private static List<Target> collect(MinecraftClient mc) {
            double range = MathHelper.clamp(CFG.nukerRange, 1.0, 6.0);
            int r = MathHelper.ceil(range);
            BlockPos center = BlockPos.ofFloored(mc.player.getEyePos());
            List<Target> out = new ArrayList<>();

            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        BlockPos pos = center.add(x, y, z);
                        if (CFG.nukerFlatten && pos.getY() < mc.player.getBlockY() - 1) continue;

                        double distance = "Cube".equalsIgnoreCase(CFG.nukerShape)
                                ? Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z))
                                : mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
                        if (distance > range + 0.25) continue;

                        BlockState state = mc.world.getBlockState(pos);
                        if (state.isAir() || state.getBlock() instanceof FluidBlock) continue;
                        if (CFG.nukerFilter && !passesFilter(state.getBlock())) continue;

                        Direction side = bestSide(mc, pos);
                        if (CFG.nukerRaycast && side == null) continue;
                        if (side == null) side = Direction.UP;
                        out.add(new Target(pos, side));
                    }
                }
            }
            return out;
        }

        private static boolean passesFilter(Block block) {
            Set<String> filter = new LinkedHashSet<>();
            String raw = CFG.nukerFilterIds == null ? "" : CFG.nukerFilterIds;
            for (String s : raw.split("[,\\n\\s]+")) {
                if (!s.isBlank()) {
                    String normalized = s.trim().toLowerCase(Locale.ROOT);
                    if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
                    filter.add(normalized);
                }
            }

            String blockId = Registries.BLOCK.getId(block).toString().toLowerCase(Locale.ROOT);
            if (filter.isEmpty()) return !CFG.nukerWhitelist;
            boolean contains = filter.contains(blockId);
            return CFG.nukerWhitelist ? contains : !contains;
        }

        private static Comparator<Target> comparator(MinecraftClient mc) {
            Comparator<Target> keepUnder = Comparator.comparing(
                    t -> t.pos.equals(BlockPos.ofFloored(mc.player.getPos()).down()));
            Comparator<Target> distance = Comparator.comparingDouble(
                    t -> mc.player.getEyePos().distanceTo(Vec3d.ofCenter(t.pos)));
            Comparator<Target> hardness = Comparator.comparingDouble(
                    t -> mc.world.getBlockState(t.pos).getHardness(mc.world, t.pos));
            Comparator<Target> result = switch (CFG.nukerSort) {
                case "Furthest" -> distance.reversed();
                case "Softest" -> hardness;
                case "Hardest" -> hardness.reversed();
                default -> distance;
            };
            return keepUnder.thenComparing(result);
        }

        private static Direction bestSide(MinecraftClient mc, BlockPos pos) {
            Vec3d eye = mc.player.getEyePos();
            Vec3d center = Vec3d.ofCenter(pos);
            try {
                BlockHitResult hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                        eye, center,
                        net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE,
                        mc.player));
                if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
                    return hit.getSide();
                }
            } catch (Throwable ignored) {
            }
            if (CFG.nukerRaycast) return null;
            Vec3d toPlayer = eye.subtract(center);
            return Direction.getFacing(toPlayer.x, toPlayer.y, toPlayer.z);
        }

        private static boolean rotateTo(MinecraftClient mc, BlockPos pos) {
            Vec3d v = Vec3d.ofCenter(pos).subtract(mc.player.getEyePos());
            double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
            float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(v.y, horizontal));
            float yawDelta = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
            float pitchDelta = pitch - mc.player.getPitch();
            boolean changed = Math.abs(yawDelta) > 2.0f || Math.abs(pitchDelta) > 2.0f;
            mc.player.setYaw(yaw);
            mc.player.setPitch(MathHelper.clamp(pitch, -90, 90));
            return changed;
        }

        private record Target(BlockPos pos, Direction side) {}
    }

    public static final class Hud {
        private Hud() {}

        public static void render(net.minecraft.client.gui.DrawContext d) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            // Wurst-style compact HUD: logo at top-left, active modules directly underneath.
            var title = Text.literal("PeoClient 1.21.4 V1")
                    .styled(style -> style.withBold(true));
            d.drawText(mc.textRenderer, title, 10, 8, 0xFFFFFFFF, false);

            int y = 24;
            if (CFG.xray) y = active(d, mc, "X-Ray", y);
            if (CFG.fullbright) y = active(d, mc, "Fullbright", y);
            if (CFG.nuker) y = active(d, mc, "Nuker [" + CFG.nukerMode + "]", y);
            if (CFG.cleaner) y = active(d, mc, "InventoryCleaner", y);
        }

        private static int active(net.minecraft.client.gui.DrawContext d, MinecraftClient mc, String name, int y) {
            d.drawText(mc.textRenderer,
                    Text.literal(name).styled(style -> style.withBold(true)),
                    10, y, 0xFFFFFFFF, false);
            return y + 14;
        }
    }}
