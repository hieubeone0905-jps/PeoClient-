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
import net.minecraft.class_299;
import net.minecraft.class_10297;
import net.minecraft.class_10352;

import java.util.Collections;

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

    private static final int OPEN_WAIT = 2;
    private static final int OPEN_RETRY_TICKS = 12;
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
    private static int craftSourceSlot = -1;
    private static boolean craftCursorActive;
    private static int craftOutputWait;

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
        selectedHotbar = -1;
        workingHotbar = -1;
        lastSyncId = -1;
        postCloseTicks = 0;
        craftSourceSlot = -1;
        craftCursorActive = false;
        craftOutputWait = 0;
        // Keybind/GUI activation should immediately start the server workflow.
        // The actual GUI may arrive a few ticks later, so keep OPEN_CRAFT until it does.
        if (MC.field_1724 != null && MC.method_1562() != null) {
            sendCraftCommand(MC);
            state = State.OPEN_CRAFT;
            waitTicks = OPEN_WAIT;
        }
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
            // The server may take several ticks to create its custom /craft GUI.
            // Do not silently abandon the module; resend the command periodically.
            if (++waitTicks >= OPEN_RETRY_TICKS) {
                sendCraftCommand(client);
            }
            return;
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
        if (client.method_1562() == null || client.field_1724 == null) return;
        sendCraftCommand(client);
        state = State.OPEN_CRAFT;
        waitTicks = OPEN_WAIT;
    }

    private static void sendCraftCommand(class_310 client) {
        if (client.method_1562() == null || client.field_1724 == null) return;
        client.method_1562().method_45730("craft");
        waitTicks = 0;
        log("OPEN /craft command sent");
    }

    /**
     * BleachHack-style recipe-book crafting engine.
     *
     * Important difference from the previous implementation: we do NOT pick up
     * individual ingredient stacks and distribute them into the 3x3 grid.  The
     * vanilla client already exposes clickRecipe(syncId, recipeId, craftAll),
     * which is exactly what the recipe-book uses for shift-click/craft-all.
     *
     * craftSpeed is treated as the maximum number of recipe operations attempted
     * in this client tick.  Each operation uses craftAll=true, so one operation
     * can consume many stacks from the inventory.  The output is immediately
     * quick-moved just like BleachHack's AutoCraft implementation.
     */
    private static void runCrafting(class_310 client) {
        if (!(client.field_1724.field_7512 instanceof class_1714 handler)) return;

        int rare = countRareStacks(client);
        if (rare >= threshold) {
            state = State.SUBMIT_PREPARE;
            return;
        }

        // Keep the recipe-book state open, matching the vanilla/BleachHack path.
        try {
            class_299 recipeBook = client.field_1724.method_3130();
            recipeBook.method_14884(handler.method_30264(), true);
        } catch (Throwable ignored) {
            // A custom server GUI can still be a CraftingScreenHandler even if
            // the client-side recipe-book state is unavailable for a tick.
        }

        int operations = Math.max(1, Math.min(MAX_SPEED, craftSpeed));
        int craftedOps = 0;
        java.util.Set<String> attemptedOutputs = new java.util.HashSet<>();

        for (int i = 0; i < operations; i++) {
            if (countRareStacks(client) >= threshold) {
                state = State.SUBMIT_PREPARE;
                break;
            }

            class_10297 entry = findCraftableTarget(client, attemptedOutputs);
            if (entry == null) {
                // There is no additional target available from the current
                // inventory. Incoming farm items will be picked up on the next
                // tick. A target is attempted at most once per output type per
                // tick, because craftAll=true already consumes the maximum
                // possible amount of that recipe in one operation.
                break;
            }

            String outputId = itemId(entry.comp_3263().method_64742(new class_10352(Collections.emptyMap())));
            attemptedOutputs.add(outputId);

            try {
                // This is the key BleachHack-style operation: craftAll=true.
                // One operation can consume all currently craftable input stacks
                // for this recipe; there is intentionally no artificial delay.
                client.field_1761.method_2912(handler.field_7763, entry.comp_3262(), true);

                // CraftingScreenHandler output is slot 0. QUICK_MOVE immediately
                // returns the result to the player inventory, matching BleachHack.
                client.field_1761.method_2906(handler.field_7763, 0, 0,
                        class_1713.field_7791, client.field_1724);
                craftedOps++;
            } catch (Throwable t) {
                log("CRAFT operation failed: " + t.getClass().getSimpleName());
                break;
            }
        }

        if (craftedOps > 0) {
            log("CRAFT batch operations=" + craftedOps + " speed=" + craftSpeed);
        }
    }

    /**
     * Find one of our nine target block recipes in the player's synced recipe
     * book.  RecipeDisplayEntry.id() is the runtime NetworkRecipeId required by
     * ClientPlayerInteractionManager.clickRecipe().
     */
    private static class_10297 findCraftableTarget(class_310 client, java.util.Set<String> attemptedOutputs) {
        class_299 book = client.field_1724.method_3130();
        class_10352 emptyContext = new class_10352(Collections.emptyMap());

        for (var collection : book.method_1393()) {
            for (class_10297 entry : collection.method_2650()) {
                try {
                    var result = entry.comp_3263().method_64742(emptyContext);
                    if (result == null || result.method_7960()) continue;
                    String outputId = itemId(result);
                    if (!isTargetOutput(outputId) || attemptedOutputs.contains(outputId)) continue;

                    // Only return recipes that currently have at least one of
                    // their required inputs in the inventory. This prevents a
                    // tight loop on a known recipe while farm items are arriving.
                    if (!hasRecipeIngredient(entry, client)) continue;
                    return entry;
                } catch (Throwable ignored) {
                    // Some custom recipe displays can require context values.
                    // Skip them and continue scanning the remaining entries.
                }
            }
        }
        return null;
    }

    private static boolean hasRecipeIngredient(class_10297 entry, class_310 client) {
        // For this module every target recipe is the standard 9-identical-item
        // mineral-block recipe. The target output is therefore enough to select
        // the corresponding ingredient from our fixed table.
        String output = itemId(entry.comp_3263().method_64742(new class_10352(Collections.emptyMap())));
        for (Recipe recipe : RECIPES) {
            if (!recipe.output.equals(output)) continue;
            return hasInventoryItem(client, recipe.ingredient);
        }
        return false;
    }

    private static boolean hasInventoryItem(class_310 client, String id) {
        var inv = client.field_1724.method_31548();
        for (int i = 0; i < 36; i++) {
            class_1799 stack = inv.method_5438(i);
            if (!stack.method_7960() && id.equals(itemId(stack))) return true;
        }
        return false;
    }

    private static boolean isTargetOutput(String id) {
        for (Recipe recipe : RECIPES) if (recipe.output.equals(id)) return true;
        return false;
    }

    private static void dropConfiguredBlocks(class_310 client, int maxActions) {
        if (!(client.field_1724.field_7512 instanceof class_1714 handler)) return;
        if (maxActions <= 0) return;

        // Do not burst dozens of drop packets into a server-backed inventory.
        // One drop action per tick is intentionally used here; the configured
        // speed remains the user's target rate, while server corrections cannot
        // cause the inventory to visibly snap back and forth.
        for (class_1735 slot : handler.field_7761) {
            if (slot == null || slot.field_7871 != client.field_1724.method_31548()) continue;
            class_1799 stack = slot.method_7677();
            if (stack.method_7960() || !DROP_BLOCKS.contains(itemId(stack))) continue;
            client.field_1761.method_2906(handler.field_7763, slot.field_7874, 1,
                    class_1713.field_7795, client.field_1724);
            return;
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
        craftSourceSlot = -1;
        craftCursorActive = false;
        craftOutputWait = 0;
    }

    private static String itemId(class_1799 stack) {
        return class_7923.field_41178.method_10221(stack.method_7909()).toString();
    }

    private static void log(String message) {
        DiagnosticRecorder.get().record("AutoCraftMaxSpeed", message);
    }
}
