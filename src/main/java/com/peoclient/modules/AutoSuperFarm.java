package com.peoclient.modules;

import com.peoclient.PeoClient;
import net.minecraft.class_1268;
import net.minecraft.class_1542;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_7923;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wurst/Meteor-inspired AutoSuperFarm for PeoClient 1.21.4.
 *
 * This module intentionally uses normal Minecraft interaction calls. It does
 * not touch Nuker, does not inject packets, and does not bypass server checks.
 * Movement is a lightweight client-side target steering system: ripe crops and
 * dropped seeds are selected by distance, then the player walks to them.
 */
public final class AutoSuperFarm {
    private static final class_310 MC = class_310.method_1551();

    private static final double MIN_RANGE = 0.1D;
    private static final double MAX_RANGE = 64.0D;
    private static final double DEFAULT_RANGE = 14.0D;
    private static final double MIN_SPEED = 0.1D;
    private static final double MAX_SPEED = 20.0D;
    private static final double DEFAULT_HARVEST_SPEED = 4.0D;
    private static final double DEFAULT_REPLANT_SPEED = 4.0D;
    private static final int MAX_REPLANT_RETRIES = 2;

    private static boolean enabled;
    private static boolean initialized;
    private static double range = DEFAULT_RANGE;
    private static double harvestSpeed = DEFAULT_HARVEST_SPEED;
    private static double replantSpeed = DEFAULT_REPLANT_SPEED;
    private static boolean checkLineOfSight = false;
    private static boolean faceTarget = true;
    private static boolean swingHand = true;
    private static boolean autoWalk = true;
    private static boolean pickupSeeds = true;
    private static boolean drawReplantSpots = false;
    private static boolean drawHarvestBlocks = false;
    private static boolean drawReplantBlocks = false;

    private static Target target;
    private static PendingReplant pendingReplant;
    private static int actionWait;
    private static int replantWait;
    private static int replantRetries;
    private static int scanWait;
    private static int previousHotbar = -1;
    private static int workingHotbar = -1;

    private AutoSuperFarm() {}

    private record CropSpec(String key, String blockId, String seedId, int maxAge,
                            boolean fruitBlock, boolean defaultHarvest, boolean defaultReplant) {}

    private record Target(class_2338 pos, class_1542 item, CropSpec crop, double distanceSq) {
        boolean isItem() { return item != null; }
    }

    private record PendingReplant(class_2338 pos, CropSpec crop) {}

    private static final List<CropSpec> CROPS = List.of(
            new CropSpec("wheat", "minecraft:wheat", "minecraft:wheat_seeds", 7, false, true, true),
            new CropSpec("beetroot", "minecraft:beetroots", "minecraft:beetroot_seeds", 3, false, true, true),
            new CropSpec("carrot", "minecraft:carrots", "minecraft:carrot", 7, false, true, true),
            new CropSpec("potato", "minecraft:potatoes", "minecraft:potato", 7, false, true, true),
            new CropSpec("nether-wart", "minecraft:nether_wart", "minecraft:nether_wart", 3, false, true, true),
            new CropSpec("pumpkin", "minecraft:pumpkin", null, 0, true, true, false),
            new CropSpec("melon", "minecraft:melon", null, 0, true, true, false),
            new CropSpec("sugar-cane", "minecraft:sugar_cane", null, 0, false, true, false),
            new CropSpec("bamboo", "minecraft:bamboo", null, 0, false, true, false),
            new CropSpec("cactus", "minecraft:cactus", null, 0, false, true, false),
            new CropSpec("kelp", "minecraft:kelp", null, 0, false, true, false),
            new CropSpec("sweet-berries", "minecraft:sweet_berry_bush", "minecraft:sweet_berries", 3, false, true, false),
            new CropSpec("cocoa", "minecraft:cocoa", "minecraft:cocoa_beans", 2, false, true, false)
    );

    public static void toggle() { setEnabled(!enabled); }

