package com.peoclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.peoclient.inventory.InventoryCleaner;
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

    public static KeyBinding menuKey, xrayKey, nukerKey, fullbrightKey, cleanerKey;
    private int saveTick;

    @Override
    public void onInitializeClient() {
        CFG.load();

        menuKey = key("PeoClient Hub", GLFW.GLFW_KEY_RIGHT_SHIFT);
        xrayKey = key("X-Ray", GLFW.GLFW_KEY_X);
        nukerKey = key("Nuker", GLFW.GLFW_KEY_N);
        fullbrightKey = key("Fullbright", GLFW.GLFW_KEY_F);
        cleanerKey = key("InventoryCleaner", GLFW.GLFW_KEY_I);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((draw, delta) -> Hud.render(draw));
    }

    private KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(name, InputUtil.Type.KEYSYM, code, "PeoClient"));
    }

    private void tick(MinecraftClient mc) {
        while (menuKey.wasPressed()) mc.setScreen(new PoeScreen());

        while (xrayKey.wasPressed()) toggleXray(mc);
        while (nukerKey.wasPressed()) CFG.nuker = !CFG.nuker;
        while (fullbrightKey.wasPressed()) toggleFullbright(mc);
        while (cleanerKey.wasPressed()) CFG.cleaner = !CFG.cleaner;

        if (mc.player == null || mc.world == null) {
            if (++saveTick >= 100) {
                saveTick = 0;
                CFG.save();
            }
            return;
        }

        FullbrightLogic.tick(mc);

        if (CFG.nuker) NukerLogic.tick(mc);
        if (CFG.cleaner) InventoryCleaner.tick(mc);

        if (++saveTick >= 100) {
            saveTick = 0;
            CFG.save();
        }
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
        public boolean nukerRangeHighlight = false;

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
                nukerRangeHighlight = c.nukerRangeHighlight;

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
        private static int cooldown;

        private NukerLogic() {}

        public static void tick(MinecraftClient mc) {
            if (mc.interactionManager == null || mc.player == null || mc.world == null
                    || mc.currentScreen != null) return;

            if (cooldown > 0) {
                cooldown--;
                return;
            }

            List<Target> targets = collect(mc);
            if (targets.isEmpty()) return;

            targets.sort(comparator(mc));
            renderBlocks.clear();

            int mode = mode();
            int limit = switch (mode) {
                case 2 -> Math.max(1, CFG.nukerMulti); // Multi
                case 1 -> Math.max(1, CFG.nukerMulti); // SurvMulti
                default -> 1;
            };

            int broken = 0;
            for (Target target : targets) {
                BlockState state = mc.world.getBlockState(target.pos);
                float delta = state.calcBlockBreakingDelta(mc.player, mc.world, target.pos);

                if (delta <= 0) continue;

                // Survival-safe serial progress: never pretend a server accepted an
                // impossible instant break.
                if (mode == 1 && delta < 1.0f && broken > 0) break;

                if (CFG.nukerRotate) rotateTo(mc, target.pos);

                mc.interactionManager.updateBlockBreakingProgress(target.pos, target.side);
                mc.player.swingHand(Hand.MAIN_HAND);
                renderBlocks.add(target.pos);
                broken++;

                if (mode == 0 || mode == 3 || broken >= limit) break;
            }

            cooldown = Math.max(0, CFG.nukerCooldown);
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

                        if (CFG.nukerFilter && filtered(state.getBlock())) continue;

                        Direction side = bestSide(mc, pos);
                        if (CFG.nukerRaycast && side == null) continue;
                        if (side == null) side = Direction.UP;

                        out.add(new Target(pos, side));
                    }
                }
            }
            return out;
        }

        private static boolean filtered(Block block) {
            String id = Registries.BLOCK.getId(block).toString();
            Set<String> filter = new LinkedHashSet<>();
            for (String s : CFG.nukerFilterIds.split("[,\\n\\s]+")) {
                if (!s.isBlank()) filter.add(s.trim());
            }
            boolean contains = filter.contains(id);
            return CFG.nukerWhitelist ? !contains : contains;
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
            for (Direction d : Direction.values()) {
                BlockPos neighbour = pos.offset(d);
                if (!mc.world.getBlockState(neighbour).isFullCube(mc.world, neighbour)) {
                    Vec3d center = Vec3d.ofCenter(pos);
                    Vec3d eye = mc.player.getEyePos();
                    Vec3d delta = center.subtract(eye);
                    if (delta.lengthSquared() <= (CFG.nukerRange + 1.0) * (CFG.nukerRange + 1.0))
                        return d;
                }
            }
            return null;
        }

        private static int mode() {
            return switch (CFG.nukerMode) {
                case "SurvMulti" -> 1;
                case "Multi" -> 2;
                case "Instant" -> 3;
                default -> 0;
            };
        }

        private static void rotateTo(MinecraftClient mc, BlockPos pos) {
            Vec3d v = Vec3d.ofCenter(pos).subtract(mc.player.getEyePos());
            double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
            float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(v.y, horizontal));
            mc.player.setYaw(yaw);
            mc.player.setPitch(MathHelper.clamp(pitch, -90, 90));
        }

        private record Target(BlockPos pos, Direction side) {}
    }

    public static final class Hud {
        private Hud() {}

        public static void render(net.minecraft.client.gui.DrawContext d) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            // PeoClient HUD: fixed to the top-left, with the client version first
            // and only currently enabled modules listed underneath it.
            int left = 8;
            String title = "PeoClient 1.21.4 V1";
            int titleW = mc.textRenderer.getWidth(title) + 10;
            d.fill(left - 4, 4, left + titleW, 20, 0x78070B10);
            d.drawTextWithShadow(mc.textRenderer, title, left, 8, 0xFFFFFFFF);

            List<String> active = new ArrayList<>();
            if (CFG.xray) active.add("X-Ray");
            if (CFG.fullbright) active.add("Fullbright");
            if (CFG.nuker) active.add("Nuker [" + CFG.nukerMode + "]");
            if (CFG.cleaner) active.add("InventoryCleaner");

            // Wurst-like readable top-left module list: strong shadow, dark backing,
            // and enough vertical spacing that enabled modules never overlap.
            int y = 23;
            for (String name : active) {
                int color = switch (name.startsWith("X-Ray") ? "X-Ray" : name.startsWith("Fullbright") ? "Fullbright" : name.startsWith("Nuker") ? "Nuker" : "InventoryCleaner") {
                    case "X-Ray" -> 0xFF43D8FF;
                    case "Fullbright" -> 0xFF71FF78;
                    case "Nuker" -> 0xFFFFB21C;
                    default -> 0xFFFF63E8;
                };
                int w = mc.textRenderer.getWidth(name) + 8;
                d.fill(left - 4, y - 2, left + w, y + 11, 0x66070B10);
                d.drawTextWithShadow(mc.textRenderer, name, left, y, color);
                y += 15;
            }
        }
    }
}
