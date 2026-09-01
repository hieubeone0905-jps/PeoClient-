package com.peoclient.inventory;

import com.peoclient.PeoClient;
import com.peoclient.modules.AntiPeoModule;
import net.minecraft.class_1713;
import net.minecraft.class_1743;
import net.minecraft.class_1744;
import net.minecraft.class_1747;
import net.minecraft.class_1753;
import net.minecraft.class_1764;
import net.minecraft.class_1771;
import net.minecraft.class_1776;
import net.minecraft.class_1779;
import net.minecraft.class_1787;
import net.minecraft.class_1792;
import net.minecraft.class_1794;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1810;
import net.minecraft.class_1812;
import net.minecraft.class_1819;
import net.minecraft.class_1821;
import net.minecraft.class_1823;
import net.minecraft.class_1829;
import net.minecraft.class_1835;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_7923;
import net.minecraft.class_9239;
import net.minecraft.class_9334;
import net.minecraft.class_9362;
import net.minecraft.item.*;
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

    public static void tick(class_310 mc) {
        if (mc.field_1724 == null || mc.field_1761 == null || mc.field_1755 != null) return;

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

        if (!AntiPeoModule.canAct()) return;

        List<Entry> entries = snapshot(mc);

        // LiquidBounce workflow: hotbar swaps -> stack merges -> disposal.
        if (sortOffhand(mc, entries)) return;
        if (sortHotbar(mc, entries)) return;
        if (PeoClient.CFG.cleanerMergeStacks && mergeOne(mc, entries)) return;
        if (disposeOne(mc, entries)) return;
    }

    private static List<Entry> snapshot(class_310 mc) {
        List<Entry> entries = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            class_1799 stack = mc.field_1724.method_31548().method_5438(slot);
            if (!stack.method_7960()) entries.add(new Entry(slot, stack.method_7972(), classify(stack), score(stack, slot)));
        }
        return entries;
    }

    private static boolean sortOffhand(class_310 mc, List<Entry> entries) {
        String target = safe(PeoClient.CFG.offHandItem, "SHIELD");
        if (target.equalsIgnoreCase("NONE") || target.equalsIgnoreCase("IGNORE")) return false;

        class_1799 current = mc.field_1724.method_6079();
        if (!current.method_7960() && matches(target, current)) return false;
        if (!PeoClient.CFG.cleanerGreedy && !current.method_7960()) return false;

        Entry best = entries.stream()
                .filter(e -> e.slot >= 9 && matches(target, e.stack))
                .max(Comparator.comparingInt(Entry::score))
                .orElse(null);
        if (best == null) return false;

        mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763,
                playerInventoryScreenSlot(best.slot), 40, class_1713.field_7791, mc.field_1724);
        afterAction(1);
        return true;
    }

    private static boolean sortHotbar(class_310 mc, List<Entry> entries) {
        String[] targets = PeoClient.CFG.slotItems == null ? new String[0] : PeoClient.CFG.slotItems;
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            String configuredTarget = hotbar < targets.length ? targets[hotbar] : "NONE";
            final String target = safe(configuredTarget, "NONE");
            if (target.equalsIgnoreCase("NONE") || target.equalsIgnoreCase("IGNORE")) continue;

            class_1799 current = mc.field_1724.method_31548().method_5438(hotbar);
            if (!current.method_7960() && matches(target, current)) continue;
            if (!PeoClient.CFG.cleanerGreedy && !current.method_7960()) continue;

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

    private static boolean mergeOne(class_310 mc, List<Entry> entries) {
        Map<String, List<Entry>> groups = new LinkedHashMap<>();
        for (Entry e : entries) {
            if (e.slot < 9 || !e.stack.method_7946() || e.stack.method_7947() >= e.stack.method_7914()) continue;
            groups.computeIfAbsent(id(e.stack), k -> new ArrayList<>()).add(e);
        }

        for (List<Entry> sameItem : groups.values()) {
            if (sameItem.size() < 2) continue;
            List<List<Entry>> exactGroups = new ArrayList<>();
            for (Entry e : sameItem) {
                List<Entry> exact = null;
                for (List<Entry> candidate : exactGroups) {
                    if (class_1799.method_31577(candidate.get(0).stack, e.stack)) {
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
            group.sort(Comparator.comparingInt(e -> e.stack.method_7947()));
            int total = group.stream().mapToInt(e -> e.stack.method_7947()).sum();
            int max = group.get(0).stack.method_7914();
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

    private static boolean disposeOne(class_310 mc, List<Entry> entries) {
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

    private static Set<Integer> computeUsefulSlots(class_310 mc, List<Entry> entries) {
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
            class_1799 current = mc.field_1724.method_31548().method_5438(hotbar);
            if (!current.method_7960() && matches(target, current)) useful.add(hotbar);
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
            totals.merge(e.category, e.stack.method_7947(), Integer::sum);
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
                        kept += e.stack.method_7947();
                    }
                }
            }
        }

        // 3) Hotbar items are never automatically deleted when TouchHotbar is off.
        if (!PeoClient.CFG.cleanerTouchHotbar) {
            for (int i = 0; i < 9; i++) {
                class_1799 s = mc.field_1724.method_31548().method_5438(i);
                if (!s.method_7960()) useful.add(i);
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

    private static boolean isBlacklisted(class_1799 stack) {
        String itemId = id(stack);
        if (PeoClient.CFG.cleanerBlacklistSet != null && PeoClient.CFG.cleanerBlacklistSet.contains(itemId)) return true;
        String raw = PeoClient.CFG.itemsBlacklist == null ? "" : PeoClient.CFG.itemsBlacklist;
        for (String token : raw.split("[,\\n\\s]+")) {
            if (!token.isBlank() && token.trim().equals(itemId)) return true;
        }
        return false;
    }

    private static boolean isProtectedHotbar(class_310 mc, int slot) {
        return slot < 9 && !PeoClient.CFG.cleanerTouchHotbar;
    }

    private static void drop(class_310 mc, int playerSlot) {
        int screenSlot = playerInventoryScreenSlot(playerSlot);
        pendingSlot = playerSlot;
        pendingScreenSlot = screenSlot;
        pendingWait = Math.max(4, PeoClient.CFG.cleanerAckTimeout);
        pendingServerAck = false;
        mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763, screenSlot, 1,
                class_1713.field_7795, mc.field_1724);
        afterAction(0);
    }

    public static void onServerSlotUpdate(int syncId, int screenSlot, class_1799 serverStack, int revision) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1724.field_7512 == null) return;
        if (pendingSlot < 0 || syncId != mc.field_1724.field_7512.field_7763 || screenSlot != pendingScreenSlot) return;
        if (serverStack.method_7960()) pendingServerAck = true;
    }

    private static void swapWithHotbar(class_310 mc, int playerSlot, int hotbarSlot) {
        mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763,
                playerInventoryScreenSlot(playerSlot), hotbarSlot, class_1713.field_7791, mc.field_1724);
    }

    private static void pickupMerge(class_310 mc, int source, int target) {
        int sync = mc.field_1724.field_7512.field_7763;
        mc.field_1761.method_2906(sync, source, 0, class_1713.field_7790, mc.field_1724);
        mc.field_1761.method_2906(sync, target, 0, class_1713.field_7790, mc.field_1724);
        mc.field_1761.method_2906(sync, source, 0, class_1713.field_7790, mc.field_1724);
    }

    private static int playerInventoryScreenSlot(int playerSlot) {
        return playerSlot < 9 ? 36 + playerSlot : playerSlot;
    }

    private static void afterAction(int extraTicks) {
        cooldown = Math.max(cooldown, PeoClient.CFG.cleanerActionDelay + extraTicks);
        AntiPeoModule.onAction();
    }

    private static String id(class_1799 stack) {
        return class_7923.field_41178.method_10221(stack.method_7909()).toString();
    }

    private static boolean matches(String target, class_1799 s) {
        return switch (target.toUpperCase(Locale.ROOT)) {
            case "SWORD" -> s.method_7909() instanceof class_1829;
            case "WEAPON" -> s.method_7909() instanceof class_1829 || s.method_7909() instanceof class_9362
                    || s.method_7909() instanceof class_1835;
            case "SPEAR" -> isSpear(s);
            case "MACE" -> s.method_7909() instanceof class_9362;
            case "BOW" -> s.method_7909() instanceof class_1753;
            case "CROSSBOW" -> s.method_7909() instanceof class_1764;
            case "AXE" -> s.method_7909() instanceof class_1743;
            case "PICKAXE" -> s.method_7909() instanceof class_1810;
            case "SHOVEL" -> s.method_7909() instanceof class_1821;
            case "HOE" -> s.method_7909() instanceof class_1794;
            case "ROD" -> s.method_7909() instanceof class_1787;
            case "SHIELD" -> s.method_7909() instanceof class_1819;
            case "WATER" -> s.method_31574(class_1802.field_8705);
            case "LAVA" -> s.method_31574(class_1802.field_8187);
            case "MILK" -> s.method_31574(class_1802.field_8103);
            case "PEARL" -> s.method_31574(class_1802.field_8634);
            case "GAPPLE" -> s.method_31574(class_1802.field_8463) || s.method_31574(class_1802.field_8367);
            case "POTION" -> s.method_7909() instanceof class_1812;
            case "FOOD" -> s.method_57826(class_9334.field_50075);
            case "BLOCK" -> s.method_7909() instanceof class_1747;
            case "THROWABLE", "THROWABLES" -> isThrowable(s);
            default -> false;
        };
    }

    private static boolean isSpear(class_1799 stack) {
        // 1.21.4 has no stable common spear base in the Yarn names used by this
        // project. Keep the hook narrow and let Weapon matching cover generic weapons.
        String id = id(stack);
        return id.contains("spear") || id.contains("trident");
    }

    private static boolean isThrowable(class_1799 s) {
        class_1792 i = s.method_7909();
        return i instanceof class_1823 || i instanceof class_1771 || i instanceof class_1776
                || i instanceof class_9239 || i instanceof class_1779;
    }

    private static Category classify(class_1799 s) {
        class_1792 i = s.method_7909();
        if (i instanceof class_1829) return Category.SWORD;
        if (i instanceof class_9362) return Category.MACE;
        if (i instanceof class_1835) return Category.WEAPON;
        if (i instanceof class_1753) return Category.BOW;
        if (i instanceof class_1764) return Category.CROSSBOW;
        if (i instanceof class_1743) return Category.AXE;
        if (i instanceof class_1810) return Category.PICKAXE;
        if (i instanceof class_1821) return Category.SHOVEL;
        if (i instanceof class_1794) return Category.HOE;
        if (i instanceof class_1787) return Category.ROD;
        if (i instanceof class_1819) return Category.SHIELD;
        if (i instanceof class_1812) return Category.POTION;
        if (s.method_31574(class_1802.field_8634)) return Category.PEARL;
        if (s.method_31574(class_1802.field_8463) || s.method_31574(class_1802.field_8367)) return Category.GAPPLE;
        if (s.method_31574(class_1802.field_8705)) return Category.WATER;
        if (s.method_31574(class_1802.field_8187)) return Category.LAVA;
        if (s.method_31574(class_1802.field_8103)) return Category.MILK;
        if (s.method_57826(class_9334.field_50075)) return Category.FOOD;
        if (i instanceof class_1747) return Category.BLOCK;
        if (i instanceof class_1744) return Category.ARROW;
        if (isThrowable(s)) return Category.THROWABLE;
        return Category.OTHER;
    }

    /** Deterministic approximation of LiquidBounce's facet comparators. */
    private static int score(class_1799 s, int slot) {
        int score = 0;
        class_1792 i = s.method_7909();
        if (i instanceof class_1829) score += 1000;
        if (i instanceof class_9362) score += 1100;
        if (i instanceof class_1835) score += 1050;
        if (i instanceof class_1743) score += 900;
        if (i instanceof class_1810) score += 800;
        if (i instanceof class_1821 || i instanceof class_1794) score += 700;
        if (i instanceof class_1753 || i instanceof class_1764) score += 850;
        if (i instanceof class_1819) score += 600;
        if (i instanceof class_1787) score += 500;
        if (s.method_57826(class_9334.field_50075)) score += 400;

        if (s.method_7936() > 0) {
            score += class_3532.method_15340(s.method_7936() - s.method_7919(), 0, 500);
        }
        score += Math.min(200, s.method_7947());

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

    private record Entry(int slot, class_1799 stack, Category category, int score) {}
}
