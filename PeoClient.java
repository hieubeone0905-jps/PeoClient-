
package com.peoclient;

import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.lwjgl.glfw.GLFW;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PeoClient implements ClientModInitializer {
    public static final Config CFG = new Config();
    public static KeyBinding menuKey;
    public static KeyBinding xrayKey, nukerKey, fullbrightKey, cleanerKey;
    private int tick;

    @Override public void onInitializeClient() {
        CFG.load();
        menuKey = key("PeoClient menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        xrayKey = key("Xray", GLFW.GLFW_KEY_X);
        nukerKey = key("Nuker", GLFW.GLFW_KEY_N);
        fullbrightKey = key("Fullbright", GLFW.GLFW_KEY_F);
        cleanerKey = key("InventoryCleaner", GLFW.GLFW_KEY_I);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, code, "PeoClient"));
    }

    private void tick(MinecraftClient mc) {
        while (menuKey.wasPressed()) mc.setScreen(new PoeScreen());
        while (xrayKey.wasPressed()) toggleXray(mc);
        while (nukerKey.wasPressed()) CFG.nuker = !CFG.nuker;
        while (fullbrightKey.wasPressed()) toggleFullbright(mc);
        while (cleanerKey.wasPressed()) CFG.cleaner = !CFG.cleaner;
        if (++tick % 10 == 0) CFG.save();

        if (mc.player == null || mc.world == null) return;
        if (CFG.fullbright && CFG.fullbrightMode == 2) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, false, false));
        }
        if (CFG.nuker) NukerLogic.tick(mc);
        if (CFG.cleaner && tick % 2 == 0) CleanerLogic.tick(mc);
    }

    private void toggleXray(MinecraftClient mc) {
        CFG.xray = !CFG.xray;
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }
    private void toggleFullbright(MinecraftClient mc) {
        CFG.fullbright = !CFG.fullbright;
        if (!CFG.fullbright && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        if (CFG.fullbright && CFG.fullbrightMode == 1) CFG.savedGamma = mc.options.getGamma().getValue();
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    public static void reload(MinecraftClient mc) { if (mc.worldRenderer != null) mc.worldRenderer.reload(); }

    public static class Config {
        public boolean xray=false, nuker=false, fullbright=false, cleaner=false;
        public boolean xrayFluids=false, xrayOpacity=true, xrayHideSurface=false;
        public int xrayAlpha=64;
        public double fullbrightGamma=9;
        public int fullbrightMode=0; // 0 table, 1 gamma, 2 potion
        public int nukerMode=0, nukerMulti=2, nukerCooldown=0, nukerShape=0, nukerSort=0;
        public double nukerRange=4.2;
        public boolean nukerFilter=false, nukerWhitelist=false, nukerRaycast=true, nukerFlatten=false, nukerRotate=false;
        public boolean nukerNoParticles=false, nukerHighlight=false, nukerRangeHighlight=false;
        public int maxBlocks=512,maxArrows=128,maxThrowables=64,maxFoods=200,maxWaterBuckets=2,maxLavaBuckets=2,maxMilkBuckets=2;
        public boolean cleanerGreedy=true;
        public String offHandItem="SHIELD";
        public String[] slotItems={"WEAPON","BOW","PICKAXE","AXE","NONE","POTION","FOOD","BLOCK","BLOCK"};
        public String itemsBlacklist="";
        public double savedGamma=1.0;
        public Set<String> xrayBlocks = new LinkedHashSet<>(Arrays.asList(
          "minecraft:coal_ore","minecraft:copper_ore","minecraft:iron_ore","minecraft:gold_ore","minecraft:lapis_ore",
          "minecraft:redstone_ore","minecraft:diamond_ore","minecraft:emerald_ore","minecraft:deepslate_coal_ore",
          "minecraft:deepslate_copper_ore","minecraft:deepslate_iron_ore","minecraft:deepslate_gold_ore",
          "minecraft:deepslate_lapis_ore","minecraft:deepslate_redstone_ore","minecraft:deepslate_diamond_ore",
          "minecraft:deepslate_emerald_ore","minecraft:ancient_debris","minecraft:nether_gold_ore",
          "minecraft:coal_block","minecraft:iron_block","minecraft:gold_block","minecraft:diamond_block","minecraft:emerald_block"));

        private Path path() { return MinecraftClient.getInstance().runDirectory.toPath().resolve("config/peoclient.json"); }
        public void load() {
            try { Path p=path(); if(!Files.exists(p)) {save();return;} Gson g=new Gson(); Config c=g.fromJson(Files.readString(p),Config.class);
                if(c!=null){ copyFrom(c); } } catch(Exception ignored){}
        }
        private void copyFrom(Config c){ this.xray=c.xray;this.nuker=c.nuker;this.fullbright=c.fullbright;this.cleaner=c.cleaner;
          this.xrayFluids=c.xrayFluids;this.xrayOpacity=c.xrayOpacity;this.xrayHideSurface=c.xrayHideSurface;this.xrayAlpha=c.xrayAlpha;
          this.fullbrightGamma=c.fullbrightGamma;this.fullbrightMode=c.fullbrightMode;this.nukerMode=c.nukerMode;this.nukerMulti=c.nukerMulti;
          this.nukerCooldown=c.nukerCooldown;this.nukerShape=c.nukerShape;this.nukerSort=c.nukerSort;this.nukerRange=c.nukerRange;
          this.nukerFilter=c.nukerFilter;this.nukerWhitelist=c.nukerWhitelist;this.nukerRaycast=c.nukerRaycast;this.nukerFlatten=c.nukerFlatten;
          this.nukerRotate=c.nukerRotate;this.nukerNoParticles=c.nukerNoParticles;this.nukerHighlight=c.nukerHighlight;this.nukerRangeHighlight=c.nukerRangeHighlight;
          this.maxBlocks=c.maxBlocks;this.maxArrows=c.maxArrows;this.maxThrowables=c.maxThrowables;this.maxFoods=c.maxFoods;this.maxWaterBuckets=c.maxWaterBuckets;
          this.maxLavaBuckets=c.maxLavaBuckets;this.maxMilkBuckets=c.maxMilkBuckets;this.cleanerGreedy=c.cleanerGreedy;this.offHandItem=c.offHandItem;
          this.slotItems=c.slotItems==null?slotItems:c.slotItems;this.itemsBlacklist=c.itemsBlacklist;this.savedGamma=c.savedGamma;
          if(c.xrayBlocks!=null)this.xrayBlocks=c.xrayBlocks; }
        public void save(){try{Path p=path();Files.createDirectories(p.getParent());Files.writeString(p,new GsonBuilder().setPrettyPrinting().create().toJson(this));}catch(Exception ignored){}}
    }

    static class NukerLogic {
        static void tick(MinecraftClient mc) {
            if(mc.interactionManager==null||mc.player==null||mc.world==null)return;
            double r=CFG.nukerRange; List<BlockPos> list=new ArrayList<>();
            int minY=CFG.nukerFlatten?(int)Math.floor(mc.player.getY()-mc.player.getEyeHeight(mc.player.getPose())+0.2):(int)Math.floor(mc.player.getEyeY()-r);
            for(int x=(int)Math.ceil(-r);x<=Math.ceil(r);x++) for(int y=minY-(int)Math.floor(mc.player.getEyeY());y<=Math.ceil(r);y++) for(int z=(int)Math.ceil(-r);z<=Math.ceil(r);z++){
                BlockPos p=BlockPos.ofFloored(mc.player.getEyePos().add(x,y,z)); double d=CFG.nukerShape==0?
                    Math.max(Math.max(Math.abs(mc.player.getX()-(p.getX()+.5)),Math.abs(mc.player.getEyeY()-(p.getY()+.5))),Math.abs(mc.player.getZ()-(p.getZ()+.5))):
                    mc.player.getEyePos().distanceTo(Vec3d.ofCenter(p));
                if(d-.5>r)continue; BlockState s=mc.world.getBlockState(p); if(s.isAir()||s.getBlock() instanceof FluidBlock)continue;
                if(CFG.nukerFilter){boolean c=filterContains(s.getBlock()); if((!CFG.nukerWhitelist&&c)||(CFG.nukerWhitelist&&!c))continue;}
                if(CFG.nukerRaycast && !mc.world.raycast(new net.minecraft.world.RaycastContext(mc.player.getEyePos(),Vec3d.ofCenter(p),net.minecraft.world.RaycastContext.ShapeType.OUTLINE,net.minecraft.world.RaycastContext.FluidHandling.NONE,mc.player)).getBlockPos().equals(p))continue;
                list.add(p);
            }
            Comparator<BlockPos> cmp=Comparator.comparingDouble(p->mc.player.getEyePos().distanceTo(Vec3d.ofCenter(p)));
            if(CFG.nukerSort==1)cmp=cmp.reversed();
            if(CFG.nukerSort==2)cmp=Comparator.comparingDouble(p->mc.world.getBlockState(p).getHardness(mc.world,p));
            if(CFG.nukerSort==3)cmp=cmp.reversed();
            list.sort(cmp);
            int done=0, limit=CFG.nukerMode==2||CFG.nukerMode==1?CFG.nukerMulti:1;
            for(BlockPos p:list){ if(CFG.nukerCooldown>0 && tickMod(CFG.nukerCooldown))return;
                Direction dir=Direction.UP; if(!mc.interactionManager.updateBlockBreakingProgress(p,dir))continue;
                mc.player.swingHand(Hand.MAIN_HAND); if(++done>=limit)return; if(CFG.nukerMode==3)mc.interactionManager.breakBlock(p);
            }
        }
        static boolean tickMod(int c){return System.nanoTime()%((c+1)*50_000_000L)>40_000_000L;}
        static boolean filterContains(Block b){String id=Registries.BLOCK.getId(b).toString(); return CFG.itemsBlacklist.contains(id);}
    }

    static class CleanerLogic {
        static void tick(MinecraftClient mc) {
            if(mc.player==null||mc.interactionManager==null)return;
            List<Integer> inv=new ArrayList<>(); for(int i=0;i<36;i++) if(!mc.player.getInventory().getStack(i).isEmpty())inv.add(i);
            // Drop explicit blacklist first.
            for(int i:inv){ItemStack s=mc.player.getInventory().getStack(i);String id=Registries.ITEM.getId(s.getItem()).toString();
                if(blacklisted(id)){drop(mc,i);return;}}
            // Keep configured hotbar categories; then remove obvious duplicates/excess.
            Map<String,Integer> counts=new HashMap<>();
            for(int i=9;i<36;i++){ItemStack s=mc.player.getInventory().getStack(i);if(s.isEmpty())continue;
                String cat=category(s); counts.merge(cat,s.getCount(),Integer::sum);}
            for(int i=9;i<36;i++){ItemStack s=mc.player.getInventory().getStack(i);if(s.isEmpty())continue;
                String cat=category(s);int max=switch(cat){case"BLOCK"->CFG.maxBlocks;case"ARROW"->CFG.maxArrows;case"THROWABLE"->CFG.maxThrowables;case"FOOD"->CFG.maxFoods;case"WATER"->CFG.maxWaterBuckets;case"LAVA"->CFG.maxLavaBuckets;case"MILK"->CFG.maxMilkBuckets;default->Integer.MAX_VALUE;};
                if(counts.getOrDefault(cat,0)>max && !"WEAPON".equals(cat)){drop(mc,i);return;}}
            // Hotbar sorting (matches LiquidBounce defaults).
            String[] targets=CFG.slotItems; for(int slot=0;slot<9;slot++){String wanted=targets[slot]; if("NONE".equals(wanted)||"IGNORE".equals(wanted))continue;
                int current=mc.player.getInventory().getStack(slot).isEmpty()? -1:slot;
                if(current>=0&&category(mc.player.getInventory().getStack(current)).equals(wanted))continue;
                for(int i=9;i<36;i++){ItemStack s=mc.player.getInventory().getStack(i);if(!s.isEmpty()&&category(s).equals(wanted)){swap(mc,i,slot);return;}}
            }
        }
        static boolean blacklisted(String id){for(String s:CFG.itemsBlacklist.split(","))if(!s.trim().isEmpty()&&s.trim().equals(id))return true;return false;}
        static void drop(MinecraftClient mc,int invSlot){int screenSlot=invSlot<9?36+invSlot:invSlot;mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId,screenSlot,1,SlotActionType.THROW,mc.player);}
        static void swap(MinecraftClient mc,int fromInv,int hotbar){int from=fromInv<9?36+fromInv:fromInv;int to=36+hotbar;mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId,from,hotbar,SlotActionType.SWAP,mc.player);}
        static String category(ItemStack s){Item i=s.getItem();if(i instanceof SwordItem||i instanceof TridentItem)return"WEAPON";if(i instanceof PickaxeItem)return"PICKAXE";if(i instanceof AxeItem)return"AXE";if(i instanceof BowItem||i instanceof CrossbowItem)return"BOW";if(i instanceof PotionItem)return"POTION";if(s.contains(net.minecraft.component.DataComponentTypes.FOOD))return"FOOD";if(i instanceof BlockItem)return"BLOCK";if(i instanceof ArrowItem)return"ARROW";if(i instanceof ShieldItem)return"SHIELD";if(i==Items.WATER_BUCKET)return"WATER";if(i==Items.LAVA_BUCKET)return"LAVA";if(i==Items.MILK_BUCKET)return"MILK";return"THROWABLE";}
    }
}
