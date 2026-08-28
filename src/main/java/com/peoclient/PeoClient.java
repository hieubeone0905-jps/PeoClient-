package com.peoclient;

import com.google.gson.*;
import com.peoclient.inventory.InventoryCleaner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PeoClient implements ClientModInitializer {
    public static final Config CFG = new Config();
    public static KeyBinding menuKey, xrayKey, nukerKey, fullbrightKey, cleanerKey;
    private int tick;

    @Override public void onInitializeClient() {
        CFG.load();
        menuKey = key("PeoClient Hub", GLFW.GLFW_KEY_RIGHT_SHIFT);
        xrayKey = key("Xray", GLFW.GLFW_KEY_X);
        nukerKey = key("Nuker", GLFW.GLFW_KEY_N);
        fullbrightKey = key("Fullbright", GLFW.GLFW_KEY_F);
        cleanerKey = key("Inventory Cleaner", GLFW.GLFW_KEY_I);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((draw, delta) -> Hud.render(draw));
    }

    private KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(name, InputUtil.Type.KEYSYM, code, "PeoClient"));
    }

    private void tick(MinecraftClient mc) {
        while (menuKey.wasPressed()) mc.setScreen(new PoeScreen());
        while (xrayKey.wasPressed()) { CFG.xray = !CFG.xray; reload(mc); }
        while (nukerKey.wasPressed()) CFG.nuker = !CFG.nuker;
        while (fullbrightKey.wasPressed()) toggleFullbright(mc);
        while (cleanerKey.wasPressed()) CFG.cleaner = !CFG.cleaner;

        if (mc.player == null || mc.world == null) return;

        if (CFG.fullbright) {
            // Night vision is a reliable client-side fallback on modern versions.
            mc.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 260, 0, false, false, false));
        }

        if (CFG.nuker) NukerLogic.tick(mc);
        if (CFG.cleaner) InventoryCleaner.tick(mc);

        if (++tick % 40 == 0) CFG.save();
    }

    private void toggleFullbright(MinecraftClient mc) {
        CFG.fullbright = !CFG.fullbright;
        if (!CFG.fullbright && mc.player != null)
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        reload(mc);
    }

    public static void reload(MinecraftClient mc) {
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    public static final class Config {
        public boolean xray=false, nuker=false, fullbright=false, cleaner=false;

        // Wurst-style Xray: only these blocks are rendered while Xray is on.
        public boolean xrayFluids=false, xrayHideSurface=true;
        public int xrayRange=0; // 0 = normal loaded render distance
        public Set<String> xrayBlocks = new LinkedHashSet<>(Arrays.asList(
                "minecraft:coal_ore","minecraft:copper_ore","minecraft:iron_ore",
                "minecraft:gold_ore","minecraft:lapis_ore","minecraft:redstone_ore",
                "minecraft:diamond_ore","minecraft:emerald_ore",
                "minecraft:deepslate_coal_ore","minecraft:deepslate_copper_ore",
                "minecraft:deepslate_iron_ore","minecraft:deepslate_gold_ore",
                "minecraft:deepslate_lapis_ore","minecraft:deepslate_redstone_ore",
                "minecraft:deepslate_diamond_ore","minecraft:deepslate_emerald_ore",
                "minecraft:ancient_debris","minecraft:nether_gold_ore",
                "minecraft:spawner","minecraft:chest","minecraft:trapped_chest",
                "minecraft:ender_chest","minecraft:shulker_box"));

        // Survival-safe nuker: uses normal server-validated block breaking.
        public boolean nukerFilter=false, nukerWhitelist=false, nukerRaycast=true;
        public boolean nukerFlatten=false, nukerRotate=true;
        public int nukerRange=4, nukerBlocksPerTick=2, nukerDelay=0;
        public String nukerFilterIds="";
        public boolean nukerOnlyWhenHoldingTool=false;

        // Fullbright.
        public boolean fullbrightNightVision=true;

        // Cleaner: conservative acknowledgement-friendly settings.
        public boolean cleanerGreedy=true, cleanerMergeStacks=true, cleanerTouchHotbar=false;
        public int cleanerActionDelay=4, cleanerAckTimeout=12;
        public int maxBlocks=512,maxArrows=128,maxThrowables=64,maxFoods=200;
        public int maxWaterBuckets=2,maxLavaBuckets=2,maxMilkBuckets=2,maxPotions=16,maxPearls=16;
        public Set<String> cleanerBlacklistSet=new LinkedHashSet<>();
        public String itemsBlacklist="";
        public String offHandItem="SHIELD";
        public String[] slotItems={"WEAPON","BOW","PICKAXE","AXE","NONE","POTION","FOOD","BLOCK","BLOCK"};

        private Path path() {
            return MinecraftClient.getInstance().runDirectory.toPath().resolve("config/peoclient.json");
        }
        public void load() {
            try {
                Path p=path();
                if (!Files.exists(p)) { save(); return; }
                Config c=new Gson().fromJson(Files.readString(p), Config.class);
                if(c==null) return;
                if(c.xrayBlocks!=null)xrayBlocks=c.xrayBlocks;
                if(c.cleanerBlacklistSet!=null)cleanerBlacklistSet=c.cleanerBlacklistSet;
                // Gson leaves fields absent in older configs at their Java defaults.
                xray=c.xray;nuker=c.nuker;fullbright=c.fullbright;cleaner=c.cleaner;
                xrayFluids=c.xrayFluids;xrayHideSurface=c.xrayHideSurface;
                nukerFilter=c.nukerFilter;nukerWhitelist=c.nukerWhitelist;nukerRaycast=c.nukerRaycast;
                nukerFlatten=c.nukerFlatten;nukerRotate=c.nukerRotate;
                nukerRange=c.nukerRange;nukerBlocksPerTick=c.nukerBlocksPerTick;nukerDelay=c.nukerDelay;
                nukerFilterIds=c.nukerFilterIds;nukerOnlyWhenHoldingTool=c.nukerOnlyWhenHoldingTool;
                fullbrightNightVision=c.fullbrightNightVision;
                cleanerGreedy=c.cleanerGreedy;cleanerMergeStacks=c.cleanerMergeStacks;
                cleanerTouchHotbar=c.cleanerTouchHotbar;cleanerActionDelay=c.cleanerActionDelay;
                cleanerAckTimeout=c.cleanerAckTimeout;maxBlocks=c.maxBlocks;maxArrows=c.maxArrows;
                maxThrowables=c.maxThrowables;maxFoods=c.maxFoods;maxWaterBuckets=c.maxWaterBuckets;
                maxLavaBuckets=c.maxLavaBuckets;maxMilkBuckets=c.maxMilkBuckets;maxPotions=c.maxPotions;
                maxPearls=c.maxPearls;itemsBlacklist=c.itemsBlacklist;offHandItem=c.offHandItem;
                if(c.slotItems!=null)slotItems=c.slotItems;
            } catch(Exception ignored) {}
        }
        public void save() {
            try {
                Path p=path(); Files.createDirectories(p.getParent());
                Files.writeString(p,new GsonBuilder().setPrettyPrinting().create().toJson(this));
            } catch(Exception ignored) {}
        }
    }

    static final class NukerLogic {
        private static int cooldown;
        private static BlockPos breaking;

        static void tick(MinecraftClient mc) {
            if(mc.interactionManager==null||mc.player==null||mc.world==null||mc.currentScreen!=null)return;
            if(cooldown>0){cooldown--;return;}

            // Survival breaking is intentionally serialized: Minecraft's server only
            // accepts a legitimate break-progress stream for one block at a time.
            if(breaking!=null) {
                if(mc.world.getBlockState(breaking).isAir()) {
                    breaking=null;
                } else {
                    Direction side=sideFor(breaking,mc.player.getEyePos());
                    mc.interactionManager.updateBlockBreakingProgress(breaking,side);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    cooldown=Math.max(0,CFG.nukerDelay);
                    return;
                }
            }

            BlockPos target=findTarget(mc);
            if(target==null)return;
            breaking=target;
            Direction side=sideFor(target,mc.player.getEyePos());
            if(CFG.nukerRotate) rotateTo(mc,target);
            mc.interactionManager.attackBlock(target,side);
            mc.interactionManager.updateBlockBreakingProgress(target,side);
            mc.player.swingHand(Hand.MAIN_HAND);
            cooldown=Math.max(0,CFG.nukerDelay);
        }

        private static BlockPos findTarget(MinecraftClient mc) {
            double r=Math.max(1,Math.min(6,CFG.nukerRange));
            BlockPos origin=mc.player.getBlockPos();
            int rr=(int)Math.ceil(r);
            BlockPos best=null; double bestD=Double.MAX_VALUE;
            for(int dx=-rr;dx<=rr;dx++) for(int dy=-rr;dy<=rr;dy++) for(int dz=-rr;dz<=rr;dz++){
                BlockPos p=origin.add(dx,dy,dz);
                double d=mc.player.getEyePos().distanceTo(Vec3d.ofCenter(p));
                if(d>r+0.75)continue;
                BlockState st=mc.world.getBlockState(p);
                if(st.isAir()||st.getBlock() instanceof FluidBlock)continue;
                if(CFG.nukerFlatten && p.getY()!=origin.getY()-1)continue;
                if(CFG.nukerFilter) {
                    boolean listed=CFG.nukerFilterIds.contains(Registries.BLOCK.getId(st.getBlock()).toString());
                    if(CFG.nukerWhitelist ? !listed : listed)continue;
                }
                if(CFG.nukerOnlyWhenHoldingTool &&
                        !(mc.player.getMainHandStack().getItem() instanceof MiningToolItem))continue;
                if(CFG.nukerRaycast) {
                    BlockHitResult hit=mc.world.raycast(new net.minecraft.world.RaycastContext(
                            mc.player.getEyePos(),Vec3d.ofCenter(p),
                            net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                            net.minecraft.world.RaycastContext.FluidHandling.NONE,mc.player));
                    if(hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(p))continue;
                }
                if(d<bestD){bestD=d;best=p;}
            }
            return best;
        }

        private static void rotateTo(MinecraftClient mc,BlockPos p) {
            Vec3d c=Vec3d.ofCenter(p);
            double dx=c.x-mc.player.getX(), dy=c.y-mc.player.getEyeY(), dz=c.z-mc.player.getZ();
            mc.player.setYaw((float)Math.toDegrees(Math.atan2(dz,dx))-90f);
            mc.player.setPitch((float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))));
        }

        private static Direction sideFor(BlockPos p,Vec3d eye) {
            Vec3d c=Vec3d.ofCenter(p);
            double ax=Math.abs(eye.x-c.x),ay=Math.abs(eye.y-c.y),az=Math.abs(eye.z-c.z);
            if(ax>=ay&&ax>=az)return eye.x<c.x?Direction.WEST:Direction.EAST;
            if(ay>=ax&&ay>=az)return eye.y<c.y?Direction.DOWN:Direction.UP;
            return eye.z<c.z?Direction.NORTH:Direction.SOUTH;
        }
    }

    public static final class Hud {
        static void render(DrawContext d) {
            MinecraftClient mc=MinecraftClient.getInstance();
            if(mc.player==null)return;
            List<String> active=new ArrayList<>();
            if(CFG.xray)active.add("Xray");
            if(CFG.nuker)active.add("Nuker");
            if(CFG.fullbright)active.add("Fullbright");
            if(CFG.cleaner)active.add("Inventory Cleaner");
            int right=mc.getWindow().getScaledWidth()-8;
            d.drawTextWithShadow(mc.textRenderer,"PeoClient",right-mc.textRenderer.getWidth("PeoClient"),8,0xFFFFFF);
            int y=22;
            for(String s:active){
                int w=mc.textRenderer.getWidth(s);
                d.drawTextWithShadow(mc.textRenderer,s,right-w,y,0xE8E8E8);
                y+=12;
            }
        }
    }
}
