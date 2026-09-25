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
            } else {
                stopMovement();
            }
            return;
        }

        if (rescanWait > 0) rescanWait--;

        if (target == null || rescanWait == 0 || isInvalidTarget(client, target)) {
            target = findNearestTarget(client);
            rescanWait = TARGET_RESCAN;
        }

        if (target == null) {
            // No crop in the scan sphere: slowly rotate the search direction by
            // moving in a small arc instead of standing permanently still.
            searchPatrol(client);
            return;
        }

        double distance = client.field_1724.method_33571().method_1022(class_243.method_24953(target.pos));
        face(client, target.pos);

        if (distance > 3.8D) {
            steerTo(client, target.pos);
            return;
        }

        stopMovement();
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
                target = null;
                break;
            }
            target = null;
        }
        breakWait = BREAK_COOLDOWN;
    }

    private static Target findNearestTarget(class_310 client) {
        class_2338 center = class_2338.method_49638(client.field_1724.method_33571());
        Target best = null;
        int r = radius;
        int farmY = center.method_10264();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                    if (x * x + z * z > r * r) continue;
                    class_2338 pos = center.method_10069(x, 0, z);
                    class_2680 state = client.field_1687.method_8320(pos);
                    Kind kind = classify(state);
                    if (kind == null) continue;

                    if (pos.method_10264() != farmY) continue;
                    double d = client.field_1724.method_33571().method_1022(class_243.method_24953(pos));
                    if (d > (double) r * r || (best != null && d >= best.distanceSq())) continue;
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
        class_243 here = client.field_1724.method_33571();
        class_243 there = class_243.method_24953(pos).method_1031(0.0D, 0.35D, 0.0D);
        double dx = there.field_1352 - here.field_1352;
        double dz = there.field_1350 - here.field_1350;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001D) {
            stopMovement();
            return;
        }

        double vx = dx / len * moveSpeed;
        double vz = dz / len * moveSpeed;
        client.field_1724.method_18799(new class_243(vx, client.field_1724.method_18798().field_1351, vz));
        face(client, pos);
    }

    private static void searchPatrol(class_310 client) {
        float yaw = client.field_1724.method_36454() + 1.8F;
        client.field_1724.method_36456(yaw);
        client.field_1724.method_18799(new class_243(0.0D, client.field_1724.method_18798().field_1351, 0.0D));
    }

    private static void face(class_310 client, class_2338 pos) {
        class_243 eye = client.field_1724.method_33571();
        class_243 dst = class_243.method_24953(pos).method_1031(0.5D, 0.5D, 0.5D);
        double dx = dst.field_1352 - eye.field_1352;
        double dy = dst.field_1351 - eye.field_1351;
        double dz = dst.field_1350 - eye.field_1350;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) return;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        client.field_1724.method_36456(yaw);
        client.field_1724.method_36457(Math.max(-90.0F, Math.min(90.0F, pitch)));
    }

    private static boolean tryReplant(class_310 client) {
        if (replantPos == null) return false;
        class_2338 farmland = replantPos.method_10074();
        class_2680 below = client.field_1687.method_8320(farmland);
        String belowId = blockId(below);
        if (!"minecraft:farmland".equals(belowId)) {
            restoreHotbar();
            replantPos = null;
            replantKind = null;
            return false;
        }

        int seed = findReplantSlot(client, replantKind);
        if (seed < 0) {
            // No seed currently available. Do not substitute another item and do
            // not break immature wheat; leave the farm block alone.
            return false;
        }

        int hotbar = ensureHotbar(client, seed);
        if (hotbar < 0) return false;
        if (previousHotbar < 0) previousHotbar = client.field_1724.method_31548().field_7545;
        workingHotbar = hotbar;
        client.field_1724.method_31548().method_61496(hotbar);
        face(client, replantPos);

        class_3965 hit = new class_3965(
                class_243.method_24953(farmland).method_1031(0.5D, 1.0D, 0.5D),
                class_2350.field_11036,
                farmland,
                false);
        client.field_1761.method_2896(client.field_1724, class_1268.field_5808, hit);

        // One normal interaction packet is enough to plant the seed. Restore the
        // player's original hotbar selection immediately after issuing it so the
        // module does not leave the seed selected.
        restoreHotbar();
        replantPos = null;
        replantWait = 0;
        return true;
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
