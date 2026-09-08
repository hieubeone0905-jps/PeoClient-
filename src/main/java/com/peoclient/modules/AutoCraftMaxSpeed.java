package com.peoclient.modules;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.minecraft.class_1268;
import net.minecraft.class_1713;
import net.minecraft.class_1714;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_7923;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AutoCraftMaxSpeed
 *
 * Server-GUI workflow:
 *  1) /craft
 *  2) craft the configured mineral blocks from 3x3 ingredients
 *  3) concurrently throw the configured low-value blocks
 *  4) once the rare-block stack threshold is reached, close crafting
 *  5) hold a rare block and right-click to open the server combine GUI
 *  6) locate and click the hopper/action slot
 *  7) close the GUI and repeat
 *
 * All GUI manipulation is performed through the normal ScreenHandler slot-click
 * path. No client-side inventory is mutated directly.
 */
public final class AutoCraftMaxSpeed {
    private static final class_310 MC = class_310.method_1551();

    private static final int MIN_SPEED = 1;
    private static final int MAX_SPEED = 36;
    private static final int DEFAULT_CRAFT_SPEED = 1;
    private static final int DEFAULT_DROP_SPEED = 1;
    private static final int DEFAULT_THRESHOLD = 10;

    private static final Set<String> RARE_BLOCKS = Set.of(
            "minecraft:coal_block",
            "minecraft:redstone_block",
            "minecraft:lapis_block",
            "minecraft:gold_block",
            "minecraft:iron_block",
            "minecraft:emerald_block",
            "minecraft:diamond_block"
    );

    private static final Set<String> DROP_BLOCKS = Set.of(
            "minecraft:cobblestone",
            "minecraft:stone",
            "minecraft:raw_gold_block",
            "minecraft:raw_iron_block"
    );

    /** ingredient -> output block */
    private static final List<Recipe> RECIPES = List.of(
            new Recipe("minecraft:coal", "minecraft:coal_block"),
            new Recipe("minecraft:redstone", "minecraft:redstone_block"),
            new Recipe("minecraft:lapis_lazuli", "minecraft:lapis_block"),
            new Recipe("minecraft:raw_gold", "minecraft:raw_gold_block"),
            new Recipe("minecraft:gold_ingot", "minecraft:gold_block"),
            new Recipe("minecraft:raw_iron", "minecraft:raw_iron_block"),
            new Recipe("minecraft:iron_ingot", "minecraft:iron_block"),
            new Recipe("minecraft:emerald", "minecraft:emerald_block"),
            new Recipe("minecraft:diamond", "minecraft:diamond_block")
    );

    private static final int OPEN_WAIT = 3;
    private static final int ACTION_RETRY_WAIT = 1;
    private static final int SUBMIT_WAIT = 6;
    private static final int POST_CLOSE_WAIT = 8;
    private static final int MAX_FILL_ACTIONS_PER_TICK = 18;

    private static boolean enabled;
    private static boolean initialized;
    private static State state = State.IDLE;
    private static int craftSpeed = DEFAULT_CRAFT_SPEED;
    private static int dropSpeed = DEFAULT_DROP_SPEED;
    private static int threshold = DEFAULT_THRESHOLD;
    private static int waitTicks;
    private static int actionBudget;
    private static int lastSyncId = -1;
    private static int recipeIndex;
    private static String currentIngredient;
    private static int selectedHotbar = -1;
    private static int workingHotbar = -1;
    private static int postCloseTicks;

    private enum State {
        IDLE,
        OPEN_CRAFT,
        CRAFTING,
        SUBMIT_PREPARE,
        SUBMIT_RIGHT_CLICK,
        WAIT_COMBINE_GUI,
        CLICK_HOPPER,
        WAIT_SUBMIT,
        CLOSE_COMBINE,
        POST_CLOSE
    }

    private record Recipe(String ingredient, String output) {}