    public static void setEnabled(boolean value) {
        enabled = value;
        PeoClient.CFG.autoSuperFarm = value;
        PeoClient.CFG.save();
        if (!value) resetState();
    }

    public static boolean isEnabled() { return enabled; }

    public static void tick(class_310 client) {
        if (!initialized) loadSettings();
        if (!enabled || client.field_1724 == null || client.field_1687 == null) return;
        if (client.field_1755 != null) { stopMovement(); return; }

        if (actionWait > 0) actionWait--;
        if (replantWait > 0) replantWait--;
        if (scanWait > 0) scanWait--;

        // Replant has priority over searching for another crop. This keeps the
        // harvest -> replant loop deterministic instead of running ahead.
        if (pendingReplant != null) {
            if (tryReplant(client, pendingReplant)) return;
        }

        if (target == null || isInvalidTarget(client, target) || scanWait == 0) {
            target = pickupSeeds ? findNearestSeed(client) : null;
            if (target == null) target = findNearestHarvestable(client);
            scanWait = 2;
        }

        if (target == null) { stopMovement(); return; }

        if (target.isItem()) {
            class_243 itemPos = target.item.method_19538();
            double d = client.field_1724.method_19538().method_1022(itemPos);
            if (d <= 1.15D) {
                stopMovement();
                target = null;
                scanWait = 1;
                return; // vanilla ItemEntity pickup handles the seed.
            }
            steerTo(client, itemPos);
            return;
        }

        double d = client.field_1724.method_33571().method_1022(
                class_243.method_24953(target.pos).method_1031(0.5D, 0.5D, 0.5D));
        if (d > interactionDistance(client, target.pos)) {
            if (autoWalk) steerTo(client, class_243.method_24953(target.pos).method_1031(0.5D, 0.0D, 0.5D));
            else stopMovement();
            return;
        }

        stopMovement();
        if (actionWait > 0) return;
        if (checkLineOfSight && !hasLineOfSight(client, target.pos)) return;
        if (faceTarget) faceSmooth(client, class_243.method_24953(target.pos).method_1031(0.5D, 0.5D, 0.5D));

        if (harvest(client, target)) {
            if (target.crop.seedId != null && isReplantEnabled(target.crop.key)) {
                pendingReplant = new PendingReplant(target.pos, target.crop);
                replantRetries = 0;
                replantWait = speedToTicks(replantSpeed);
            }
            target = null;
            actionWait = speedToTicks(harvestSpeed);
            scanWait = 1;
        }
    }

    private static boolean harvest(class_310 client, Target target) {
        // Normal attackBlock call; this is the same client interaction path used
        // by ordinary Minecraft controls rather than a packet-spam shortcut.
        boolean ok = client.field_1761.method_2902(target.pos, class_2350.field_11036);
        if (ok && swingHand) client.field_1724.method_7350(class_1268.field_5808);
        return ok;
    }

    private static boolean tryReplant(class_310 client, PendingReplant pending) {
        class_2338 farmland = pending.pos.method_10074();
        if (!"minecraft:farmland".equals(blockId(client.field_1687.method_8320(farmland)))) {
            pendingReplant = null;
            restoreHotbar(client);
            return false;
        }

        if (isMatureOrFreshCrop(client.field_1687.method_8320(pending.pos), pending.crop, 0)) {
            pendingReplant = null;
            restoreHotbar(client);
            return false;
        }

        int seedSlot = findHotbarItem(client, pending.crop.seedId);
        if (seedSlot < 0) {
            // No seed available: do not spin or spam interaction packets.
            pendingReplant = null;
            restoreHotbar(client);
            return false;
        }

        if (previousHotbar < 0) previousHotbar = client.field_1724.field_7514.field_7545;
        workingHotbar = seedSlot;
        if (client.field_1724.field_7514.field_7545 != seedSlot) {
            client.field_1724.field_7514.method_61496(seedSlot);
            replantWait = Math.max(1, speedToTicks(replantSpeed));
            stopMovement();
            return true;
        }

        if (replantWait > 0) { stopMovement(); return true; }
        if (replantRetries >= MAX_REPLANT_RETRIES) {
            pendingReplant = null;
            restoreHotbar(client);
            return false;
        }

        if (checkLineOfSight && !hasLineOfSight(client, farmland)) return true;
        if (faceTarget) faceSmooth(client, class_243.method_24953(farmland).method_1031(0.5D, 1.0D, 0.5D));

        class_3965 hit = new class_3965(
                class_243.method_24953(farmland).method_1031(0.5D, 1.0D, 0.5D),
                class_2350.field_11036,
                farmland,
                false);
        client.field_1761.method_2896(client.field_1724, class_1268.field_5808, hit);
        if (swingHand) client.field_1724.method_7350(class_1268.field_5808);
        replantRetries++;
        replantWait = Math.max(1, speedToTicks(replantSpeed));
        stopMovement();
        return true;
    }

