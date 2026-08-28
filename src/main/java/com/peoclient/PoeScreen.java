package com.peoclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class PoeScreen extends Screen {
    private TextFieldWidget search;
    private String selected = "Nuker [Multi]";
    private boolean listening;
    private String bindTarget;
    private double scroll;

    private static final List<String> MODULES = Arrays.asList(
            "Fullbright", "InventoryCleaner", "Nuker [Multi]", "X-Ray",
            "AimAssist", "AirPlace", "AnchorAura", "AntiAFK", "AntiBlind", "AntiCactus",
            "AntiEntityPush", "AntiHunger", "AntiKnockback", "AntiSpam", "AntiWaterPush",
            "AntiWobble", "ArrowDMG", "AutoArmor", "AutoBuild", "AutoComplete", "AutoDisconnect",
            "AutoDrop", "AutoEat", "AutoFish", "AutoLoot", "AutoMine", "AutoReconnect",
            "AutoSprint", "AutoSwim", "BetterChat", "BetterTab", "BlockESP", "BoneESP",
            "CameraNoClip", "ChestESP", "CPS", "Criticals [Packet]", "ElytraHelper",
            "EntityControl", "EntityESP", "FastBreak", "FastUse", "Flight", "Freecam",
            "GuiMove", "HitBox", "HoleESP", "KillAura", "MobESP", "NameTags", "NoFall",
            "NoRotate", "NoSlowDown", "NoVoid", "PacketLogger", "PearlPredict", "ProjectileESP",
            "Reach", "Scaffold", "Speed", "Step", "Timer", "Trajectories", "Truesight",
            "Velocity", "WindowWalk", "WorldTime"
    );

    public PoeScreen() {
        super(Text.literal("PeoClient 1.21.4 v1"));
    }

    @Override
    protected void init() {
        search = new TextFieldWidget(textRenderer, width / 2 - 220, 18, 440, 28,
                Text.literal("Search"));
        search.setMaxLength(64);
        search.setPlaceholder(Text.literal("Search hacks..."));
        addDrawableChild(search);
    }

    private boolean implemented(String name) {
        return name.equals("Fullbright") || name.equals("InventoryCleaner")
                || name.equals("Nuker [Multi]") || name.equals("X-Ray");
    }

    private boolean enabled(String name) {
        return switch (name) {
            case "Fullbright" -> PeoClient.CFG.fullbright;
            case "InventoryCleaner" -> PeoClient.CFG.cleaner;
            case "Nuker [Multi]" -> PeoClient.CFG.nuker;
            case "X-Ray" -> PeoClient.CFG.xray;
            default -> false;
        };
    }

    private void toggle(String name) {
        switch (name) {
            case "Fullbright" -> PeoClient.toggleFullbright(client);
            case "InventoryCleaner" -> PeoClient.CFG.cleaner = !PeoClient.CFG.cleaner;
            case "Nuker [Multi]" -> PeoClient.CFG.nuker = !PeoClient.CFG.nuker;
            case "X-Ray" -> PeoClient.toggleXray(client);
        }
        PeoClient.CFG.save();
    }

    private void openBind(String module) {
        listening = true;
        bindTarget = module;
    }

    private void setBind(String module, int keyCode) {
        var key = switch (module) {
            case "Fullbright" -> PeoClient.fullbrightKey;
            case "InventoryCleaner" -> PeoClient.cleanerKey;
            case "Nuker [Multi]" -> PeoClient.nukerKey;
            case "X-Ray" -> PeoClient.xrayKey;
            default -> null;
        };
        if (key != null) {
            key.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(keyCode));
            MinecraftClient.getInstance().options.write();
        }
        listening = false;
        bindTarget = null;
    }

    private String keyName(String module) {
        var key = switch (module) {
            case "Fullbright" -> PeoClient.fullbrightKey;
            case "InventoryCleaner" -> PeoClient.cleanerKey;
            case "Nuker [Multi]" -> PeoClient.nukerKey;
            case "X-Ray" -> PeoClient.xrayKey;
            default -> null;
        };
        return key == null ? "NONE" : key.getBoundKeyLocalizedText().getString();
    }

    @Override
    public void render(DrawContext d, int mouseX, int mouseY, float delta) {
        d.fill(0, 0, width, height, 0xD90A0F16);

        // Header
        d.fill(0, 0, width, 74, 0xF20B1119);
        d.fill(0, 72, width, 75, 0xFF1D466A);
        d.drawTextWithShadow(textRenderer, "P", 18, 26, 0xFF5E8DFF);
        d.drawTextWithShadow(textRenderer, "PeoClient", 48, 18, 0xFFF5F7FF);
        d.drawTextWithShadow(textRenderer, "1.21.4", 276, 22, 0xFF9CA7B7);
        d.drawTextWithShadow(textRenderer, "v1", 365, 22, 0xFF9CA7B7);
        d.drawTextWithShadow(textRenderer, "Right Shift • Hub", width - 154, 22, 0xFFB5C0CF);
        d.drawTextWithShadow(textRenderer, listening
                ? "Press a key (ESC cancels)" : "Click module to toggle • ▼ settings",
                width - 270, 51, listening ? 0xFFFFD429 : 0xFF8E9AA8);

        super.render(d, mouseX, mouseY, delta);

        // Panels
        int top = 88;
        int bottom = height - 22;
        int leftW = 250;
        int settingsW = Math.min(360, Math.max(300, width / 4));
        int mainX = leftW + 16;
        int settingsX = width - settingsW - 16;
        int mainW = settingsX - mainX - 12;

        panel(d, 16, top, leftW, bottom - top, 0xC90C131C);
        panel(d, mainX, top, mainW, bottom - top, 0xB50B1118);
        panel(d, settingsX, top, settingsW, bottom - top, 0xC90C131C);

        d.drawTextWithShadow(textRenderer, "HACK LIST", 34, top + 24, 0xFF72CFFF);
        d.drawTextWithShadow(textRenderer, "MODULES", mainX + 14, top + 24, 0xFF72CFFF);
        d.drawTextWithShadow(textRenderer, "Module Settings", settingsX + 18, top + 24, 0xFFD6DCE6);

        drawHackList(d, 34, top + 48, leftW - 20, bottom - top - 66);
        drawModules(d, mainX + 12, top + 48, mainW - 24, bottom - top - 66);
        drawSettings(d, settingsX + 14, top + 48, settingsW - 28, bottom - top - 66);

        d.drawTextWithShadow(textRenderer, "↑↓ Scroll • Click module", 22, height - 16, 0xFF7D8794);
    }

    private void panel(DrawContext d, int x, int y, int w, int h, int color) {
        d.fill(x, y, x + w, y + h, color);
        d.drawBorder(x, y, w, h, 0xFF203B55);
    }

    private void drawHackList(DrawContext d, int x, int y, int w, int h) {
        String q = query();
        int row = 15;
        int yy = y - (int) scroll;
        for (String name : MODULES) {
            if (!q.isBlank() && !name.toLowerCase(Locale.ROOT).contains(q)) continue;
            if (yy > y - row && yy < y + h) {
                int color = implemented(name) ? 0xFFE2E7EE : 0xFF87919D;
                if (enabled(name)) color = 0xFFFFE84D;
                d.drawTextWithShadow(textRenderer, name, x, yy, color);
                if (enabled(name)) d.fill(x + w - 7, yy + 4, x + w - 2, yy + 9, 0xFF4DFF5D);
            }
            yy += row;
        }
    }

    private void drawModules(DrawContext d, int x, int y, int w, int h) {
        String q = query();
        List<String> filtered = new ArrayList<>();
        for (String n : MODULES)
            if (q.isBlank() || n.toLowerCase(Locale.ROOT).contains(q)) filtered.add(n);

        int cols = 3;
        int gap = 10;
        int cardW = (w - gap * (cols - 1)) / cols;
        int rowH = 35;
        int yy = y - (int) scroll;
        for (int i = 0; i < filtered.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cardW + gap);
            int cy = yy + row * rowH;
            if (cy < y - rowH || cy > y + h) continue;

            boolean real = implemented(filtered.get(i));
            boolean on = enabled(filtered.get(i));
            int bg = on ? 0xC92A435A : 0xA6121B25;
            if (filtered.get(i).equals(selected)) bg = on ? 0xD53A5874 : 0xB31B2A3A;

            d.fill(cx, cy, cx + cardW, cy + 30, bg);
            d.drawBorder(cx, cy, cardW, 30, real ? 0xFF2B5578 : 0xFF243340);
            d.drawTextWithShadow(textRenderer, filtered.get(i), cx + 12, cy + 10,
                    real ? (on ? 0xFFFFEA42 : 0xFFE7ECF2) : 0xFF9AA4AF);
            d.drawTextWithShadow(textRenderer, "▼", cx + cardW - 20, cy + 10,
                    real ? 0xFFDDE6EF : 0xFF5F6974);
            if (on) d.fill(cx + cardW - 42, cy + 25, cx + cardW - 34, cy + 27, 0xFF4DFF5D);
        }
    }

    private void drawSettings(DrawContext d, int x, int y, int w, int h) {
        if (!implemented(selected)) {
            d.drawTextWithShadow(textRenderer, "Select a supported module.", x, y + 18, 0xFF8F9AA7);
            d.drawTextWithShadow(textRenderer, "Only modules with real logic can be toggled.",
                    x, y + 36, 0xFF687481);
            return;
        }

        int yy = y + 6;
        d.drawTextWithShadow(textRenderer, selected, x, yy, colorFor(selected));
        yy += 24;
        row(d, x, yy, w, "Enable", enabled(selected), true); yy += 28;
        row(d, x, yy, w, "Keybind", keyName(selected), false); yy += 28;

        switch (selected) {
            case "Nuker [Multi]" -> yy = nukerSettings(d, x, yy, w);
            case "X-Ray" -> yy = xraySettings(d, x, yy, w);
            case "Fullbright" -> yy = fullbrightSettings(d, x, yy, w);
            case "InventoryCleaner" -> cleanerSettings(d, x, yy, w);
        }
    }

    private int nukerSettings(DrawContext d, int x, int y, int w) {
        row(d, x, y, w, "Mode", PeoClient.CFG.nukerMode, false); y += 28;
        row(d, x, y, w, "Multi", PeoClient.CFG.nukerMulti, false); y += 28;
        row(d, x, y, w, "Cooldown", PeoClient.CFG.nukerCooldown + " ticks", false); y += 28;
        row(d, x, y, w, "Shape", PeoClient.CFG.nukerShape, false); y += 28;
        row(d, x, y, w, "Range", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.nukerRange), false); y += 28;
        row(d, x, y, w, "Sort", PeoClient.CFG.nukerSort, false); y += 28;
        row(d, x, y, w, "Filter", PeoClient.CFG.nukerFilter, true); y += 28;
        row(d, x, y, w, "Whitelist", PeoClient.CFG.nukerWhitelist, true); y += 28;
        row(d, x, y, w, "Raycast", PeoClient.CFG.nukerRaycast, true); y += 28;
        row(d, x, y, w, "Flatten", PeoClient.CFG.nukerFlatten, true); y += 28;
        row(d, x, y, w, "Rotate", PeoClient.CFG.nukerRotate, true); y += 28;
        row(d, x, y, w, "Highlight", PeoClient.CFG.nukerHighlight, true); y += 28;
        return y;
    }

    private int xraySettings(DrawContext d, int x, int y, int w) {
        row(d, x, y, w, "FullBright", PeoClient.CFG.xrayFullBright, true); y += 28;
        row(d, x, y, w, "Fluids", PeoClient.CFG.xrayFluids, true); y += 28;
        row(d, x, y, w, "ExposedOnly", PeoClient.CFG.xrayExposedOnly, true); y += 28;
        row(d, x, y, w, "BackgroundOpacity", PeoClient.CFG.xrayBackgroundOpacity, false); y += 28;
        row(d, x, y, w, "Blocks", PeoClient.CFG.xrayBlocks.size() + " selected", false); y += 28;
        d.drawTextWithShadow(textRenderer, "Ore set: coal • copper • iron • gold", x, y, 0xFF9AA5B2); y += 17;
        d.drawTextWithShadow(textRenderer, "diamond • emerald • redstone • lapis", x, y, 0xFF9AA5B2);
        return y + 24;
    }

    private int fullbrightSettings(DrawContext d, int x, int y, int w) {
        row(d, x, y, w, "Method", PeoClient.CFG.fullbrightMethod, false); y += 28;
        row(d, x, y, w, "Fade", PeoClient.CFG.fullbrightFade, true); y += 28;
        row(d, x, y, w, "Brightness", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.fullbrightBrightness), false); y += 28;
        row(d, x, y, w, "Default brightness", String.format(Locale.ROOT, "%.2f", PeoClient.CFG.fullbrightDefaultBrightness), false); y += 28;
        d.drawTextWithShadow(textRenderer,
                "Gamma mode uses a forced gamma value, not a", x, y, 0xFF9AA5B2);
        d.drawTextWithShadow(textRenderer,
                "Night Vision status effect.", x, y + 17, 0xFF9AA5B2);
        return y + 40;
    }

    private int cleanerSettings(DrawContext d, int x, int y, int w) {
        row(d, x, y, w, "Greedy", PeoClient.CFG.cleanerGreedy, true); y += 28;
        row(d, x, y, w, "Merge stacks", PeoClient.CFG.cleanerMergeStacks, true); y += 28;
        row(d, x, y, w, "Touch hotbar", PeoClient.CFG.cleanerTouchHotbar, true); y += 28;
        row(d, x, y, w, "Action delay", PeoClient.CFG.cleanerActionDelay + " ticks", false); y += 28;
        row(d, x, y, w, "Ack timeout", PeoClient.CFG.cleanerAckTimeout + " ticks", false); y += 28;
        row(d, x, y, w, "Max blocks", PeoClient.CFG.maxBlocks, false); y += 28;
        row(d, x, y, w, "Max arrows", PeoClient.CFG.maxArrows, false); y += 28;
        row(d, x, y, w, "Max throwables", PeoClient.CFG.maxThrowables, false); y += 28;
        row(d, x, y, w, "Max food points", PeoClient.CFG.maxFoods, false);
        return y + 28;
    }

    private void row(DrawContext d, int x, int y, int w, String label, Object value, boolean toggle) {
        d.drawTextWithShadow(textRenderer, label, x, y + 8, 0xFFE0E6ED);
        String text = String.valueOf(value);
        int tw = textRenderer.getWidth(text);
        d.fill(x + w - tw - 10, y + 3, x + w, y + 24, 0xA51B2733);
        d.drawTextWithShadow(textRenderer, text, x + w - tw - 5, y + 8,
                toggle ? (Boolean.TRUE.equals(value) ? 0xFF55FF66 : 0xFF8E99A6) : 0xFFB9C7D5);
    }

    private int colorFor(String s) {
        return switch (s) {
            case "Fullbright" -> 0xFF70FF70;
            case "X-Ray" -> 0xFF40C8FF;
            case "Nuker [Multi]" -> 0xFFFFB000;
            default -> 0xFFFF70FF;
        };
    }

    private String query() {
        return search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int top = 88, bottom = height - 22;
        int leftW = 250;
        int mainX = leftW + 16;
        int settingsW = Math.min(360, Math.max(300, width / 4));
        int settingsX = width - settingsW - 16;
        int mainW = settingsX - mainX - 12;

        if (mouseY >= top + 48 && mouseY < bottom) {
            // Module grid
            String q = query();
            List<String> filtered = new ArrayList<>();
            for (String n : MODULES)
                if (q.isBlank() || n.toLowerCase(Locale.ROOT).contains(q)) filtered.add(n);

            int cols = 3, gap = 10, cardW = (mainW - gap * 2) / 3, rowH = 35;
            int yy = top + 48 - (int) scroll;
            for (int i = 0; i < filtered.size(); i++) {
                int col = i % cols, row = i / cols;
                int cx = mainX + 12 + col * (cardW + gap);
                int cy = yy + row * rowH;
                if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + 30) {
                    String name = filtered.get(i);
                    if (implemented(name)) {
                        selected = name;
                        if (mouseX > cx + cardW - 32) {
                            // Arrow = select settings without toggling.
                        } else {
                            toggle(name);
                        }
                    }
                    return true;
                }
            }
        }

        if (mouseX >= settingsX && mouseY >= top + 48 && mouseY < bottom && implemented(selected)) {
            int y = top + 54;
            if (mouseY >= y && mouseY <= y + 24) {
                toggle(selected);
                return true;
            }
            y += 28;
            if (mouseY >= y && mouseY <= y + 24) {
                openBind(selected);
                return true;
            }
            y += 28;

            switch (selected) {
                case "Nuker [Multi]" -> {
                    if (clickNuker(mouseY, y)) return true;
                }
                case "X-Ray" -> {
                    if (clickXray(mouseY, y)) return true;
                }
                case "Fullbright" -> {
                    if (clickFullbright(mouseY, y)) return true;
                }
                case "InventoryCleaner" -> {
                    if (clickCleaner(mouseY, y)) return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickNuker(double my, int y) {
        if (hit(my, y)) { PeoClient.CFG.nukerMode = switch (PeoClient.CFG.nukerMode) {
            case "Normal" -> "SurvMulti"; case "SurvMulti" -> "Multi"; case "Multi" -> "Instant"; default -> "Normal";
        }; return save();
        }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerMulti = PeoClient.CFG.nukerMulti >= 10 ? 1 : PeoClient.CFG.nukerMulti + 1; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerCooldown = PeoClient.CFG.nukerCooldown >= 4 ? 0 : PeoClient.CFG.nukerCooldown + 1; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerShape = PeoClient.CFG.nukerShape.equals("Cube") ? "Sphere" : "Cube"; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerRange = PeoClient.CFG.nukerRange >= 6 ? 1 : PeoClient.CFG.nukerRange + 0.5; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerSort = switch (PeoClient.CFG.nukerSort) {
            case "Closest" -> "Furthest"; case "Furthest" -> "Softest"; case "Softest" -> "Hardest"; case "Hardest" -> "None"; default -> "Closest";
        }; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerFilter = !PeoClient.CFG.nukerFilter; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerWhitelist = !PeoClient.CFG.nukerWhitelist; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerRaycast = !PeoClient.CFG.nukerRaycast; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerFlatten = !PeoClient.CFG.nukerFlatten; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerRotate = !PeoClient.CFG.nukerRotate; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.nukerHighlight = !PeoClient.CFG.nukerHighlight; return save(); }
        return false;
    }

    private boolean clickXray(double my, int y) {
        if (hit(my, y)) { PeoClient.CFG.xrayFullBright = !PeoClient.CFG.xrayFullBright; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.xrayFluids = !PeoClient.CFG.xrayFluids; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.xrayExposedOnly = !PeoClient.CFG.xrayExposedOnly; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.xrayBackgroundOpacity = PeoClient.CFG.xrayBackgroundOpacity >= 240 ? 0 : PeoClient.CFG.xrayBackgroundOpacity + 40; return save(); }
        return false;
    }

    private boolean clickFullbright(double my, int y) {
        if (hit(my, y)) { PeoClient.CFG.fullbrightMethod = PeoClient.CFG.fullbrightMethod.equals("Gamma") ? "Night Vision" : "Gamma"; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.fullbrightFade = !PeoClient.CFG.fullbrightFade; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.fullbrightBrightness = PeoClient.CFG.fullbrightBrightness >= 16 ? 1 : PeoClient.CFG.fullbrightBrightness + 1; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.fullbrightDefaultBrightness = PeoClient.CFG.fullbrightDefaultBrightness >= 1 ? 0 : PeoClient.CFG.fullbrightDefaultBrightness + 0.1; return save(); }
        return false;
    }

    private boolean clickCleaner(double my, int y) {
        if (hit(my, y)) { PeoClient.CFG.cleanerGreedy = !PeoClient.CFG.cleanerGreedy; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.cleanerMergeStacks = !PeoClient.CFG.cleanerMergeStacks; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.cleanerTouchHotbar = !PeoClient.CFG.cleanerTouchHotbar; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.cleanerActionDelay = PeoClient.CFG.cleanerActionDelay >= 10 ? 1 : PeoClient.CFG.cleanerActionDelay + 1; return save(); }
        if (hit(my, y += 28)) { PeoClient.CFG.cleanerAckTimeout = PeoClient.CFG.cleanerAckTimeout >= 60 ? 10 : PeoClient.CFG.cleanerAckTimeout + 5; return save(); }
        return false;
    }

    private boolean hit(double mouseY, int y) {
        return mouseY >= y && mouseY <= y + 24;
    }

    private boolean save() {
        PeoClient.CFG.save();
        if (client != null) PeoClient.reload(client);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = false;
                bindTarget = null;
                return true;
            }
            setBind(bindTarget, keyCode);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, scroll - verticalAmount * 28);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