    private AutoCraftMaxSpeed() {}

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        PeoClient.CFG.autoCraftMaxSpeed = value;
        PeoClient.CFG.save();
        if (!value) {
            reset();
            log("Disabled");
            return;
        }
        state = State.IDLE;
        waitTicks = 0;
        actionBudget = 0;
        recipeIndex = 0;
        currentIngredient = null;
        log("Enabled craftSpeed=" + craftSpeed + " dropSpeed=" + dropSpeed + " threshold=" + threshold);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void tick(class_310 client) {
        if (!initialized) {
            enabled = PeoClient.CFG.autoCraftMaxSpeed;
            craftSpeed = clamp(PeoClient.CFG.autoCraftMaxSpeedCraftSpeed);
            dropSpeed = clamp(PeoClient.CFG.autoCraftMaxSpeedDropSpeed);
            threshold = clamp(PeoClient.CFG.autoCraftMaxSpeedThreshold);
            initialized = true;
        }
        if (!enabled || client.field_1724 == null || client.field_1761 == null) return;

        if (waitTicks > 0) {
            waitTicks--;
            if (state == State.CRAFTING && isCraftingScreen(client)) {
                // Drop is intentionally independent of the crafting state.
                dropConfiguredBlocks(client, dropSpeed);
            }
            return;
        }

        if (state == State.SUBMIT_RIGHT_CLICK && !isCraftingScreen(client)) {
            rightClickRareBlock(client);
            return;
        }
        if (state == State.OPEN_CRAFT && !isCraftingScreen(client)) {
            if (++waitTicks <= 12) return;
            state = State.IDLE;
        }
        if (state == State.WAIT_COMBINE_GUI && !hasCombineAction(client)) {
            if (++waitTicks <= 12) return;
            restoreHotbar(client);
            state = State.IDLE;
            return;
        }

        if (isCraftingScreen(client)) {
            if (state == State.SUBMIT_PREPARE) {
                client.field_1724.method_7346();
                waitTicks = 2;
                return;
            }
            if (state == State.OPEN_CRAFT || state == State.IDLE || state == State.CRAFTING) {
                state = State.CRAFTING;
                runCrafting(client);
                dropConfiguredBlocks(client, dropSpeed);
                return;
            }
            if (state == State.SUBMIT_RIGHT_CLICK
                    || state == State.WAIT_COMBINE_GUI || state == State.CLICK_HOPPER
                    || state == State.WAIT_SUBMIT || state == State.CLOSE_COMBINE) {
                // If the server returned to crafting unexpectedly, restart cleanly.
                state = State.CRAFTING;
                return;
            }
        }

        if (hasCombineAction(client)) {
            handleCombineGui(client);
            return;
        }

        if (state == State.OPEN_CRAFT) {
            if (++waitTicks <= 12) return;
            state = State.IDLE;
        }

        if (state == State.POST_CLOSE) {
            if (--postCloseTicks <= 0) {
                state = State.IDLE;
            }
            return;
        }

        if (countRareStacks(client) >= threshold) {
            prepareSubmission(client);
            return;
        }

        if (state == State.IDLE) {
            openCraft(client);
        }
    }

    private static void openCraft(class_310 client) {
        if (client.method_1562() == null) return;
        client.method_1562().method_45730("craft");
        state = State.OPEN_CRAFT;
        waitTicks = OPEN_WAIT;
        log("OPEN /craft");
    }

    private static void runCrafting(class_310 client) {
        if (!(client.field_1724.field_7512 instanceof class_1714)) return;

        int rare = countRareStacks(client);
        if (rare >= threshold) {
            state = State.SUBMIT_PREPARE;
            return;
        }

        int actions = 0;
        // Rotate through all nine requested recipes. A recipe is skipped if no
        // ingredient remains. This avoids spending ticks on empty recipes.
        for (int tries = 0; tries < RECIPES.size() && actions < craftSpeed; tries++) {
            Recipe recipe = RECIPES.get(recipeIndex);
            recipeIndex = (recipeIndex + 1) % RECIPES.size();
            int made = craftRecipeBatch(client, recipe, Math.min(craftSpeed - actions, MAX_FILL_ACTIONS_PER_TICK));
            actions += made;
        }

        if (actions == 0) {
            // Nothing craftable in the current inventory. Keep the GUI open so
            // newly arriving resources can be processed without reopening it.
            waitTicks = ACTION_RETRY_WAIT;
        }
    }

