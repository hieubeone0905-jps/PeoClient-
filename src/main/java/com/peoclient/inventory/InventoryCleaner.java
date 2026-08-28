package com.peoclient.inventory;

import com.peoclient.PeoClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;

import java.util.*;

/**
 * PeoClient InventoryCleaner.
 *
 * Behaviour is independently implemented from the public LiquidBounce InventoryCleaner
 * design: categorisation, per-category quotas, blacklist handling, hotbar targets,
 * greedy allocation, best-item selection and stack consolidation are represented here
 * without copying LiquidBounce implementation code.
 */
public final class InventoryCleaner {
    private InventoryCleaner() {}

    private static int cooldown;

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (cooldown > 0) { cooldown--; return; }

        // 1) Explicit blacklist always wins.
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && PeoClient.CFG.cleanerBlacklistSet.contains(id(stack))) {
                drop(mc, playerInventoryScreenSlot(slot));
                cooldown = PeoClient.CFG.cleanerActionDelay;
                return;
            }
        }

        // 2) Build a deterministic view of the inventory and keep the best item for
        // one-item categories before considering disposable duplicates.
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty()) entries.add(new Entry(i, s.copy(), classify(s), score(s)));
        }

        // 3) Enforce configured quotas in the same priority-oriented order used by
        // modern inventory-cleaner designs: special one-per-function items first,
        // consumables/blocks/throwables afterwards.
        if (disposeExcess(mc, entries)) return;

        // 4) Merge partial stacks when possible. This reduces fragmentation before
        // dropping low-value leftovers.
        if (PeoClient.CFG.cleanerMergeStacks && mergeOne(mc, entries)) return;

        // 5) Fill configured hotbar positions with the highest-scoring compatible item.
        if (sortHotbar(mc, entries)) return;
    }

    private static boolean disposeExcess(MinecraftClient mc, List<Entry> entries) {
        Map<Category, Integer> amount = new EnumMap<>(Category.class);
        Map<Category, List<Entry>> groups = new EnumMap<>(Category.class);
        for (Entry e : entries) {
            groups.computeIfAbsent(e.category, k -> new ArrayList<>()).add(e);
            amount.merge(e.category, e.stack.getCount(), Integer::sum);
        }

        for (List<Entry> group : groups.values()) group.sort(Comparator.comparingInt((Entry e) -> e.score).reversed());

        for (Category c : Category.values()) {
            int max = maxFor(c);
            List<Entry> group = groups.getOrDefault(c, List.of());
            if (max < 0 || group.isEmpty()) continue;

            int total = amount.getOrDefault(c, 0);
            if (c.oneIsEnough && total > 1) {
                // Keep the strongest item; extra weapons/tools/utility items are only
                // removed when the configured slot policy does not request them.
                for (int i = 1; i < group.size(); i++) {
                    if (isProtectedHotbar(mc, group.get(i).slot)) continue;
                    drop(mc, playerInventoryScreenSlot(group.get(i).slot));
                    cooldown = PeoClient.CFG.cleanerActionDelay;
                    return true;
                }
            }

            if (total > max) {
                // Remove from the weakest end until the quota is satisfied. One action
                // per tick avoids spamming click packets and mirrors human-like inventory UI flow.
                for (int i = group.size() - 1; i >= 0; i--) {
                    Entry e = group.get(i);
                    if (isProtectedHotbar(mc, e.slot)) continue;
                    drop(mc, playerInventoryScreenSlot(e.slot));
                    cooldown = PeoClient.CFG.cleanerActionDelay;
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean mergeOne(MinecraftClient mc, List<Entry> entries) {
        for (int a = 0; a < entries.size(); a++) {
            Entry x = entries.get(a);
            if (x.slot < 9) continue;
            for (int b = a + 1; b < entries.size(); b++) {
                Entry y = entries.get(b);
                if (y.slot < 9 || !ItemStack.areItemsAndComponentsEqual(x.stack, y.stack)) continue;
                int max = x.stack.getMaxCount();
                if (x.stack.getCount() >= max || y.stack.getCount() >= max) continue;
                int from = playerInventoryScreenSlot(y.slot);
                int to = playerInventoryScreenSlot(x.slot);
                pickupSwap(mc, from, to);
                cooldown = PeoClient.CFG.cleanerActionDelay + 1;
                return true;
            }
        }
        return false;
    }

    private static boolean sortHotbar(MinecraftClient mc, List<Entry> entries) {
        String[] targets = PeoClient.CFG.slotItems;
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            String target = targets[Math.min(hotbar, targets.length - 1)];
            if (target == null || target.equalsIgnoreCase("NONE") || target.equalsIgnoreCase("IGNORE")) continue;

            ItemStack current = mc.player.getInventory().getStack(hotbar);
            if (!current.isEmpty() && matches(target, current)) continue;

            Entry best = entries.stream()
                    .filter(e -> e.slot >= 9 && matches(target, e.stack))
                    .max(Comparator.comparingInt(e -> e.score))
                    .orElse(null);
            if (best == null) continue;

            swapWithHotbar(mc, best.slot, hotbar);
            cooldown = PeoClient.CFG.cleanerActionDelay + 1;
            return true;
        }
        return false;
    }

    private static int maxFor(Category c) {
        return switch (c) {
            case BLOCK -> PeoClient.CFG.maxBlocks;
            case ARROW -> PeoClient.CFG.maxArrows;
            case THROWABLE -> PeoClient.CFG.maxThrowables;
            case FOOD -> PeoClient.CFG.maxFoods;
            case WATER -> PeoClient.CFG.maxWaterBuckets;
            case LAVA -> PeoClient.CFG.maxLavaBuckets;
            case MILK -> PeoClient.CFG.maxMilkBuckets;
            case POTION -> PeoClient.CFG.maxPotions;
            case PEARL -> PeoClient.CFG.maxPearls;
            default -> Integer.MAX_VALUE;
        };
    }

    private static boolean isProtectedHotbar(MinecraftClient mc, int playerSlot) {
        return playerSlot < 9 && !PeoClient.CFG.cleanerTouchHotbar;
    }

    private static void drop(MinecraftClient mc, int screenSlot) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, screenSlot, 1,
                SlotActionType.THROW, mc.player);
    }

    private static void swapWithHotbar(MinecraftClient mc, int playerSlot, int hotbarSlot) {
        int screenSource = playerInventoryScreenSlot(playerSlot);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, screenSource, hotbarSlot,
                SlotActionType.SWAP, mc.player);
    }

    private static void pickupSwap(MinecraftClient mc, int from, int to) {
        int sync = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(sync, from, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(sync, to, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(sync, from, 0, SlotActionType.PICKUP, mc.player);
    }

    private static int playerInventoryScreenSlot(int playerSlot) {
        return playerSlot < 9 ? 36 + playerSlot : playerSlot;
    }

    private static String id(ItemStack s) {
        return Registries.ITEM.getId(s.getItem()).toString();
    }

    private static boolean matches(String target, ItemStack s) {
        return switch (target.toUpperCase(Locale.ROOT)) {
            case "SWORD" -> s.getItem() instanceof SwordItem;
            case "WEAPON" -> s.getItem() instanceof SwordItem || s.getItem() instanceof MaceItem || s.getItem() instanceof TridentItem;
            case "BOW" -> s.getItem() instanceof BowItem;
            case "CROSSBOW" -> s.getItem() instanceof CrossbowItem;
            case "AXE" -> s.getItem() instanceof AxeItem;
            case "PICKAXE" -> s.getItem() instanceof PickaxeItem;
            case "SHOVEL" -> s.getItem() instanceof ShovelItem;
            case "HOE" -> s.getItem() instanceof HoeItem;
            case "ROD" -> s.getItem() instanceof FishingRodItem;
            case "SHIELD" -> s.getItem() instanceof ShieldItem;
            case "WATER" -> s.isOf(Items.WATER_BUCKET);
            case "LAVA" -> s.isOf(Items.LAVA_BUCKET);
            case "MILK" -> s.isOf(Items.MILK_BUCKET);
            case "PEARL" -> s.isOf(Items.ENDER_PEARL);
            case "GAPPLE" -> s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE);
            case "POTION" -> s.getItem() instanceof PotionItem;
            case "FOOD" -> s.contains(DataComponentTypes.FOOD);
            case "BLOCK" -> s.getItem() instanceof BlockItem;
            case "THROWABLE", "THROWABLES" -> isThrowable(s);
            default -> false;
        };
    }

    private static boolean isThrowable(ItemStack s) {
        Item i = s.getItem();
        return i instanceof SnowballItem || i instanceof EggItem || i instanceof EnderPearlItem
                || i instanceof WindChargeItem || i instanceof ExperienceBottleItem;
    }

    private static Category classify(ItemStack s) {
        if (s.getItem() instanceof SwordItem) return Category.SWORD;
        if (s.getItem() instanceof MaceItem) return Category.MACE;
        if (s.getItem() instanceof TridentItem) return Category.WEAPON;
        if (s.getItem() instanceof BowItem) return Category.BOW;
        if (s.getItem() instanceof CrossbowItem) return Category.CROSSBOW;
        if (s.getItem() instanceof AxeItem) return Category.AXE;
        if (s.getItem() instanceof PickaxeItem) return Category.PICKAXE;
        if (s.getItem() instanceof ShovelItem) return Category.SHOVEL;
        if (s.getItem() instanceof HoeItem) return Category.HOE;
        if (s.getItem() instanceof FishingRodItem) return Category.ROD;
        if (s.getItem() instanceof ShieldItem) return Category.SHIELD;
        if (s.getItem() instanceof PotionItem) return Category.POTION;
        if (s.isOf(Items.ENDER_PEARL)) return Category.PEARL;
        if (s.isOf(Items.WATER_BUCKET)) return Category.WATER;
        if (s.isOf(Items.LAVA_BUCKET)) return Category.LAVA;
        if (s.isOf(Items.MILK_BUCKET)) return Category.MILK;
        if (s.contains(DataComponentTypes.FOOD)) return Category.FOOD;
        if (s.getItem() instanceof BlockItem) return Category.BLOCK;
        if (s.getItem() instanceof ArrowItem) return Category.ARROW;
        if (isThrowable(s)) return Category.THROWABLE;
        return Category.OTHER;
    }

    private static int score(ItemStack s) {
        int score = 0;
        Item i = s.getItem();
        if (i instanceof SwordItem) score += 100;
        if (i instanceof AxeItem) score += 90;
        if (i instanceof PickaxeItem) score += 80;
        if (i instanceof ShovelItem || i instanceof HoeItem) score += 70;
        if (i instanceof BowItem || i instanceof CrossbowItem) score += 80;
        if (i instanceof MaceItem || i instanceof TridentItem) score += 100;
        score += MathHelper.clamp(s.getMaxDamage() - s.getDamage(), 0, 200);
        score += s.getCount();
        return score;
    }

    private enum Category {
        SWORD(true), MACE(true), WEAPON(true), BOW(true), CROSSBOW(true), AXE(true), PICKAXE(true),
        SHOVEL(true), HOE(true), ROD(true), SHIELD(true), PEARL(false), POTION(false), FOOD(false),
        BLOCK(false), ARROW(false), THROWABLE(false), WATER(false), LAVA(false), MILK(false), OTHER(false);
        final boolean oneIsEnough;
        Category(boolean one) { this.oneIsEnough = one; }
    }

    private record Entry(int slot, ItemStack stack, Category category, int score) {}
}