    private static Target findNearestSeed(class_310 client) {
        class_243 player = client.field_1724.method_19538();
        double max = range * range;
        class_1542 best = null;
        double bestD = Double.MAX_VALUE;
        for (Object object : client.field_1687.method_18112()) {
            if (!(object instanceof class_1542 item)) continue;
            class_1799 stack = item.method_6983();
            if (stack.method_7960() || !"minecraft:wheat_seeds".equals(itemId(stack))) continue;
            double d = player.method_1022(item.method_19538());
            if (d <= max && d < bestD) { best = item; bestD = d; }
        }
        return best == null ? null : new Target(null, best, null, bestD);
    }

    private static Target findNearestHarvestable(class_310 client) {
        class_243 player = client.field_1724.method_19538();
        int px = (int)Math.floor(player.field_1352);
        int py = (int)Math.floor(player.field_1351);
        int pz = (int)Math.floor(player.field_1350);
        int r = (int)Math.ceil(range);
        Target best = null;
        double max = range * range + 1.0D;

        for (int x = px - r; x <= px + r; x++) {
            for (int y = py - 2; y <= py + 2; y++) {
                for (int z = pz - r; z <= pz + r; z++) {
                    class_2338 pos = new class_2338(x, y, z);
                    double d = player.method_1022(class_243.method_24953(pos).method_1031(0.5D, 0.5D, 0.5D));
                    if (d > max || (best != null && d >= best.distanceSq)) continue;
                    class_2680 state = client.field_1687.method_8320(pos);
                    CropSpec spec = cropFor(state);
                    if (spec == null || !isHarvestEnabled(spec.key)) continue;
                    if (!isMature(state, spec)) continue;
                    if (isColumnCrop(spec) && !isTopColumnSegment(client, pos, spec)) continue;
                    if (checkLineOfSight && !hasLineOfSight(client, pos)) continue;
                    best = new Target(pos, null, spec, d);
                }
            }
        }
        return best;
    }


    private static boolean isColumnCrop(CropSpec spec) {
        return spec.blockId.equals("minecraft:sugar_cane")
                || spec.blockId.equals("minecraft:bamboo")
                || spec.blockId.equals("minecraft:cactus")
                || spec.blockId.equals("minecraft:kelp");
    }

    private static boolean isTopColumnSegment(class_310 client, class_2338 pos, CropSpec spec) {
        class_2680 above = client.field_1687.method_8320(pos.method_10086());
        return !spec.blockId.equals(blockId(above));
    }

    private static CropSpec cropFor(class_2680 state) {
        String id = blockId(state);
        for (CropSpec spec : CROPS) if (spec.blockId.equals(id)) return spec;
        return null;
    }

    private static boolean isMature(class_2680 state, CropSpec spec) {
        if (spec.fruitBlock) return true;
        String text = state.toString().toLowerCase(Locale.ROOT);
        int age = extractAge(text);
        if (age >= 0) return age >= spec.maxAge;
        // Multi-block plants have no simple age property; harvest only the
        // upper segment so the base remains intact.
        String id = spec.blockId;
        return id.equals("minecraft:sugar_cane") || id.equals("minecraft:bamboo")
                || id.equals("minecraft:cactus") || id.equals("minecraft:kelp");
    }