    /**
     * Fill the 3x3 input with the requested ingredient, then quick-move the
     * result. The slot ids are discovered from the CraftingScreenHandler itself,
     * rather than guessed from screen coordinates.
     */
    private static int craftRecipeBatch(class_310 client, Recipe recipe, int budget) {
        class_1714 handler = (class_1714) client.field_1724.field_7512;
        int filled = fillCraftingGrid(client, handler, recipe.ingredient, budget);
        if (filled > 0) return filled;

        if (!gridReady(handler, recipe.ingredient)) return 0;

        class_1735 output = handler.method_61627();
        if (output == null || output.method_7677().method_7960()) return 0;

        int crafted = Math.min(budget, Math.max(1, craftSpeed));
        int actions = 0;
        int sync = handler.field_7763;
        for (int i = 0; i < crafted; i++) {
            if (handler.method_7611(output.field_7874).method_7677().method_7960()) break;
            client.field_1761.method_2906(sync, output.field_7874, 0, class_1713.field_7791, client.field_1724);
            actions++;
        }
        if (actions > 0) log("CRAFT ingredient=" + recipe.ingredient + " output=" + recipe.output + " actions=" + actions);
        return actions;
    }

    private static int fillCraftingGrid(class_310 client, class_1714 handler, String ingredient, int budget) {
        int actions = 0;
        List<class_1735> inputs = handler.method_61628();

        // First use complete inventory stacks. Shift-clicking each stack into
        // the crafting handler is equivalent to a normal quick-move and avoids
        // cursor manipulation for the common case.
        for (int slot = 0; slot < handler.field_7761.size() && actions < budget; slot++) {
            class_1735 screenSlot = handler.method_7611(slot);
            if (screenSlot == null || screenSlot.field_7871 != client.field_1724.method_31548()) continue;
            if (screenSlot.method_7677().method_7960()) continue;
            if (!ingredient.equals(itemId(screenSlot.method_7677()))) continue;
            if (isInventorySourceSlot(screenSlot)) {
                int emptyInput = firstEmptyInput(inputs);
                if (emptyInput < 0) break;
                client.field_1761.method_2906(handler.field_7763, screenSlot.field_7874, 0,
                        class_1713.field_7791, client.field_1724);
                actions++;
                // Re-evaluate on the next tick/action; server sync is authoritative.
                if (actions >= budget) break;
                if (gridReady(handler, ingredient)) break;
            }
        }

        // If fewer than nine source stacks exist, use right-click splitting from
        // one remaining stack to put one ingredient in each empty input slot.
        if (actions < budget && !gridReady(handler, ingredient)) {
            int source = findIngredientScreenSlot(handler, client, ingredient);
            int empty = firstEmptyInput(inputs);
            if (source >= 0 && empty >= 0 && actions + 2 <= budget) {
                client.field_1761.method_2906(handler.field_7763, source, 1,
                        class_1713.field_7790, client.field_1724);
                actions++;
                for (class_1735 input : inputs) {
                    if (!input.method_7677().method_7960()) continue;
                    client.field_1761.method_2906(handler.field_7763, input.field_7874, 1,
                            class_1713.field_7790, client.field_1724);
                    actions++;
                    if (actions >= budget) break;
                }
                // Return any remaining cursor stack to the original source slot.
                client.field_1761.method_2906(handler.field_7763, source, 0,
                        class_1713.field_7790, client.field_1724);
                actions++;
            }
        }
        return actions;
    }

    private static boolean gridReady(class_1714 handler, String ingredient) {
        for (class_1735 input : handler.method_61628()) {
            if (input == null || input.method_7677().method_7960()
                    || !ingredient.equals(itemId(input.method_7677()))) return false;
        }
        return true;
    }

    private static int firstEmptyInput(List<class_1735> inputs) {
        for (class_1735 slot : inputs) {
            if (slot.method_7677().method_7960()) return slot.field_7874;
        }
        return -1;
    }

    private static int findIngredientScreenSlot(class_1714 handler, class_310 client, String ingredient) {
        for (class_1735 slot : handler.field_7761) {
            if (slot == null || slot.field_7871 != client.field_1724.method_31548()) continue;
            if (!slot.method_7677().method_7960() && ingredient.equals(itemId(slot.method_7677()))) {
                return slot.field_7874;
            }
        }
        return -1;
    }

    private static boolean isInventorySourceSlot(class_1735 slot) {
        return slot.field_7874 >= 10;
    }

