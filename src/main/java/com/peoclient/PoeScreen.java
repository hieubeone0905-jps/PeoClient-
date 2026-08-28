package com.peoclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PeoClient 1.21.4 v1 - Wurst-inspired hub layout.
 *
 * This is an independent UI implementation: searchable left module list,
 * three-column module grid, expandable settings panel and per-module keybind capture.
 */
public final class PoeScreen extends Screen {
    private TextFieldWidget search;
    private String expanded = "";
    private String listening = "";
    private int scroll;

    private static final String[] ALL_MODULES = {
            "Fullbright", "InventoryCleaner", "Nuker [Multi]", "X-Ray",
            "AimAssist", "AirPlace", "AnchorAura", "AntiAFK", "AntiBlind",
            "AntiCactus", "AntiEntityPush", "AntiHunger", "AntiKnockback",
            "AntiSpam", "AntiWaterPush", "AntiWobble", "ArrowDMG",
            "AutoArmor", "AutoBuild", "AutoComplete", "AutoDisconnect",
            "AutoDrop", "AutoEat", "AutoFish", "AutoLoot", "AutoMine",
            "AutoReconnect", "AutoSprint", "AutoSwim", "BetterChat",
            "BetterTab", "BlockESP", "ChestESP", "CPS", "Criticals [Packet]",
            "ElytraHelper", "EntityControl", "EntityESP", "FastBreak",
            "FastUse", "Freecam", "Fullbright+", "GuiMove", "HitBox",
            "HoleESP", "ItemSaver", "Jesus", "KillAura", "LiquidVision",
            "MobESP", "NameTags", "NoFall", "NoRotate", "NoSlowDown",
            "NoVoid", "PacketLogger", "PearlPredict", "ProjectileESP",
            "Reach", "Scaffold", "Speed", "Step", "Timer", "Trajectories",
            "Truesight", "Velocity", "WindowWalk", "WorldTime"
    };

    private static final String[] FUNCTIONAL = {
            "Fullbright", "InventoryCleaner", "Nuker [Multi]", "X-Ray"
    };

    public PoeScreen() {
        super(Text.literal("PeoClient 1.21.4 v1"));
    }

    @Override
    protected void init() {
        search = new TextFieldWidget(textRenderer, 254, 24, Math.max(220, width - 560), 22,
                Text.literal("Search"));
        search.setMaxLength(40);
        search.setPlaceholder(Text.literal("Search hacks..."));
        addDrawableChild(search);
        search.setFocused(true);
    }