    private static boolean isMatureOrFresh(class_2680 state, CropSpec spec, int wantedAge) {
        if (!spec.blockId.equals(blockId(state))) return false;
        int age = extractAge(state.toString().toLowerCase(Locale.ROOT));
        return age < 0 || age == wantedAge;
    }

    private static boolean isInvalidTarget(class_310 client, Target t) {
        if (t.isItem()) {
            if (!client.field_1687.method_62145(t.item)) return true;
            class_1799 stack = t.item.method_6983();
            return stack.method_7960() || !"minecraft:wheat_seeds".equals(itemId(stack));
        }
        class_2680 state = client.field_1687.method_8320(t.pos);
        return !isHarvestEnabled(t.crop.key) || !isMature(state, t.crop);
    }

    private static boolean hasLineOfSight(class_310 client, class_2338 pos) {
        class_243 eye = client.field_1724.method_33571();
        class_243 center = class_243.method_24953(pos).method_1031(0.5D, 0.5D, 0.5D);
        try {
            class_3965 hit = client.field_1687.method_17742(new class_3959(
                    eye, center, class_3959.class_3960.field_17559,
                    class_239.class_242.field_1348, client.field_1724));
            return hit.method_17783() == class_239.class_240.field_1332 && pos.equals(hit.method_17777());
        } catch (Throwable ignored) { return true; }
    }

    private static double interactionDistance(class_310 client, class_2338 pos) {
        // Keep the configured scan range separate from the actual vanilla reach.
        // Movement continues until the block can be interacted with normally.
        return Math.max(2.9D, Math.min(4.5D, range));
    }