    private static void dropConfiguredBlocks(class_310 client, int maxActions) {
        if (!(client.field_1724.field_7512 instanceof class_1714 handler)) return;
        int actions = 0;
        for (class_1735 slot : handler.field_7761) {
            if (actions >= maxActions) break;
            if (slot == null || slot.field_7871 != client.field_1724.method_31548()) continue;
            class_1799 stack = slot.method_7677();
            if (stack.method_7960() || !DROP_BLOCKS.contains(itemId(stack))) continue;
            client.field_1761.method_2906(handler.field_7763, slot.field_7874, 1,
                    class_1713.field_7795, client.field_1724);
            actions++;
        }
    }

    private static int countRareStacks(class_310 client) {
        int count = 0;
        var inv = client.field_1724.method_31548();
        for (int i = 0; i < 36; i++) {
            class_1799 stack = inv.method_5438(i);
            if (!stack.method_7960() && RARE_BLOCKS.contains(itemId(stack))) count++;
        }
        return count;
    }

    private static void prepareSubmission(class_310 client) {
        if (client.field_1755 != null) {
            client.field_1724.method_7346();
            waitTicks = 2;
            state = State.SUBMIT_PREPARE;
            return;
        }

        int slot = findRareHotbar(client);
        if (slot < 0) {
            int invSlot = findRareInventory(client);
            if (invSlot >= 0) {
                int target = findFreeHotbar(client);
                if (target < 0) target = 8;
                if (target >= 0) {
                    // PlayerScreenHandler mapping: main inventory 9..35 -> same
                    // screen id, hotbar 0..8 -> 36..44.
                    int sourceScreen = invSlot < 9 ? 36 + invSlot : invSlot;
                    client.field_1761.method_2906(client.field_1724.field_7512.field_7763,
                            sourceScreen, target, class_1713.field_7791, client.field_1724);
                    slot = target;
                }
            }
        }
        if (slot < 0) return;

        selectedHotbar = client.field_1724.method_31548().field_7545;
        workingHotbar = slot;
        client.field_1724.method_31548().method_61496(slot);
        state = State.SUBMIT_RIGHT_CLICK;
        waitTicks = 2;
        log("SUBMIT select hotbar=" + slot);
    }

    private static void rightClickRareBlock(class_310 client) {
        if (workingHotbar < 0 || workingHotbar >= 9) {
            state = State.IDLE;
            return;
        }
        class_1799 held = client.field_1724.method_31548().method_5438(workingHotbar);
        if (held.method_7960() || !RARE_BLOCKS.contains(itemId(held))) {
            state = State.IDLE;
            return;
        }
        client.field_1761.method_2919(client.field_1724, class_1268.field_5808);
        state = State.WAIT_COMBINE_GUI;
        waitTicks = 2;
        log("SUBMIT right-click block=" + itemId(held));
    }

    private static void handleCombineGui(class_310 client) {
        var handler = client.field_1724.field_7512;
        if (state == State.SUBMIT_RIGHT_CLICK) {
            rightClickRareBlock(client);
            return;
        }

        int sync = handler.field_7763;
        if (lastSyncId != sync) {
            lastSyncId = sync;
            state = State.CLICK_HOPPER;
        }

        if (state == State.WAIT_COMBINE_GUI || state == State.CLICK_HOPPER) {
            int hopper = findHopperActionSlot(handler);
            if (hopper < 0) {
                if (++waitTicks <= 12) return;
                state = State.IDLE;
                return;
            }
            client.field_1761.method_2906(sync, hopper, 0, class_1713.field_7790, client.field_1724);
            state = State.WAIT_SUBMIT;
            waitTicks = SUBMIT_WAIT;
            log("SUBMIT hopperSlot=" + hopper + " syncId=" + sync);
            return;
        }

        if (state == State.WAIT_SUBMIT) {
            if (waitTicks > 0) return;
            state = State.CLOSE_COMBINE;
        }

        if (state == State.CLOSE_COMBINE) {
            try {
                client.field_1724.method_7346();
            } catch (Throwable ignored) {
                client.method_1507(null);
            }
            restoreHotbar(client);
            postCloseTicks = POST_CLOSE_WAIT;
            state = State.POST_CLOSE;
            log("SUBMIT close/update");
        }
    }

