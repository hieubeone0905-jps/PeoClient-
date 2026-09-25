package com.peoclient.modules;

import com.peoclient.PeoClient;
import net.minecraft.class_1268;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_304;
import net.minecraft.class_7923;

import java.util.Locale;

/**
 * AutoSuperFarm
 *
 * Lightweight local farm AI for Fabric 1.21.4.
 *
 * Targets only:
 *  - mature wheat (age=7): break, then immediately replant wheat seeds;
 *  - melon blocks: harvest the fruit block only;
 *  - pumpkin blocks: harvest the fruit block only.
 *
 * Stems are never selected. The module uses the normal interaction manager for
 * breaking/placing and a small steering controller for movement. It does not
 * toggle, configure, or call Nuker, so enabling it does not modify Nuker's
 * settings/state.
 */
public final class AutoSuperFarm {
    private static final class_310 MC = class_310.method_1551();

    private static final int MIN_RADIUS = 4;
    private static final int MAX_RADIUS = 32;
    private static final int DEFAULT_RADIUS = 14;
    private static final double DEFAULT_SPEED = 0.115D;
    private static final int REPLANT_TIMEOUT = 8;
    private static final int BREAK_COOLDOWN = 2;
    private static final int TARGET_RESCAN = 3;
    private static final int MIN_HARVEST_PER_TICK = 1;
    private static final int MAX_HARVEST_PER_TICK = 8;
    private static final int DEFAULT_HARVEST_PER_TICK = 1;

    private static boolean enabled;
    private static boolean initialized;
    private static int radius = DEFAULT_RADIUS;
    private static double moveSpeed = DEFAULT_SPEED;
    private static Target target;
    private static class_2338 replantPos;
    private static Kind replantKind;
    private static int replantWait;
    private static int replantAttempts;
    private static int breakWait;
    private static int rescanWait;
    private static int harvestPerTick = DEFAULT_HARVEST_PER_TICK;
    private static boolean harvestWheat = true;
    private static boolean harvestBeetroot = true;
    private static boolean harvestPumpkin = true;
    private static boolean harvestMelon = true;
    private static boolean harvestCarrot = true;
    private static boolean harvestPotato = true;
    private static int previousHotbar = -1;
    private static int workingHotbar = -1;

    private AutoSuperFarm() {}