    private boolean matches(String name) {
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        return q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q);
    }

    private boolean isFunctional(String name) {
        for (String s : FUNCTIONAL) if (s.equals(name)) return true;
        return false;
    }

    private boolean enabled(String name) {
        return switch (name) {
            case "X-Ray" -> PeoClient.CFG.xray;
            case "Nuker [Multi]" -> PeoClient.CFG.nuker;
            case "Fullbright" -> PeoClient.CFG.fullbright;
            case "InventoryCleaner" -> PeoClient.CFG.cleaner;
            default -> false;
        };
    }

    private void toggle(String name) {
        MinecraftClient mc = client;
        if (mc == null) return;
        switch (name) {
            case "X-Ray" -> {
                PeoClient.CFG.xray = !PeoClient.CFG.xray;
                PeoClient.reload(mc);
            }
            case "Nuker [Multi]" -> PeoClient.CFG.nuker = !PeoClient.CFG.nuker;
            case "Fullbright" -> {
                PeoClient.CFG.fullbright = !PeoClient.CFG.fullbright;
                if (!PeoClient.CFG.fullbright && mc.player != null)
                    mc.player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
            }
            case "InventoryCleaner" -> PeoClient.CFG.cleaner = !PeoClient.CFG.cleaner;
        }
        PeoClient.CFG.save();
    }

    private void cycleSetting(String module, int row) {
        var c = PeoClient.CFG;
        switch (module) {
            case "Nuker [Multi]" -> {
                if (row == 0) c.nukerRange = c.nukerRange >= 6 ? 1 : c.nukerRange + 1;
                if (row == 1) c.nukerBlocksPerTick = c.nukerBlocksPerTick >= 4 ? 1 : c.nukerBlocksPerTick + 1;
                if (row == 2) c.nukerRaycast = !c.nukerRaycast;
                if (row == 3) c.nukerRotate = !c.nukerRotate;
                if (row == 4) c.nukerFlatten = !c.nukerFlatten;
                if (row == 5) c.nukerOnlyWhenHoldingTool = !c.nukerOnlyWhenHoldingTool;
                if (row == 6) c.nukerFilter = !c.nukerFilter;
            }
            case "X-Ray" -> {
                if (row == 0) c.xrayFluids = !c.xrayFluids;
                if (row == 1) c.xrayHideSurface = !c.xrayHideSurface;
            }
            case "Fullbright" -> {
                if (row == 0) c.fullbrightNightVision = !c.fullbrightNightVision;
            }
            case "InventoryCleaner" -> {
                if (row == 0) c.cleanerGreedy = !c.cleanerGreedy;
                if (row == 1) c.cleanerMergeStacks = !c.cleanerMergeStacks;
                if (row == 2) c.cleanerTouchHotbar = !c.cleanerTouchHotbar;
                if (row == 3) c.cleanerActionDelay = c.cleanerActionDelay >= 10 ? 1 : c.cleanerActionDelay + 1;
                if (row == 4) c.cleanerAckTimeout = c.cleanerAckTimeout >= 20 ? 4 : c.cleanerAckTimeout + 2;
            }
        }
        c.save();
    }

    private String keyFor(String module) {
        return switch (module) {
            case "X-Ray" -> PeoClient.xrayKey.getBoundKeyLocalizedText().getString();
            case "Nuker [Multi]" -> PeoClient.nukerKey.getBoundKeyLocalizedText().getString();
            case "Fullbright" -> PeoClient.fullbrightKey.getBoundKeyLocalizedText().getString();
            case "InventoryCleaner" -> PeoClient.cleanerKey.getBoundKeyLocalizedText().getString();
            default -> "NONE";
        };
    }

    private void beginKeybind(String module) {
        if (isFunctional(module)) {
            listening = module;
        }
    }

    private void setKey(String module, int keyCode) {
        if (!isFunctional(module) || client == null) return;
        var key = switch (module) {
            case "X-Ray" -> PeoClient.xrayKey;
            case "Nuker [Multi]" -> PeoClient.nukerKey;
            case "Fullbright" -> PeoClient.fullbrightKey;
            case "InventoryCleaner" -> PeoClient.cleanerKey;
            default -> null;
        };
        if (key != null) {
            key.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(keyCode));
            KeyBinding.updateKeysByCode();
            PeoClient.CFG.save();
        }
        listening = "";
    }

    private void drawPanel(DrawContext d, int x, int y, int w, int h, int color) {
        d.fill(x, y, x + w, y + h, color);
        d.drawBorder(x, y, w, h, 0x704E79A8);
    }

    private void drawModule(DrawContext d, String name, int x, int y, int w, int h) {
        boolean active = enabled(name);
        boolean real = isFunctional(name);
        int bg = active ? 0xB02D5F89 : 0xB01D2938;
        drawPanel(d, x, y, w, h, bg);
        d.drawTextWithShadow(textRenderer, name, x + 9, y + 7, active ? 0xFFFFFF55 : 0xFFE8EEF5);
        d.drawTextWithShadow(textRenderer, real ? "▼" : "·", x + w - 18, y + 7, real ? 0xFFFFFFFF : 0xFF66717F);
        if (active) d.drawTextWithShadow(textRenderer, "ON", x + w - 42, y + 7, 0xFF66FF55);
    }

    private List<String> filteredModules() {
        List<String> out = new ArrayList<>();
        for (String s : ALL_MODULES) if (matches(s)) out.add(s);
        return out;
    }

    private void drawSettings(DrawContext d, int x, int y, int w) {
        if (expanded.isEmpty()) {
            drawPanel(d, x, y, w, 110, 0xB018222F);
            d.drawTextWithShadow(textRenderer, "Module Settings", x + 12, y + 12, 0xFFB7C8D9);
            d.drawTextWithShadow(textRenderer, "Click a ▼ module to edit it.", x + 12, y + 34, 0xFF8D9AAA);
            d.drawTextWithShadow(textRenderer, "Click the module name to toggle.", x + 12, y + 52, 0xFF8D9AAA);
            return;
        }

        int h = expanded.equals("Nuker [Multi]") ? 292 : expanded.equals("InventoryCleaner") ? 248 : 190;
        drawPanel(d, x, y, w, h, 0xE0192533);
        int title = enabled(expanded) ? 0xFFFFD83D : 0xFF67D7FF;
        d.drawTextWithShadow(textRenderer, expanded, x + 12, y + 12, title);

        d.drawTextWithShadow(textRenderer, "Enable", x + 12, y + 35, 0xFFE7EDF5);
        drawToggle(d, x + w - 52, y + 31, enabled(expanded));

        String[] rows;
        if (expanded.equals("Nuker [Multi]")) {
            rows = new String[]{
                    "Range: " + PeoClient.CFG.nukerRange,
                    "Blocks/tick: " + PeoClient.CFG.nukerBlocksPerTick,
                    "Raycast: " + on(PeoClient.CFG.nukerRaycast),
                    "Auto Rotate: " + on(PeoClient.CFG.nukerRotate),
                    "Flatten: " + on(PeoClient.CFG.nukerFlatten),
                    "Tool Required: " + on(PeoClient.CFG.nukerOnlyWhenHoldingTool),
                    "Filter: " + on(PeoClient.CFG.nukerFilter)
            };
        } else if (expanded.equals("X-Ray")) {
            rows = new String[]{
                    "Fluids: " + on(PeoClient.CFG.xrayFluids),
                    "Hide Surface: " + on(PeoClient.CFG.xrayHideSurface),
                    "Targets: " + PeoClient.CFG.xrayBlocks.size() + " blocks",
                    "Mode: Ore / Chest / Spawner"
            };
        } else if (expanded.equals("Fullbright")) {
            rows = new String[]{
                    "Night Vision fallback: " + on(PeoClient.CFG.fullbrightNightVision),
                    "Mode: Client brightness",
                    "Keybind: " + keyFor(expanded)
            };
        } else {
            rows = new String[]{
                    "Greedy: " + on(PeoClient.CFG.cleanerGreedy),
                    "Merge Stacks: " + on(PeoClient.CFG.cleanerMergeStacks),
                    "Touch Hotbar: " + on(PeoClient.CFG.cleanerTouchHotbar),
                    "Action Delay: " + PeoClient.CFG.cleanerActionDelay + " ticks",
                    "Ack Timeout: " + PeoClient.CFG.cleanerAckTimeout + " ticks"
            };
        }

        int ry = y + 61;
        for (int i = 0; i < rows.length; i++) {
            d.fill(x + 9, ry - 3, x + w - 9, ry + 20, 0x601E2A38);
            d.drawTextWithShadow(textRenderer, rows[i], x + 15, ry + 3, 0xFFD9E2EA);
            if (i < rows.length - 1 && !rows[i].startsWith("Mode:") && !rows[i].startsWith("Targets:"))
                d.drawTextWithShadow(textRenderer, "›", x + w - 20, ry + 3, 0xFF7EA9CC);
            ry += 28;
        }

        int by = y + h - 32;
        d.fill(x + 10, by, x + w - 10, by + 22, 0x702C4053);
        d.drawTextWithShadow(textRenderer,
                listening.equals(expanded) ? "Press a key..." : "Keybind: " + keyFor(expanded),
                x + 17, by + 7, listening.equals(expanded) ? 0xFFFFFF55 : 0xFFB9C8D7);
    }

    private String on(boolean v) { return v ? "ON" : "OFF"; }

    private void drawToggle(DrawContext d, int x, int y, boolean v) {
        d.fill(x, y, x + 34, y + 16, v ? 0xFF42A65A : 0xFF4B5662);
        d.fill(v ? x + 20 : x + 2, y + 2, v ? x + 32 : x + 14, y + 14, 0xFFFFFFFF);
    }

    @Override
    public void render(DrawContext d, int mouseX, int mouseY, float delta) {
        d.fill(0, 0, width, height, 0xB00A111A);

        // Header.
        d.fill(0, 0, width, 52, 0xE0131D29);
        d.fill(0, 51, width, 53, 0xFF315A78);
        d.drawTextWithShadow(textRenderer, "P", 14, 9, 0xFF6B8CFF);
        d.drawTextWithShadow(textRenderer, "PeoClient", 36, 7, 0xFFFFFFFF);
        d.drawTextWithShadow(textRenderer, "1.21.4", 140, 10, 0xFF9EADBC);
        d.drawTextWithShadow(textRenderer, "v1", 188, 10, 0xFF9EADBC);
        d.drawTextWithShadow(textRenderer, "Right Shift • Hub", width - 125, 11, 0xFF8FA5B9);

        // Left module list.
        int leftW = 228;
        drawPanel(d, 8, 62, leftW, height - 76, 0xC5141D28);
        d.drawTextWithShadow(textRenderer, "HACK LIST", 18, 73, 0xFF7FC8FF);

        int ly = 94;
        int visible = Math.max(1, (height - 112) / 15);
        List<String> mods = filteredModules();
        int start = Math.max(0, Math.min(scroll, Math.max(0, mods.size() - visible)));
        for (int i = start; i < Math.min(mods.size(), start + visible); i++) {
            String name = mods.get(i);
            boolean real = isFunctional(name);
            boolean active = enabled(name);
            if (name.equals(expanded)) d.fill(12, ly - 2, leftW + 4, ly + 13, 0xA02B5371);
            d.drawTextWithShadow(textRenderer, name, 18, ly, active ? 0xFFFFFF55 : (real ? 0xFFE5EDF5 : 0xFF93A1AE));
            if (active) d.drawTextWithShadow(textRenderer, "●", leftW - 10, ly, 0xFF57E65C);
            ly += 15;
        }
        d.drawTextWithShadow(textRenderer, "↑↓ Scroll  •  Click module", 18, height - 27, 0xFF6F8193);

        // Main grid.
        int rightW = 245;
        int gridX = 246;
        int gridW = Math.max(300, width - gridX - rightW - 18);
        int settingsX = width - rightW - 9;
        int cols = 3;
        int gap = 6;
        int cellW = (gridW - gap * (cols - 1)) / cols;
        int gy = 62;

        d.drawTextWithShadow(textRenderer, "MODULES", gridX, 72, 0xFF7FC8FF);

        List<String> grid = filteredModules();
        int idx = 0;
        for (int row = 0; row < 100 && idx < grid.size(); row++) {
            for (int col = 0; col < cols && idx < grid.size(); col++) {
                String name = grid.get(idx++);
                int x = gridX + col * (cellW + gap);
                int y = gy + 18 + row * 29;
                drawModule(d, name, x, y, cellW, 24);
                if (y > height - 45) break;
            }
            if (gy + 18 + row * 29 > height - 45) break;
        }

        // Settings.
        drawSettings(d, settingsX, 62, rightW);

        if (listening.equals(expanded)) {
            d.drawTextWithShadow(textRenderer, "Press a key (ESC cancels)", settingsX + 12,
                    Math.min(height - 15, 62 + (expanded.equals("Nuker [Multi]") ? 292 : 190) - 7),
                    0xFFFFFF55);
        }

        super.render(d, mouseX, mouseY, delta);
        // redraw header title after widgets so it stays crisp
        d.drawTextWithShadow(textRenderer, "PeoClient 1.21.4 v1", 36, 31, 0xFF8FA5B9);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Don't steal clicks from the search field.
        if (mouseX >= 254 && mouseX <= 254 + Math.max(220, width - 560) &&
                mouseY >= 24 && mouseY <= 46) return super.mouseClicked(mouseX, mouseY, button);

        int leftW = 228;
        if (mouseX >= 8 && mouseX <= leftW + 8 && mouseY >= 92 && mouseY < height - 34) {
            List<String> mods = filteredModules();
            int visible = Math.max(1, (height - 112) / 15);
            int start = Math.max(0, Math.min(scroll, Math.max(0, mods.size() - visible)));
            int i = start + (int)((mouseY - 92) / 15);
            if (i >= 0 && i < mods.size()) {
                String name = mods.get(i);
                if (isFunctional(name)) {
                    toggle(name);
                    expanded = name;
                }
                return true;
            }
        }

        int rightW = 245;
        int gridX = 246;
        int gridW = Math.max(300, width - gridX - rightW - 18);
        int cols = 3, gap = 6;
        int cellW = (gridW - gap * (cols - 1)) / cols;
        List<String> grid = filteredModules();
        int idx = 0;
        for (int row = 0; row < 100 && idx < grid.size(); row++) {
            for (int col = 0; col < cols && idx < grid.size(); col++) {
                String name = grid.get(idx++);
                int x = gridX + col * (cellW + gap);
                int y = 80 + row * 29;
                if (mouseX >= x && mouseX <= x + cellW && mouseY >= y && mouseY <= y + 24) {
                    if (isFunctional(name)) {
                        if (mouseX >= x + cellW - 25) {
                            expanded = expanded.equals(name) ? "" : name;
                        } else {
                            toggle(name);
                            expanded = name;
                        }
                    }
                    return true;
                }
            }
            if (80 + row * 29 > height - 45) break;
        }

        int settingsX = width - rightW - 9;
        int settingsY = 62;
        if (!expanded.isEmpty() && mouseX >= settingsX && mouseX <= settingsX + rightW) {
            int h = expanded.equals("Nuker [Multi]") ? 292 : expanded.equals("InventoryCleaner") ? 248 : 190;
            if (mouseY >= settingsY + 28 && mouseY <= settingsY + 54) {
                toggle(expanded);
                return true;
            }
            int rows = expanded.equals("Nuker [Multi]") ? 7 : expanded.equals("InventoryCleaner") ? 5 : expanded.equals("X-Ray") ? 2 : 1;
            int ry = settingsY + 58;
            for (int i = 0; i < rows; i++) {
                if (mouseY >= ry && mouseY <= ry + 23) {
                    cycleSetting(expanded, i);
                    return true;
                }
                ry += 28;
            }
            if (mouseY >= settingsY + h - 34 && mouseY <= settingsY + h) {
                beginKeybind(expanded);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < 240) scroll -= (int)Math.signum(verticalAmount) * 3;
        scroll = Math.max(0, scroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = "";
                return true;
            }
            setKey(listening, keyCode);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
