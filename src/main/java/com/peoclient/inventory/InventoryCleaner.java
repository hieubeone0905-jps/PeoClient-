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
 * Ported to PeoClient's Fabric/Java architecture from the behaviour model of
 * LiquidBounce's modern InventoryCleaner: hotbar/offhand placement first,
 * stack consolidation second, and disposal last, with item categories,
 * per-category quotas and blacklist support.
 *
 * This is intentionally kept as Java because PeoClient itself is Java-only.
 */
public final class InventoryCleaner {
    private InventoryCleaner() {}

    private static int cooldown;
    private static int pendingSlot = -1;
    private static int pendingScreenSlot = -1;
    private static int pendingWait;
    private static boolean pendingServerAck;

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;

        if (pendingSlot >= 0) {
            if (pendingServerAck || --pendingWait <= 0) {
                pendingSlot = -1;
                pendingScreenSlot = -1;
                pendingWait = 0;
                pendingServerAck = false;
            } else {
                return;
            }
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        List<Entry> entries = snapshot(mc);

        // LiquidBounce workflow: hotbar swaps -> stack merges -> disposal.
        if (sortOffhand(mc, entries)) return;
        if (sortHotbar(mc, entries)) return;
        if (PeoClient.CFG.cleanerMergeStacks && mergeOne(mc, entries)) return;
        if (disposeOne(mc, entries)) return;
    }