    private static boolean hasCombineAction(class_310 client) {
        if (client.field_1724 == null || client.field_1724.field_7512 == null || isCraftingScreen(client)) return false;
        if (!(client.field_1755 instanceof net.minecraft.class_465)) return false;
        return findHopperActionSlot(client.field_1724.field_7512) >= 0;
    }

    private static int findHopperActionSlot(net.minecraft.class_1703 handler) {
        int limit = Math.min(handler.field_7761.size(), 100);
        for (int i = 0; i < limit; i++) {
            class_1735 slot = handler.field_7761.get(i);
            if (slot == null || slot.field_7871 == MC.field_1724.method_31548()) continue;
            class_1799 stack = slot.method_7677();
            if (stack.method_7960()) continue;
            String id = itemId(stack);
            if ("minecraft:hopper".equals(id)) return slot.field_7874;
            String name = stack.method_7964().getString().toLowerCase(Locale.ROOT);
            if (name.contains("chuyển tất cả") || name.contains("transfer all")
                    || name.contains("all block") || name.contains("tất cả block")) return slot.field_7874;
        }
        return -1;
    }

    private static boolean isCraftingScreen(class_310 client) {
        return client.field_1724 != null && client.field_1724.field_7512 instanceof class_1714;
    }

    private static int findRareHotbar(class_310 client) {
        var inv = client.field_1724.method_31548();
        for (int i = 0; i < 9; i++) {
            class_1799 stack = inv.method_5438(i);
            if (!stack.method_7960() && RARE_BLOCKS.contains(itemId(stack))) return i;
        }
        return -1;
    }

    private static int findRareInventory(class_310 client) {
        var inv = client.field_1724.method_31548();
        for (int i = 9; i < 36; i++) {
            class_1799 stack = inv.method_5438(i);
            if (!stack.method_7960() && RARE_BLOCKS.contains(itemId(stack))) return i;
        }
        return -1;
    }

    private static int findFreeHotbar(class_310 client) {
        var inv = client.field_1724.method_31548();
        for (int i = 0; i < 9; i++) if (inv.method_5438(i).method_7960()) return i;
        return -1;
    }

    private static void restoreHotbar(class_310 client) {
        if (selectedHotbar >= 0 && selectedHotbar < 9) {
            client.field_1724.method_31548().method_61496(selectedHotbar);
        }
        selectedHotbar = -1;
        workingHotbar = -1;
        lastSyncId = -1;
    }

    public static int getCraftSpeed() { return craftSpeed; }
    public static int getDropSpeed() { return dropSpeed; }
    public static int getThreshold() { return threshold; }

    public static void setCraftSpeed(int value) {
        craftSpeed = clamp(value);
        PeoClient.CFG.autoCraftMaxSpeedCraftSpeed = craftSpeed;
        PeoClient.CFG.save();
    }

    public static void setDropSpeed(int value) {
        dropSpeed = clamp(value);
        PeoClient.CFG.autoCraftMaxSpeedDropSpeed = dropSpeed;
        PeoClient.CFG.save();
    }

    public static void setThreshold(int value) {
        threshold = clamp(value);
        PeoClient.CFG.autoCraftMaxSpeedThreshold = threshold;
        PeoClient.CFG.save();
    }

    public static String getStatus() {
        return enabled ? state.name() + " | rare=" + countRareStacks(MC) + "/" + threshold : "OFF";
    }

    public static boolean isRareBlock(class_1799 stack) {
        return !stack.method_7960() && RARE_BLOCKS.contains(itemId(stack));
    }

    public static boolean isDropBlock(class_1799 stack) {
        return !stack.method_7960() && DROP_BLOCKS.contains(itemId(stack));
    }

    private static int clamp(int value) {
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, value));
    }

    private static void reset() {
        state = State.IDLE;
        waitTicks = 0;
        actionBudget = 0;
        recipeIndex = 0;
        currentIngredient = null;
        selectedHotbar = -1;
        workingHotbar = -1;
        lastSyncId = -1;
        postCloseTicks = 0;
    }

    private static String itemId(class_1799 stack) {
        return class_7923.field_41178.method_10221(stack.method_7909()).toString();
    }

    private static void log(String message) {
        DiagnosticRecorder.get().record("AutoCraftMaxSpeed", message);
    }
}