    private record Target(class_2338 pos, Kind kind, double distanceSq) {}
    private enum Kind { WHEAT(true, "minecraft:wheat_seeds"), BEETROOT(true, "minecraft:beetroot_seeds"), CARROT(true, "minecraft:carrot"), POTATO(true, "minecraft:potato"), MELON(false, null), PUMPKIN(false, null);
        final boolean replant; final String seedId;
        Kind(boolean replant, String seedId) { this.replant = replant; this.seedId = seedId; }
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        PeoClient.CFG.autoSuperFarm = value;
        PeoClient.CFG.save();
        if (!value) {
            stopMovement();
            target = null;
            replantPos = null;
            replantKind = null;
            replantWait = 0;
            replantAttempts = 0;
            breakWait = 0;
            rescanWait = 0;
            restoreHotbar();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void tick(class_310 client) {
        if (!initialized) {
            enabled = PeoClient.CFG.autoSuperFarm;
            radius = clamp(PeoClient.CFG.autoSuperFarmRadius, MIN_RADIUS, MAX_RADIUS);
            moveSpeed = Math.max(0.04D, Math.min(0.22D, PeoClient.CFG.autoSuperFarmMoveSpeed));
            harvestPerTick = clamp(PeoClient.CFG.autoSuperFarmHarvestSpeed, MIN_HARVEST_PER_TICK, MAX_HARVEST_PER_TICK);
            harvestWheat = PeoClient.CFG.autoSuperFarmWheat;
            harvestBeetroot = PeoClient.CFG.autoSuperFarmBeetroot;
            harvestPumpkin = PeoClient.CFG.autoSuperFarmPumpkin;
            harvestMelon = PeoClient.CFG.autoSuperFarmMelon;
            harvestCarrot = PeoClient.CFG.autoSuperFarmCarrot;
            harvestPotato = PeoClient.CFG.autoSuperFarmPotato;
            initialized = true;
        }

        if (!enabled || client.field_1724 == null || client.field_1687 == null || client.field_1761 == null) {
            return;
        }

        // Never operate through a GUI/container. This prevents inventory/server GUI
        // traffic from being mixed with farm actions.
        if (client.field_1755 != null) {
            stopMovement();
            return;
        }

        if (breakWait > 0) breakWait--;

        if (replantPos != null) {
            if (tryReplant(client)) return;
            if (++replantWait > REPLANT_TIMEOUT) {
                restoreHotbar();
                replantPos = null;
                replantWait = 0;
                replantAttempts = 0;
            } else {
                stopMovement();
            }
            return;
        }

        if (rescanWait > 0) rescanWait--;

        if (target == null || isInvalidTarget(client, target) || rescanWait == 0) {
            Target next = findNearestTarget(client);
            if (target == null || next == null || next.pos().equals(target.pos()) || next.distanceSq() + 1.0D < target.distanceSq()) {
                target = next;
            }
            rescanWait = TARGET_RESCAN;
        }

        if (target == null) {
            // No crop in the scan sphere: slowly rotate the search direction by
            // moving in a small arc instead of standing permanently still.
            searchPatrol(client);
            return;
        }

        double distanceSq = client.field_1724.method_33571().method_1022(class_243.method_24953(target.pos).method_1031(0.5D, 0.5D, 0.5D));

        // Keep steering until we are genuinely close to the crop. Do not snap
        // the camera every tick while walking; that was the source of the
        // jerky movement seen in-game. Rotation is only smoothed as needed.
        if (distanceSq > 2.25D) {
            steerTo(client, target.pos);
            return;
        }

        stopMovement();
        faceSmooth(client, target.pos);
        if (breakWait > 0) return;

        // Process a configurable number of harvest actions per tick. Every action
        // is still validated against this module's own crop filter and same-Y
        // farming plane; Nuker is never called or modified.
        int actions = Math.max(1, harvestPerTick);
        for (int i = 0; i < actions; i++) {
            Target current = (i == 0 && target != null) ? target : findNearestTarget(client);
            if (current == null) break;
            class_2350 side = class_2350.field_11036;
            client.field_1761.method_2902(current.pos, side);
            client.field_1724.method_6104(class_1268.field_5808);
            if (current.kind.replant) {
                replantPos = current.pos;
                replantKind = current.kind;
                replantWait = 0;
                replantAttempts = 0;
                target = null;
                break;
            }
            target = null;
        }
        breakWait = BREAK_COOLDOWN;
    }

    private static Target findNearestTarget(class_310 client) {
        // Use the player's FEET block, not the eye block. The eye is ~1.6 blocks
        // higher and the old implementation therefore scanned the wrong Y plane.
        class_243 playerPos = client.field_1724.method_19538();
        int playerX = (int) Math.floor(playerPos.field_1352);
        int playerY = (int) Math.floor(playerPos.field_1351);
        int playerZ = (int) Math.floor(playerPos.field_1350);
        class_2338 playerBlock = new class_2338(playerX, playerY, playerZ);
        Target best = null;
        int r = radius;

        // The player can stand directly on the farmland (for example Y=3.9375
        // while the farmland block is Y=3), or one block above it.  The crop is
        // therefore determined from the farmland/support block instead of
        // blindly requiring cropY == playerY.  This preserves the old elevated
        // case while making the normal in-farm position work correctly.
        int farmY;
        class_2680 currentState = client.field_1687.method_8320(playerBlock);
        class_2680 belowState = client.field_1687.method_8320(playerBlock.method_10074());
        if ("minecraft:farmland".equals(blockId(currentState))) {
            farmY = playerY;
        } else if ("minecraft:farmland".equals(blockId(belowState))) {
            farmY = playerY - 1;
        } else {
            // No visible farmland directly supporting the player. Keep the
            // previous same-level behavior as a fallback.
            farmY = playerY - 1;
        }
        int cropY = farmY + 1;
        class_2338 center = new class_2338(playerX, cropY, playerZ);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r) continue;
                class_2338 pos = center.method_10069(x, 0, z);
                    class_2680 state = client.field_1687.method_8320(pos);
                    Kind kind = classify(state);
                    if (kind == null) continue;

                if (pos.method_10264() != cropY) continue;
                double d = client.field_1724.method_33571().method_1022(class_243.method_24953(pos).method_1031(0.5D, 0.5D, 0.5D));
                double maxDistanceSq = (double) r * r + 1.0D;
                if (d > maxDistanceSq || (best != null && d >= best.distanceSq())) continue;
                best = new Target(pos, kind, d);
            }
        }
        return best;
    }

    private static Kind classify(class_2680 state) {
        String id = blockId(state);
        if ("minecraft:melon".equals(id)) return harvestMelon ? Kind.MELON : null;
        if ("minecraft:pumpkin".equals(id)) return harvestPumpkin ? Kind.PUMPKIN : null;
        if ("minecraft:wheat".equals(id)) return harvestWheat && extractAge(state.toString().toLowerCase(Locale.ROOT)) >= 7 ? Kind.WHEAT : null;
        if ("minecraft:beetroots".equals(id)) return harvestBeetroot && extractAge(state.toString().toLowerCase(Locale.ROOT)) >= 3 ? Kind.BEETROOT : null;
        if ("minecraft:carrots".equals(id)) return harvestCarrot && extractAge(state.toString().toLowerCase(Locale.ROOT)) >= 7 ? Kind.CARROT : null;
        if ("minecraft:potatoes".equals(id)) return harvestPotato && extractAge(state.toString().toLowerCase(Locale.ROOT)) >= 7 ? Kind.POTATO : null;
        return null;
    }

    private static int extractAge(String text) {
        int p = text.indexOf("age=");
        if (p < 0) return -1;
        p += 4;
        int end = p;
        while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
        try {
            return Integer.parseInt(text.substring(p, end));
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static boolean isInvalidTarget(class_310 client, Target t) {
        class_2680 state = client.field_1687.method_8320(t.pos);
        return classify(state) != t.kind;
    }

    private static void steerTo(class_310 client, class_2338 pos) {
        class_243 here = client.field_1724.method_19538();
        class_243 there = class_243.method_24953(pos).method_1031(0.5D, 0.0D, 0.5D);
        double dx = there.field_1352 - here.field_1352;
        double dz = there.field_1350 - here.field_1350;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.05D) {
            stopMovement();
            return;
        }

        // Do not fight ClientPlayerEntity's normal movement by writing velocity.
        // Instead, steer the normal W input toward the target. This is much more
        // stable on multiplayer servers because vanilla movement and movement
        // packets remain in control of the player's horizontal motion.
        float desiredYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float yaw = approachAngle(client.field_1724.method_36454(), desiredYaw, 18.0F);
        client.field_1724.method_36456(yaw);

        class_304 forward = client.field_1690.field_1894;
        class_304 back = client.field_1690.field_1881;
        class_304 left = client.field_1690.field_1913;
        class_304 right = client.field_1690.field_1849;
        back.method_23481(false);
        left.method_23481(false);
        right.method_23481(false);
        forward.method_23481(true);
    }

    private static void searchPatrol(class_310 client) {
        // Do not spin the camera when no target is found. Spinning made the
        // module look active while providing no useful movement and also caused
        // visible jitter. The scan is repeated as the player moves.
        stopMovement();
    }

    private static void faceSmooth(class_310 client, class_2338 pos) {
        class_243 eye = client.field_1724.method_33571();
        class_243 dst = class_243.method_24953(pos).method_1031(0.5D, 0.5D, 0.5D);
        double dx = dst.field_1352 - eye.field_1352;
        double dy = dst.field_1351 - eye.field_1351;
        double dz = dst.field_1350 - eye.field_1350;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) return;
        float desiredYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float desiredPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        float yaw = approachAngle(client.field_1724.method_36454(), desiredYaw, 10.0F);
        float pitch = approachAngle(client.field_1724.method_36455(), Math.max(-90.0F, Math.min(90.0F, desiredPitch)), 8.0F);
        client.field_1724.method_36456(yaw);
        client.field_1724.method_36457(pitch);
    }

    private static float approachAngle(float current, float target, float maxStep) {
        float delta = target - current;
        while (delta > 180.0F) delta -= 360.0F;
        while (delta < -180.0F) delta += 360.0F;
        if (delta > maxStep) delta = maxStep;
        if (delta < -maxStep) delta = -maxStep;
        return current + delta;
    }

    private static boolean tryReplant(class_310 client) {
        if (replantPos == null || replantKind == null) return false;

        // The block immediately below the harvested crop must be farmland.
        class_2338 farmland = replantPos.method_10074();
        class_2680 below = client.field_1687.method_8320(farmland);
        if (!"minecraft:farmland".equals(blockId(below))) {
            restoreHotbar();
            replantPos = null;
            replantKind = null;
            replantWait = 0;
            replantAttempts = 0;
            return false;
        }

        // IMPORTANT: keep the seed selected until the server/client state shows
        // that the crop was actually replanted.  The old implementation restored
        // the previous slot immediately after interactBlock(), which could make
        // the first few placements work and then silently lose later placements.
        // One normal interaction is issued, then we verify the result on the next
        // client tick.  This remains vanilla interaction; no packet spoofing or
        // anti-cheat bypass is used.
        String expectedBlock;
        int expectedAge;
        switch (replantKind) {
            case WHEAT -> { expectedBlock = "minecraft:wheat"; expectedAge = 0; }
            case BEETROOT -> { expectedBlock = "minecraft:beetroots"; expectedAge = 0; }
            case CARROT -> { expectedBlock = "minecraft:carrots"; expectedAge = 0; }
            case POTATO -> { expectedBlock = "minecraft:potatoes"; expectedAge = 0; }
            default -> { // Fruit crops do not use the replant path.
                restoreHotbar();
                replantPos = null;
                replantKind = null;
                replantWait = 0;
                replantAttempts = 0;
                return false;
            }
        }

        class_2680 planted = client.field_1687.method_8320(replantPos);
        String plantedId = blockId(planted);
        int plantedAge = extractAge(planted.toString().toLowerCase(Locale.ROOT));
        if (expectedBlock.equals(plantedId) && plantedAge == expectedAge) {
            restoreHotbar();
            replantPos = null;
            replantKind = null;
            replantWait = 0;
            replantAttempts = 0;
            target = null;
            stopMovement();
            return true;
        }

        int seed = findReplantSlot(client, replantKind);
        if (seed < 0) {
            restoreHotbar();
            replantPos = null;
            replantKind = null;
            replantWait = 0;
            replantAttempts = 0;
            return false;
        }

        var inv = client.field_1724.method_31548();
        if (previousHotbar < 0) previousHotbar = inv.field_7545;

        int hotbar = ensureHotbar(client, seed);
        if (hotbar < 0) return false;
        workingHotbar = hotbar;

        if (inv.field_7545 != hotbar) {
            inv.method_61496(hotbar);
            stopMovement();
            // Give the normal selected-slot state one client tick to settle.
            replantWait = 1;
            return false;
        }

        if (replantWait > 0) {
            replantWait--;
            stopMovement();
            return false;
        }

        // If a previous attempt did not result in the crop appearing, allow only
        // a very small number of normal retries. This is deliberately bounded so
        // a server that rejects placement cannot be flooded with interactions.
        if (replantAttempts >= 2) {
            restoreHotbar();
            replantPos = null;
            replantKind = null;
            replantWait = 0;
            replantAttempts = 0;
            target = null;
            stopMovement();
            return false;
        }

        faceSmooth(client, replantPos);
        class_3965 hit = new class_3965(
                class_243.method_24953(farmland).method_1031(0.5D, 1.0D, 0.5D),
                class_2350.field_11036,
                farmland,
                false);

        // Normal vanilla block interaction.  Keep the seed selected after the
        // click so the following tick can confirm the placement before restoring
        // the player's original hotbar slot.
        client.field_1761.method_2896(client.field_1724, class_1268.field_5808, hit);
        replantAttempts++;
        replantWait = 1;
        stopMovement();
        return false;
    }

    private static int findReplantSlot(class_310 client, Kind kind) {
        if (kind == null || kind.seedId == null) return -1;
        var inv = client.field_1724.method_31548();
        for (int i = 0; i < 36; i++) {
            class_1799 stack = inv.method_5438(i);
            if (!stack.method_7960() && kind.seedId.equals(itemId(stack))) return i;
        }
        return -1;
    }

    private static int ensureHotbar(class_310 client, int slot) {
        var inv = client.field_1724.method_31548();
        if (slot >= 0 && slot < 9) return slot;

        int hotbar = findEmptyHotbar(client);
        if (hotbar < 0) hotbar = 8;
        int sourceScreen = slot < 9 ? 36 + slot : slot;
        client.field_1761.method_2906(client.field_1724.field_7512.field_7763,
                sourceScreen, hotbar, class_1713.field_7791, client.field_1724);
        return hotbar;
    }

    private static int findEmptyHotbar(class_310 client) {
        var inv = client.field_1724.method_31548();
        for (int i = 0; i < 9; i++) if (inv.method_5438(i).method_7960()) return i;
        return -1;
    }

    private static void restoreHotbar() {
        if (MC.field_1724 == null) return;
        if (previousHotbar >= 0 && previousHotbar < 9) {
            MC.field_1724.method_31548().method_61496(previousHotbar);
        }
        previousHotbar = -1;
        workingHotbar = -1;
    }

    private static void stopMovement() {
        if (MC.field_1724 == null) return;
        class_304 forward = MC.field_1690.field_1894;
        class_304 back = MC.field_1690.field_1881;
        class_304 left = MC.field_1690.field_1913;
        class_304 right = MC.field_1690.field_1849;
        forward.method_23481(false);
        back.method_23481(false);
        left.method_23481(false);
        right.method_23481(false);
        class_304.method_1424();
        class_243 v = MC.field_1724.method_18798();
        MC.field_1724.method_18799(new class_243(0.0D, v.field_1351, 0.0D));
        MC.field_1724.method_24830(false);
    }

    private static String blockId(class_2680 state) {
        return class_7923.field_41175.method_10221(state.method_26204()).toString().toLowerCase(Locale.ROOT);
    }

    private static String itemId(class_1799 stack) {
        return class_7923.field_41178.method_10221(stack.method_7909()).toString().toLowerCase(Locale.ROOT);
    }

    public static int getHarvestSpeed() { return harvestPerTick; }
    public static void setHarvestSpeed(int value) {
        harvestPerTick = clamp(value, MIN_HARVEST_PER_TICK, MAX_HARVEST_PER_TICK);
        PeoClient.CFG.autoSuperFarmHarvestSpeed = harvestPerTick;
        PeoClient.CFG.save();
    }
    public static int getRadius() { return radius; }
    public static void setRadius(int value) {
        radius = clamp(value, MIN_RADIUS, MAX_RADIUS);
        PeoClient.CFG.autoSuperFarmRadius = radius;
        PeoClient.CFG.save();
    }
    public static boolean isWheatEnabled() { return harvestWheat; }
    public static boolean isBeetrootEnabled() { return harvestBeetroot; }
    public static boolean isPumpkinEnabled() { return harvestPumpkin; }
    public static boolean isMelonEnabled() { return harvestMelon; }
    public static boolean isCarrotEnabled() { return harvestCarrot; }
    public static boolean isPotatoEnabled() { return harvestPotato; }
    public static void setCropEnabled(String crop, boolean value) {
        switch (crop) {
            case "wheat" -> harvestWheat = value;
            case "beetroot" -> harvestBeetroot = value;
            case "pumpkin" -> harvestPumpkin = value;
            case "melon" -> harvestMelon = value;
            case "carrot" -> harvestCarrot = value;
            case "potato" -> harvestPotato = value;
            default -> { return; }
        }
        switch (crop) {
            case "wheat" -> PeoClient.CFG.autoSuperFarmWheat = value;
            case "beetroot" -> PeoClient.CFG.autoSuperFarmBeetroot = value;
            case "pumpkin" -> PeoClient.CFG.autoSuperFarmPumpkin = value;
            case "melon" -> PeoClient.CFG.autoSuperFarmMelon = value;
            case "carrot" -> PeoClient.CFG.autoSuperFarmCarrot = value;
            case "potato" -> PeoClient.CFG.autoSuperFarmPotato = value;
        }
        PeoClient.CFG.save();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