    private static List<Entry> snapshot(MinecraftClient mc) {
        List<Entry> entries = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty()) entries.add(new Entry(slot, stack.copy(), classify(stack), score(stack, slot)));
        }
        return entries;
    }

    private static boolean sortOffhand(MinecraftClient mc, List<Entry> entries) {
        String target = safe(PeoClient.CFG.offHandItem, "SHIELD");
        if (target.equalsIgnoreCase("NONE") || target.equalsIgnoreCase("IGNORE")) return false;

        ItemStack current = mc.player.getOffHandStack();
        if (!current.isEmpty() && matches(target, current)) return false;
        if (!PeoClient.CFG.cleanerGreedy && !current.isEmpty()) return false;

        Entry best = entries.stream()
                .filter(e -> e.slot >= 9 && matches(target, e.stack))
                .max(Comparator.comparingInt(Entry::score))
                .orElse(null);
        if (best == null) return false;

        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId,
                playerInventoryScreenSlot(best.slot), 40, SlotActionType.SWAP, mc.player);
        afterAction(1);
        return true;
    }

    private static boolean sortHotbar(MinecraftClient mc, List<Entry> entries) {
        String[] targets = PeoClient.CFG.slotItems == null ? new String[0] : PeoClient.CFG.slotItems;
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            String configuredTarget = hotbar < targets.length ? targets[hotbar] : "NONE";
            final String target = safe(configuredTarget, "NONE");
            if (target.equalsIgnoreCase("NONE") || target.equalsIgnoreCase("IGNORE")) continue;

            ItemStack current = mc.player.getInventory().getStack(hotbar);
            if (!current.isEmpty() && matches(target, current)) continue;
            if (!PeoClient.CFG.cleanerGreedy && !current.isEmpty()) continue;

            Entry best = entries.stream()
                    .filter(e -> e.slot >= 9 && matches(target, e.stack))
                    .max(Comparator.comparingInt(Entry::score))
                    .orElse(null);
            if (best == null) continue;

            swapWithHotbar(mc, best.slot, hotbar);
            afterAction(1);
            return true;
        }
        return false;
    }

    private static boolean mergeOne(MinecraftClient mc, List<Entry> entries) {
        Map<String, List<Entry>> groups = new LinkedHashMap<>();
        for (Entry e : entries) {
            if (e.slot < 9 || !e.stack.isStackable() || e.stack.getCount() >= e.stack.getMaxCount()) continue;
            groups.computeIfAbsent(id(e.stack), k -> new ArrayList<>()).add(e);
        }

        for (List<Entry> sameItem : groups.values()) {
            if (sameItem.size() < 2) continue;
            List<List<Entry>> exactGroups = new ArrayList<>();
            for (Entry e : sameItem) {
                List<Entry> exact = null;
                for (List<Entry> candidate : exactGroups) {
                    if (ItemStack.areItemsAndComponentsEqual(candidate.get(0).stack, e.stack)) {
                        exact = candidate;
                        break;
                    }
                }
                if (exact == null) {
                    exact = new ArrayList<>();
                    exactGroups.add(exact);
                }
                exact.add(e);
            }
            for (List<Entry> group : exactGroups) {
                if (group.size() < 2) continue;
            group.sort(Comparator.comparingInt(e -> e.stack.getCount()));
            int total = group.stream().mapToInt(e -> e.stack.getCount()).sum();
            int max = group.get(0).stack.getMaxCount();
            if ((int) Math.ceil(total / (double) max) >= group.size()) continue;

            Entry target = group.get(group.size() - 1);
            Entry source = group.get(0);
            if (source == target) continue;

                pickupMerge(mc, playerInventoryScreenSlot(source.slot), playerInventoryScreenSlot(target.slot));
                afterAction(2);
                return true;
            }
        }
        return false;
    }

    private static boolean disposeOne(MinecraftClient mc, List<Entry> entries) {
        Set<Integer> useful = computeUsefulSlots(mc, entries);

        // Explicit blacklist is always lowest-level and wins over quota decisions.
        for (Entry e : entries) {
            if (isBlacklisted(e.stack) && !isProtectedHotbar(mc, e.slot)) {
                drop(mc, e.slot);
                return true;
            }
        }

        // Dispose the least valuable item that is not needed by the target/limits.
        Entry worst = entries.stream()
                .filter(e -> !useful.contains(e.slot))
                .filter(e -> !isProtectedHotbar(mc, e.slot))
                .min(Comparator.comparingInt(Entry::score).thenComparingInt(Entry::slot))
                .orElse(null);
        if (worst == null) return false;

        drop(mc, worst.slot);
        return true;
    }

    private static Set<Integer> computeUsefulSlots(MinecraftClient mc, List<Entry> entries) {
        Set<Integer> useful = new HashSet<>();

        // 1) Reserve best matching candidates for explicit offhand/hotbar targets.
        String offhand = safe(PeoClient.CFG.offHandItem, "SHIELD");
        if (!offhand.equalsIgnoreCase("NONE") && !offhand.equalsIgnoreCase("IGNORE")) {
            Entry best = entries.stream().filter(e -> e.slot >= 9 && matches(offhand, e.stack))
                    .max(Comparator.comparingInt(Entry::score)).orElse(null);
            if (best != null) useful.add(best.slot);
        }
        String[] slots = PeoClient.CFG.slotItems == null ? new String[0] : PeoClient.CFG.slotItems;
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            String target = hotbar < slots.length ? safe(slots[hotbar], "NONE") : "NONE";
            if (target.equalsIgnoreCase("NONE") || target.equalsIgnoreCase("IGNORE")) continue;
            ItemStack current = mc.player.getInventory().getStack(hotbar);
            if (!current.isEmpty() && matches(target, current)) useful.add(hotbar);
            else {
                Entry best = entries.stream().filter(e -> e.slot >= 9 && matches(target, e.stack))
                        .max(Comparator.comparingInt(Entry::score)).orElse(null);
                if (best != null) useful.add(best.slot);
            }
        }

        // 2) Keep one best item for one-sufficient categories (the modern cleaner's
        // "one item is enough" classes) and quota-limited stacks for bulk classes.
        EnumMap<Category, Integer> totals = new EnumMap<>(Category.class);
        EnumMap<Category, List<Entry>> byCategory = new EnumMap<>(Category.class);
        for (Entry e : entries) {
            if (isBlacklisted(e.stack)) continue;
            byCategory.computeIfAbsent(e.category, k -> new ArrayList<>()).add(e);
            totals.merge(e.category, e.stack.getCount(), Integer::sum);
        }

        for (Category category : Category.values()) {
            List<Entry> group = byCategory.getOrDefault(category, List.of());
            if (group.isEmpty()) continue;
            group.sort(Comparator.comparingInt(Entry::score).reversed());

            if (category.oneIsEnough) {
                useful.add(group.get(0).slot);
            } else {
                int max = maxFor(category);
                if (max == Integer.MAX_VALUE) {
                    useful.addAll(group.stream().map(Entry::slot).toList());
                } else {
                    int kept = 0;
                    for (Entry e : group) {
                        if (kept >= max) break;
                        useful.add(e.slot);
                        kept += e.stack.getCount();
                    }
                }
            }
        }

        // 3) Hotbar items are never automatically deleted when TouchHotbar is off.
        if (!PeoClient.CFG.cleanerTouchHotbar) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = mc.player.getInventory().getStack(i);
                if (!s.isEmpty()) useful.add(i);
            }
        }
        return useful;
    }

    private static int maxFor(Category category) {
        return switch (category) {
            case BLOCK -> clamp(PeoClient.CFG.maxBlocks, 0, 2500);
            case ARROW -> clamp(PeoClient.CFG.maxArrows, 0, 2500);
            case THROWABLE -> clamp(PeoClient.CFG.maxThrowables, 0, 600);
            case FOOD -> clamp(PeoClient.CFG.maxFoods, 0, 2000);
            case WATER -> clamp(PeoClient.CFG.maxWaterBuckets, 0, 16);
            case LAVA -> clamp(PeoClient.CFG.maxLavaBuckets, 0, 16);
            case MILK -> clamp(PeoClient.CFG.maxMilkBuckets, 0, 16);
            case POTION, PEARL, GAPPLE -> 1;
            default -> Integer.MAX_VALUE;
        };
    }

    private static boolean isBlacklisted(ItemStack stack) {
        String itemId = id(stack);
        if (PeoClient.CFG.cleanerBlacklistSet != null && PeoClient.CFG.cleanerBlacklistSet.contains(itemId)) return true;
        String raw = PeoClient.CFG.itemsBlacklist == null ? "" : PeoClient.CFG.itemsBlacklist;
        for (String token : raw.split("[,\\n\\s]+")) {
            if (!token.isBlank() && token.trim().equals(itemId)) return true;
        }
        return false;
    }

    private static boolean isProtectedHotbar(MinecraftClient mc, int slot) {
        return slot < 9 && !PeoClient.CFG.cleanerTouchHotbar;
    }

    private static void drop(MinecraftClient mc, int playerSlot) {
        int screenSlot = playerInventoryScreenSlot(playerSlot);
        pendingSlot = playerSlot;
        pendingScreenSlot = screenSlot;
        pendingWait = Math.max(4, PeoClient.CFG.cleanerAckTimeout);
        pendingServerAck = false;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, screenSlot, 1,
                SlotActionType.THROW, mc.player);
        afterAction(0);
    }

    public static void onServerSlotUpdate(int syncId, int revision, int screenSlot, ItemStack serverStack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.currentScreenHandler == null) return;
        if (pendingSlot < 0 || syncId != mc.player.currentScreenHandler.syncId || screenSlot != pendingScreenSlot) return;
        if (serverStack.isEmpty()) pendingServerAck = true;
    }

    private static void swapWithHotbar(MinecraftClient mc, int playerSlot, int hotbarSlot) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId,
                playerInventoryScreenSlot(playerSlot), hotbarSlot, SlotActionType.SWAP, mc.player);
    }

    private static void pickupMerge(MinecraftClient mc, int source, int target) {
        int sync = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(sync, source, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(sync, target, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(sync, source, 0, SlotActionType.PICKUP, mc.player);
    }

    private static int playerInventoryScreenSlot(int playerSlot) {
        return playerSlot < 9 ? 36 + playerSlot : playerSlot;
    }

    private static void afterAction(int extraTicks) {
        cooldown = Math.max(cooldown, PeoClient.CFG.cleanerActionDelay + extraTicks);
    }

    private static String id(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    private static boolean matches(String target, ItemStack s) {
        return switch (target.toUpperCase(Locale.ROOT)) {
            case "SWORD" -> s.getItem() instanceof SwordItem;
            case "WEAPON" -> s.getItem() instanceof SwordItem || s.getItem() instanceof MaceItem
                    || s.getItem() instanceof TridentItem;
            case "SPEAR" -> isSpear(s);
            case "MACE" -> s.getItem() instanceof MaceItem;
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

    private static boolean isSpear(ItemStack stack) {
        // 1.21.4 has no stable common spear base in the Yarn names used by this
        // project. Keep the hook narrow and let Weapon matching cover generic weapons.
        String id = id(stack);
        return id.contains("spear") || id.contains("trident");
    }

    private static boolean isThrowable(ItemStack s) {
        Item i = s.getItem();
        return i instanceof SnowballItem || i instanceof EggItem || i instanceof EnderPearlItem
                || i instanceof WindChargeItem || i instanceof ExperienceBottleItem;
    }

    private static Category classify(ItemStack s) {
        Item i = s.getItem();
        if (i instanceof SwordItem) return Category.SWORD;
        if (i instanceof MaceItem) return Category.MACE;
        if (i instanceof TridentItem) return Category.WEAPON;
        if (i instanceof BowItem) return Category.BOW;
        if (i instanceof CrossbowItem) return Category.CROSSBOW;
        if (i instanceof AxeItem) return Category.AXE;
        if (i instanceof PickaxeItem) return Category.PICKAXE;
        if (i instanceof ShovelItem) return Category.SHOVEL;
        if (i instanceof HoeItem) return Category.HOE;
        if (i instanceof FishingRodItem) return Category.ROD;
        if (i instanceof ShieldItem) return Category.SHIELD;
        if (i instanceof PotionItem) return Category.POTION;
        if (s.isOf(Items.ENDER_PEARL)) return Category.PEARL;
        if (s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE)) return Category.GAPPLE;
        if (s.isOf(Items.WATER_BUCKET)) return Category.WATER;
        if (s.isOf(Items.LAVA_BUCKET)) return Category.LAVA;
        if (s.isOf(Items.MILK_BUCKET)) return Category.MILK;
        if (s.contains(DataComponentTypes.FOOD)) return Category.FOOD;
        if (i instanceof BlockItem) return Category.BLOCK;
        if (i instanceof ArrowItem) return Category.ARROW;
        if (isThrowable(s)) return Category.THROWABLE;
        return Category.OTHER;
    }

    /** Deterministic approximation of LiquidBounce's facet comparators. */
    private static int score(ItemStack s, int slot) {
        int score = 0;
        Item i = s.getItem();
        if (i instanceof SwordItem) score += 1000;
        if (i instanceof MaceItem) score += 1100;
        if (i instanceof TridentItem) score += 1050;
        if (i instanceof AxeItem) score += 900;
        if (i instanceof PickaxeItem) score += 800;
        if (i instanceof ShovelItem || i instanceof HoeItem) score += 700;
        if (i instanceof BowItem || i instanceof CrossbowItem) score += 850;
        if (i instanceof ShieldItem) score += 600;
        if (i instanceof FishingRodItem) score += 500;
        if (s.contains(DataComponentTypes.FOOD)) score += 400;

        if (s.getMaxDamage() > 0) {
            score += MathHelper.clamp(s.getMaxDamage() - s.getDamage(), 0, 500);
        }
        score += Math.min(200, s.getCount());

        // Stable preference for hotbar items, matching LB's comparator chain.
        if (slot < 9) score += 100;
        return score;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Category {
        SWORD(true), MACE(true), WEAPON(true), BOW(true), CROSSBOW(true), AXE(true), PICKAXE(true),
        SHOVEL(true), HOE(true), ROD(true), SHIELD(true), PEARL(false), GAPPLE(false), POTION(false),
        FOOD(false), BLOCK(false), ARROW(false), THROWABLE(false), WATER(false), LAVA(false), MILK(false), OTHER(false);
        final boolean oneIsEnough;
        Category(boolean oneIsEnough) { this.oneIsEnough = oneIsEnough; }
    }

    private record Entry(int slot, ItemStack stack, Category category, int score) {}
}
