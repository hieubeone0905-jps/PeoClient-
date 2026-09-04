package com.peoclient.modules;

import com.peoclient.PeoClient;
import net.minecraft.class_1268;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_239;
import net.minecraft.class_3965;
import net.minecraft.class_310;
import net.minecraft.class_7923;
import net.minecraft.class_490;

import java.util.Set;

/**
 * UpLevelVipProMax
 *
 * Automates the server-side island-level workflow described by the user:
 * - only the seven configured level blocks may trigger placement;
 * - the four known zero-value blocks are dropped from the player's inventory;
 * - raw mineral blocks / other items are never dropped by this module;
 * - after the server opens the "Cấp độ đảo" handled screen, click the hopper
 *   action ("Chuyển Tất Cả Block"), wait briefly for the server to process it,
 *   then close the handled screen normally.
 *
 * This uses normal client inventory/block interaction APIs. It does not spoof
 * server packets or attempt to bypass anti-cheat.
 */
public final class UpLevelVipProMax {
    private static final class_310 mc = class_310.method_1551();

    private static final Set<String> LEVEL_BLOCKS = Set.of(
            "minecraft:diamond_block",
            "minecraft:emerald_block",
            "minecraft:lapis_block",
            "minecraft:coal_block",
            "minecraft:redstone_block",
            "minecraft:iron_block",
            "minecraft:gold_block"
    );

    private static final int DROP_STACKS_PER_TICK = 16;

    private static final Set<String> DROP_BLOCKS = Set.of(
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:raw_gold_block",
            "minecraft:raw_iron_block"
    );

    private static final String LEVEL_SCREEN_TITLE = "Cấp độ đảo";
    private static final int PROCESS_WAIT_TICKS = 10;
    private static final int CLOSE_WAIT_TICKS = 10;
    private static final int ACTION_COOLDOWN_TICKS = 10;
    private static final int INVENTORY_ACTION_COOLDOWN = 3;
    private static final int FALLBACK_HOTBAR_SLOT = 8;
    private static final int INVENTORY_VERIFY_TICKS = 2;

    private static boolean enabled;
    private static State state = State.IDLE;
    private static int waitTicks;
    private static int cooldown;
    private static int lastContainerSyncId = -1;
    private static int selectedHotbar = -1;
    private static int workingHotbar = -1;
    private static boolean configInitialized;

    private enum State {
        IDLE,
        PLACE_LEVEL_BLOCK,
        WAIT_FOR_LEVEL_GUI,
        CLICK_HOPPER,
        WAIT_FOR_SERVER,
        CLOSE_GUI,
        OPEN_INVENTORY_VERIFY,
        CLOSE_INVENTORY_VERIFY,
        COOLDOWN
    }