    private static void steerTo(class_310 client, class_243 there) {
        class_243 here = client.field_1724.method_19538();
        double dx = there.field_1352 - here.field_1352;
        double dz = there.field_1350 - here.field_1350;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.05D) { stopMovement(); return; }
        float desiredYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        client.field_1724.method_36456(approachAngle(client.field_1724.method_36454(), desiredYaw, 14.0F));
        class_304 forward = client.field_1690.field_1894;
        client.field_1690.field_1881.method_23481(false);
        client.field_1690.field_1913.method_23481(false);
        client.field_1690.field_1849.method_23481(false);
        forward.method_23481(autoWalk);
    }

    private static void faceSmooth(class_310 client, class_243 there) {
        class_243 eye = client.field_1724.method_33571();
        double dx = there.field_1352 - eye.field_1352;
        double dy = there.field_1351 - eye.field_1351;
        double dz = there.field_1350 - eye.field_1350;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
        client.field_1724.method_36456(approachAngle(client.field_1724.method_36454(), yaw, 10.0F));
        client.field_1724.method_36457(approachAngle(client.field_1724.method_36455(), pitch, 8.0F));
    }

    private static float approachAngle(float current, float target, float maxStep) {
        float delta = target - current;
        while (delta > 180.0F) delta -= 360.0F;
        while (delta < -180.0F) delta += 360.0F;
        if (delta > maxStep) delta = maxStep;
        if (delta < -maxStep) delta = -maxStep;
        return current + delta;
    }

    private static int findHotbarItem(class_310 client, String wantedId) {
        if (wantedId == null) return -1;
        for (int slot = 0; slot < 9; slot++) {
            class_1799 stack = client.field_1724.field_7514.method_5438(slot);
            if (!stack.method_7960() && wantedId.equals(itemId(stack))) return slot;
        }
        return -1;
    }

    private static void restoreHotbar(class_310 client) {
        if (previousHotbar >= 0 && client.field_1724 != null) {
            client.field_1724.field_7514.method_61496(previousHotbar);
        }
        previousHotbar = -1;
        workingHotbar = -1;
    }

    private static void stopMovement() {
        if (MC.field_1724 == null) return;
        MC.field_1690.field_1894.method_23481(false);
        MC.field_1690.field_1881.method_23481(false);
        MC.field_1690.field_1913.method_23481(false);
        MC.field_1690.field_1849.method_23481(false);
        class_304.method_1424();
        class_243 v = MC.field_1724.method_18798();
        MC.field_1724.method_18799(new class_243(0.0D, v.field_1351, 0.0D));
        MC.field_1724.method_24830(false);
    }

    private static int speedToTicks(double speed) {
        return Math.max(1, (int)Math.round(5.0D / Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed))));
    }

    private static int extractAge(String text) {
        int p = text.indexOf("age=");
        if (p < 0) return -1;
        p += 4;
        int end = p;
        while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
        try { return Integer.parseInt(text.substring(p, end)); }
        catch (RuntimeException ignored) { return -1; }
    }

    private static String blockId(class_2680 state) {
        return class_7923.field_41175.method_10221(state.method_26204()).toString().toLowerCase(Locale.ROOT);
    }

    private static String itemId(class_1799 stack) {
        return class_7923.field_41178.method_10221(stack.method_7909()).toString().toLowerCase(Locale.ROOT);
    }

    private static boolean isHarvestEnabled(String key) {
        return boolSetting(PeoClient.CFG.autoSuperFarmHarvest, key, true);
    }

    private static boolean isReplantEnabled(String key) {
        return boolSetting(PeoClient.CFG.autoSuperFarmReplant, key, false);
    }

    private static boolean boolSetting(Map<String, Boolean> map, String key, boolean fallback) {
        if (map == null || !map.containsKey(key)) return fallback;
        return Boolean.TRUE.equals(map.get(key));
    }

    public static List<String> getCropKeys() {
        List<String> out = new ArrayList<>();
        for (CropSpec spec : CROPS) out.add(spec.key);
        return out;
    }

    public static String displayName(String key) {
        return switch (key) {
            case "wheat" -> "Wheat";
            case "beetroot" -> "Beetroot";
            case "carrot" -> "Carrots";
            case "potato" -> "Potatoes";
            case "nether-wart" -> "Nether Wart";
            case "pumpkin" -> "Pumpkins";
            case "melon" -> "Melons";
            case "sugar-cane" -> "Sugar Cane";
            case "bamboo" -> "Bamboo";
            case "cactus" -> "Cactus";
            case "kelp" -> "Kelp";
            case "sweet-berries" -> "Sweet Berries";
            case "cocoa" -> "Cocoa";
            default -> key;
        };
    }

    public static boolean isHarvestEnabledFor(String key) { return isHarvestEnabled(key); }
    public static boolean isReplantEnabledFor(String key) { return isReplantEnabled(key); }

    public static void setHarvestEnabled(String key, boolean value) {
        PeoClient.CFG.autoSuperFarmHarvest.put(key, value);
        PeoClient.CFG.save();
    }

    public static void setReplantEnabled(String key, boolean value) {
        PeoClient.CFG.autoSuperFarmReplant.put(key, value);
        PeoClient.CFG.save();
    }

    public static boolean isCheckLineOfSight() { return checkLineOfSight; }
    public static void setCheckLineOfSight(boolean value) { checkLineOfSight = value; PeoClient.CFG.autoSuperFarmCheckLineOfSight = value; PeoClient.CFG.save(); }
    public static boolean isFaceTarget() { return faceTarget; }
    public static void setFaceTarget(boolean value) { faceTarget = value; PeoClient.CFG.autoSuperFarmFaceTarget = value; PeoClient.CFG.save(); }
    public static boolean isSwingHand() { return swingHand; }
    public static void setSwingHand(boolean value) { swingHand = value; PeoClient.CFG.autoSuperFarmSwingHand = value; PeoClient.CFG.save(); }
    public static boolean isAutoWalk() { return autoWalk; }
    public static void setAutoWalk(boolean value) { autoWalk = value; PeoClient.CFG.autoSuperFarmAutoWalk = value; PeoClient.CFG.save(); }
    public static boolean isPickupSeeds() { return pickupSeeds; }
    public static void setPickupSeeds(boolean value) { pickupSeeds = value; PeoClient.CFG.autoSuperFarmPickupSeeds = value; PeoClient.CFG.save(); }

    public static double getRange() { return range; }
    public static void setRange(double value) { range = clamp(value, MIN_RANGE, MAX_RANGE); PeoClient.CFG.autoSuperFarmRange = range; PeoClient.CFG.save(); }
    public static double getHarvestSpeed() { return harvestSpeed; }
    public static void setHarvestSpeed(double value) { harvestSpeed = clamp(value, MIN_SPEED, MAX_SPEED); PeoClient.CFG.autoSuperFarmHarvestSpeedX = harvestSpeed; PeoClient.CFG.save(); }
    public static double getReplantSpeed() { return replantSpeed; }
    public static void setReplantSpeed(double value) { replantSpeed = clamp(value, MIN_SPEED, MAX_SPEED); PeoClient.CFG.autoSuperFarmReplantSpeedX = replantSpeed; PeoClient.CFG.save(); }

    // Backward-compatible accessors used by older PoeScreen builds.
    public static int getRadius() { return (int)Math.round(range); }
    public static void setRadius(int value) { setRange(value); }
    public static int getHarvestSpeedInt() { return (int)Math.round(harvestSpeed); }
    public static boolean isWheatEnabled() { return isHarvestEnabled("wheat"); }
    public static boolean isBeetrootEnabled() { return isHarvestEnabled("beetroot"); }
    public static boolean isPumpkinEnabled() { return isHarvestEnabled("pumpkin"); }
    public static boolean isMelonEnabled() { return isHarvestEnabled("melon"); }
    public static boolean isCarrotEnabled() { return isHarvestEnabled("carrot"); }
    public static boolean isPotatoEnabled() { return isHarvestEnabled("potato"); }
    public static void setCropEnabled(String crop, boolean value) { setHarvestEnabled(crop, value); }

    private static void loadSettings() {
        enabled = PeoClient.CFG.autoSuperFarm;
        range = clamp(PeoClient.CFG.autoSuperFarmRange, MIN_RANGE, MAX_RANGE);
        harvestSpeed = clamp(PeoClient.CFG.autoSuperFarmHarvestSpeedX, MIN_SPEED, MAX_SPEED);
        replantSpeed = clamp(PeoClient.CFG.autoSuperFarmReplantSpeedX, MIN_SPEED, MAX_SPEED);
        checkLineOfSight = PeoClient.CFG.autoSuperFarmCheckLineOfSight;
        faceTarget = PeoClient.CFG.autoSuperFarmFaceTarget;
        swingHand = PeoClient.CFG.autoSuperFarmSwingHand;
        autoWalk = PeoClient.CFG.autoSuperFarmAutoWalk;
        pickupSeeds = PeoClient.CFG.autoSuperFarmPickupSeeds;
        if (PeoClient.CFG.autoSuperFarmHarvest == null) PeoClient.CFG.autoSuperFarmHarvest = new java.util.LinkedHashMap<>();
        if (PeoClient.CFG.autoSuperFarmReplant == null) PeoClient.CFG.autoSuperFarmReplant = new java.util.LinkedHashMap<>();
        for (CropSpec spec : CROPS) {
            PeoClient.CFG.autoSuperFarmHarvest.putIfAbsent(spec.key, spec.defaultHarvest);
            PeoClient.CFG.autoSuperFarmReplant.putIfAbsent(spec.key, spec.defaultReplant);
        }
        initialized = true;
    }

    private static void resetState() {
        stopMovement();
        target = null;
        pendingReplant = null;
        actionWait = 0;
        replantWait = 0;
        replantRetries = 0;
        scanWait = 0;
        restoreHotbar(MC);
    }

    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
}