    private UpLevelVipProMax() {}

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            reset();
        } else {
            state = State.IDLE;
            waitTicks = 0;
            cooldown = 0;
        }
        PeoClient.CFG.upLevelVipProMax = enabled;
        PeoClient.CFG.save();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void tick(class_310 client) {
        if (!configInitialized) {
            enabled = PeoClient.CFG.upLevelVipProMax;
            configInitialized = true;
        }

        if (!enabled || client.field_1724 == null || client.field_1761 == null
                || client.field_1687 == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // Post-level verification deliberately uses the vanilla inventory screen
        // for a couple of ticks so the player state visibly refreshes like pressing E.
        if (state == State.OPEN_INVENTORY_VERIFY || state == State.CLOSE_INVENTORY_VERIFY) {
            handleInventoryVerify(client);
            return;
        }

        // Never touch the inventory while another handled GUI is being processed.
        if (isLevelScreen(client)) {
            handleLevelScreen(client);
            return;
        }

        // If a different GUI is open, leave it completely alone.
        if (client.field_1755 != null) {
            state = State.IDLE;
            return;
        }

        // Dispose matching blocks in a bounded batch. This keeps the normal
        // inventory THROW action while allowing up to 16 stacks/actions per tick.
        int dropped = dropInvalidBlocksBatch(client, DROP_STACKS_PER_TICK);
        if (dropped > 0) {
            return;
        }

        switch (state) {
            case IDLE, PLACE_LEVEL_BLOCK -> tryPlaceLevelBlock(client);
            case WAIT_FOR_LEVEL_GUI -> {
                // A successful placement should open the server GUI. If it did not,
                // return to scanning rather than repeatedly placing the same stack.
                if (++waitTicks > 10) {
                    restoreSelectedHotbar(client);
                    state = State.IDLE;
                    waitTicks = 0;
                }
            }
            case CLICK_HOPPER, WAIT_FOR_SERVER, CLOSE_GUI, OPEN_INVENTORY_VERIFY, CLOSE_INVENTORY_VERIFY, COOLDOWN -> {
                // These states are normally consumed by handleLevelScreen().
                state = State.IDLE;
            }
        }
    }

    private static int dropInvalidBlocksBatch(class_310 client, int maxActions) {
        var inv = client.field_1724.method_31548();
        int dropped = 0;

        // Re-scan after each THROW so slot contents are always current. The
        // upper bound prevents an accidental unbounded inventory loop.
        while (dropped < maxActions) {
            boolean found = false;
            for (int slot = 0; slot < 36 && dropped < maxActions; slot++) {
                class_1799 stack = inv.method_5438(slot);
                if (stack.method_7960()) continue;
                if (!DROP_BLOCKS.contains(itemId(stack))) continue;

                int screenSlot = playerInventoryScreenSlot(slot);
                client.field_1761.method_2906(
                        client.field_1724.field_7512.field_7763,
                        screenSlot,
                        1,
                        class_1713.field_7795,
                        client.field_1724
                );
                dropped++;
                found = true;
            }
            if (!found) break;
        }
        return dropped;
    }

    /**
     * Find a valuable block in the COMPLETE player inventory, not just the hotbar.
     * If it is in the main inventory, swap it into a dedicated hotbar slot using
     * the normal inventory SWAP action. This is the important part that was missing
     * from the previous version.
     */
    private static int findAndPrepareValuableBlock(class_310 client) {
        var inv = client.field_1724.method_31548();

        // Prefer an already available hotbar stack.
        for (int slot = 0; slot < 9; slot++) {
            class_1799 stack = inv.method_5438(slot);
            if (!stack.method_7960() && LEVEL_BLOCKS.contains(itemId(stack))) return slot;
        }

        // Then scan the entire main inventory (slots 9..35).
        for (int slot = 9; slot < 36; slot++) {
            class_1799 stack = inv.method_5438(slot);
            if (stack.method_7960() || !LEVEL_BLOCKS.contains(itemId(stack))) continue;

            int targetHotbar = FALLBACK_HOTBAR_SLOT;
            // If the fallback slot somehow contains a valuable block, choose another slot.
            for (int h = 0; h < 9; h++) {
                if (inv.method_5438(h).method_7960()) {
                    targetHotbar = h;
                    break;
                }
            }

            client.field_1761.method_2906(
                    client.field_1724.field_7512.field_7763,
                    playerInventoryScreenSlot(slot),
                    targetHotbar,
                    class_1713.field_7791,
                    client.field_1724
            );
            workingHotbar = targetHotbar;
            return targetHotbar;
        }
        return -1;
    }

    private static void tryPlaceLevelBlock(class_310 client) {
        var inv = client.field_1724.method_31548();
        int foundHotbar = findAndPrepareValuableBlock(client);

        if (foundHotbar < 0) {
            state = State.IDLE;
            return;
        }

        class_239 hit = client.field_1765;
        if (!(hit instanceof class_3965 blockHit)) {
            state = State.IDLE;
            return;
        }

        selectedHotbar = inv.field_7545;
        if (selectedHotbar != foundHotbar) inv.field_7545 = foundHotbar;

        client.field_1761.method_2896(client.field_1724, class_1268.field_5808, blockHit);
        state = State.WAIT_FOR_LEVEL_GUI;
        waitTicks = 0;
    }

    private static boolean isLevelScreen(class_310 client) {
        if (client.field_1755 == null || client.field_1724 == null) return false;
        if (!(client.field_1755 instanceof net.minecraft.class_465)) return false;

        String title;
        try {
            title = client.field_1755.method_25440().getString();
        } catch (Throwable ignored) {
            return false;
        }
        String normalized = title == null ? "" : title.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.contains(LEVEL_SCREEN_TITLE.toLowerCase(java.util.Locale.ROOT));
    }

    private static void handleLevelScreen(class_310 client) {
        if (client.field_1724.field_7512 == null) return;

        int syncId = client.field_1724.field_7512.field_7763;
        if (lastContainerSyncId != syncId) {
            lastContainerSyncId = syncId;
            state = State.CLICK_HOPPER;
            waitTicks = 0;
        } else if (state == State.WAIT_FOR_LEVEL_GUI || state == State.IDLE) {
            // A server can reuse a handler sync id. The fact that the expected
            // level screen is visible is enough to arm a fresh hopper click.
            state = State.CLICK_HOPPER;
            waitTicks = 0;
        }

        if (state == State.CLICK_HOPPER) {
            int hopperSlot = findHopperSlot(client);
            if (hopperSlot < 0) {
                // Give the server one more tick to populate/update the menu.
                if (++waitTicks <= 10) return;
                state = State.WAIT_FOR_SERVER;
                waitTicks = 3;
                return;
            }

            // Normal PICKUP click on the server-provided hopper/action item.
            client.field_1761.method_2906(
                    syncId, hopperSlot, 0, class_1713.field_7790, client.field_1724);
            state = State.WAIT_FOR_SERVER;
            waitTicks = PROCESS_WAIT_TICKS;
            return;
        }

        if (state == State.WAIT_FOR_SERVER) {
            if (--waitTicks > 0) return;
            state = State.CLOSE_GUI;
        }

        if (state == State.CLOSE_GUI) {
            // closeHandledScreen() sends the normal close packet and closes the
            // server-backed menu. This is equivalent to leaving with ESC, without
            // opening another screen or desynchronizing the handler.
            try {
                client.field_1724.method_7346();
            } catch (Throwable ignored) {
                client.method_1507(null);
            }
            restoreSelectedHotbar(client);
            workingHotbar = -1;
            // Give the server a short normal post-close acknowledgement window.
            // Then briefly open the vanilla inventory (same screen the player gets
            // with E) and close it again; this forces the client to consume the
            // latest player/inventory state without sending synthetic packets.
            state = State.OPEN_INVENTORY_VERIFY;
            waitTicks = CLOSE_WAIT_TICKS;
            return;
        }

        if (state == State.COOLDOWN) {
            if (--waitTicks <= 0) {
                state = State.IDLE;
                cooldown = ACTION_COOLDOWN_TICKS;
            }
        }
    }

    private static void handleInventoryVerify(class_310 client) {
        if (state == State.OPEN_INVENTORY_VERIFY) {
            if (--waitTicks > 0) return;
            if (client.field_1755 == null && client.field_1724 != null) {
                client.method_1507(new class_490(client.field_1724));
                state = State.CLOSE_INVENTORY_VERIFY;
                waitTicks = INVENTORY_VERIFY_TICKS;
                return;
            }
            state = State.COOLDOWN;
            waitTicks = CLOSE_WAIT_TICKS;
            return;
        }

        if (state == State.CLOSE_INVENTORY_VERIFY) {
            if (--waitTicks > 0) return;
            try {
                client.field_1724.method_7346();
            } catch (Throwable ignored) {
                client.method_1507(null);
            }
            state = State.COOLDOWN;
            waitTicks = CLOSE_WAIT_TICKS;
        }
    }

    private static int findHopperSlot(class_310 client) {
        var handler = client.field_1724.field_7512;
        int limit = 100;
        for (int i = 0; i < limit; i++) {
            class_1799 stack;
            try {
                stack = handler.method_7611(i).method_7677();
            } catch (Throwable ignored) {
                continue;
            }
            if (stack.method_7960()) continue;
            if ("minecraft:hopper".equals(itemId(stack))) return i;

            // Some server menus use a renamed item instead of the literal hopper id.
            String name = stack.method_7964().getString().toLowerCase(java.util.Locale.ROOT);
            if (name.contains("chuyển tất cả") || name.contains("transfer all")
                    || name.contains("all block") || name.contains("tất cả block")) return i;
        }
        return -1;
    }

    /**
     * Maps PlayerInventory indices (0..35) to PlayerScreenHandler slot ids.
     * In the vanilla player handler, hotbar inventory slots 0..8 are exposed
     * as screen slots 36..44, while main inventory slots 9..35 keep their ids.
     */
    private static int playerInventoryScreenSlot(int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= 36) return -1;
        return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
    }

    private static void restoreSelectedHotbar(class_310 client) {
        if (selectedHotbar >= 0 && selectedHotbar < 9) {
            client.field_1724.method_31548().field_7545 = selectedHotbar;
        }
        selectedHotbar = -1;
        workingHotbar = -1;
    }

    private static String itemId(class_1799 stack) {
        return class_7923.field_41178.method_10221(stack.method_7909()).toString();
    }

    private static void reset() {
        state = State.IDLE;
        waitTicks = 0;
        cooldown = 0;
        lastContainerSyncId = -1;
        selectedHotbar = -1;
        workingHotbar = -1;
    }

    /** Exposed for the GUI/diagnostics without exposing mutable collections. */
    public static boolean isLevelBlock(class_1799 stack) {
        return !stack.method_7960() && LEVEL_BLOCKS.contains(itemId(stack));
    }

    public static boolean isDropBlock(class_1799 stack) {
        return !stack.method_7960() && DROP_BLOCKS.contains(itemId(stack));
    }

    public static String getStatus() {
        return enabled ? state.name() : "OFF";
    }
}
