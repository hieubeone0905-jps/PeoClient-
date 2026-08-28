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

/**
 * PeoClient V1 Hub.
 *
 * Layout is intentionally close to the Wurst-style Hub shown by the user:
 * compact hack list on the far left, detailed settings immediately to its right,
 * and a three-column module grid occupying the rest of the screen.  No settings
 * panel is placed on the far right, and every panel has its own scroll position.
 */
public final class PoeScreen extends Screen {
    private TextFieldWidget search;
    private String selected = "Nuker [Multi]";
    private boolean listening;
    private String bindTarget;
    private double hackScroll;
    private double moduleScroll;
    private double settingsScroll;

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
        super(Text.literal("PeoClient 1.21.4 V1"));
    }

    @Override
    protected void init() {
        search = new TextFieldWidget(textRenderer, Math.max(430, width / 2 - 230), 18, 460, 30,
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
        // Wurst-like dark translucent Hub background.
        d.fill(0, 0, width, height, 0xD9080D14);
        d.fill(0, 0, width, 72, 0xF20A1119);
        d.fill(0, 70, width, 74, 0xFF1D5278);

        // Header.
        text(d, "P", 18, 22, 0xFF5AA8FF, true);
        text(d, "PeoClient", 46, 16, 0xFFFFFFFF, true);
        text(d, "1.21.4", 278, 20, 0xFFB8C7D9, false);
        text(d, "V1", 374, 20, 0xFFB8C7D9, false);
        text(d, "Right Shift • Hub", width - 158, 20, 0xFFB8C7D9, false);
        text(d, listening ? "Press a key • ESC cancels" : "Left click = toggle • Right click = select settings",
                width - 360, 48, listening ? 0xFFFFD54A : 0xFF8090A0, false);

        super.render(d, mouseX, mouseY, delta);

        int top = 88;
        int bottom = height - 24;
        int hackX = 14, hackW = 208;
        int settingsX = hackX + hackW + 10, settingsW = 330;
        int mainX = settingsX + settingsW + 12;
        int mainW = width - mainX - 14;

        panel(d, hackX, top, hackW, bottom - top, 0xDD0C141E, 0xFF29445E);
        panel(d, settingsX, top, settingsW, bottom - top, 0xDD0C141E, 0xFF29445E);
        panel(d, mainX, top, mainW, bottom - top, 0xB80A1118, 0xFF29445E);

        text(d, "HACK LIST", hackX + 18, top + 18, 0xFF69CFFF, true);
        text(d, "SETTINGS", settingsX + 18, top + 18, colorFor(selected), true);
        text(d, "MODULES", mainX + 14, top + 18, 0xFF69CFFF, true);

        drawHackList(d, hackX + 18, top + 42, hackW - 28, bottom - top - 60);
        drawSettings(d, settingsX + 14, top + 42, settingsW - 28, bottom - top - 60);
        drawModules(d, mainX + 12, top + 42, mainW - 24, bottom - top - 60);

        text(d, "↑↓ Scroll", hackX + 18, height - 16, 0xFF758595, false);
        text(d, "ESC • Close", width - 92, height - 16, 0xFF758595, false);
    }

    private void panel(DrawContext d, int x, int y, int w, int h, int fill, int border) {
        d.fill(x, y, x + w, y + h, fill);
        d.drawBorder(x, y, w, h, border);
    }

    private void text(DrawContext d, String s, int x, int y, int color, boolean bold) {
        d.drawTextWithShadow(textRenderer, s, x, y, color);
        if (bold) d.drawTextWithShadow(textRenderer, s, x + 1, y, color);
    }

    private void drawHackList(DrawContext d, int x, int y, int w, int h) {
        List<String> filtered = filtered();
        int rowH = 16;
        int yy = y - (int) hackScroll;
        for (String name : filtered) {
            if (yy + rowH >= y && yy <= y + h) {
                boolean real = implemented(name);
                boolean on = enabled(name);
                if (name.equals(selected)) d.fill(x - 8, yy - 2, x + w, yy + 14, 0xFF19344A);
                text(d, name, x, yy, on ? colorFor(name) : (real ? 0xFFE5ECF3 : 0xFF8997A5), false);
                if (on) {
                    d.fill(x + w - 9, yy + 4, x + w - 3, yy + 10, 0xFF55E66A);
                }
            }
            yy += rowH;
        }
    }

    private void drawModules(DrawContext d, int x, int y, int w, int h) {
        List<String> filtered = filtered();
        int cols = w >= 720 ? 3 : 2;
        int gap = 8;
        int cardW = Math.max(150, (w - gap * (cols - 1)) / cols);
        int rowH = 34;
        int cardH = 29;
        int yy = y - (int) moduleScroll;

        for (int i = 0; i < filtered.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cardW + gap);
            int cy = yy + row * rowH;
            if (cy + cardH < y || cy > y + h) continue;

            String name = filtered.get(i);
            boolean real = implemented(name);
            boolean on = enabled(name);
            boolean sel = name.equals(selected);
            int bg = sel ? 0xD52A4860 : (on ? 0xD01F3E55 : 0xB5131C26);
            int border = sel ? 0xFF5B9FD1 : (real ? 0xFF2B5578 : 0xFF263542);

            d.fill(cx, cy, cx + cardW, cy + cardH, bg);
            d.drawBorder(cx, cy, cardW, cardH, border);
            text(d, name, cx + 10, cy + 9, real ? (on ? colorFor(name) : 0xFFE4EAF1) : 0xFF8B98A5, false);

            // Wurst-style arrow area on the right side of each module card.
            d.fill(cx + cardW - 34, cy + 1, cx + cardW - 33, cy + cardH - 1, 0xFF34516A);
            text(d, "▼", cx + cardW - 22, cy + 9, real ? 0xFFE5EDF5 : 0xFF667582, false);
            if (on) d.fill(cx + cardW - 31, cy + 23, cx + cardW - 10, cy + 25, colorFor(name));
        }
    }

    private void drawSettings(DrawContext d, int x, int y, int w, int h) {
        if (!implemented(selected)) {
            text(d, "Select one of the implemented modules.", x, y + 12, 0xFF8B99A7, false);
            text(d, "Settings are shown here, on the left side", x, y + 31, 0xFF738291, false);
            text(d, "of the Hub as requested.", x, y + 48, 0xFF738291, false);
            return;
        }

        int yy = y - (int) settingsScroll;
        String desc = switch (selected) {
            case "Nuker [Multi]" -> "Automatically breaks blocks around you.";
            case "X-Ray" -> "Shows only selected blocks through the world.";
            case "Fullbright" -> "Makes dark areas bright without a server effect.";
            default -> "Automatically throws useless items and sorts inventory.";
        };
        text(d, selected, x, yy, colorFor(selected), true); yy += 18;
        text(d, desc, x, yy, 0xFF9BA9B7, false); yy += 23;

        rowToggle(d, x, yy, w, "Enable", enabled(selected)); yy += 30;
        rowValue(d, x, yy, w, "Keybind", keyName(selected)); yy += 30;

        switch (selected) {
            case "Nuker [Multi]" -> yy = nukerSettings(d, x, yy, w);
            case "X-Ray" -> yy = xraySettings(d, x, yy, w);
            case "Fullbright" -> yy = fullbrightSettings(d, x, yy, w);
            case "InventoryCleaner" -> yy = cleanerSettings(d, x, yy, w);
        }
    }

    private int nukerSettings(DrawContext d, int x, int y, int w) {
        section(d, x, y, "Mining"); y += 22;
        rowValue(d, x, y, w, "Mode", PeoClient.CFG.nukerMode); y += 30;
        rowValue(d, x, y, w, "Multi", PeoClient.CFG.nukerMulti + " blocks"); y += 30;
        rowValue(d, x, y, w, "Cooldown", PeoClient.CFG.nukerCooldown + " ticks"); y += 30;
        rowValue(d, x, y, w, "Shape", PeoClient.CFG.nukerShape); y += 30;
        rowValue(d, x, y, w, "Range", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.nukerRange)); y += 30;
        rowValue(d, x, y, w, "Sort", PeoClient.CFG.nukerSort); y += 30;

        section(d, x, y, "Filter"); y += 22;
        rowToggle(d, x, y, w, "Filter", PeoClient.CFG.nukerFilter); y += 30;
        rowValue(d, x, y, w, "Filter mode", PeoClient.CFG.nukerWhitelist ? "Whitelist" : "Blacklist"); y += 30;
        rowValue(d, x, y, w, "Edit blocks", PeoClient.CFG.nukerFilterIds.isBlank() ? "Empty list" : "Custom list"); y += 30;

        section(d, x, y, "Mining behavior"); y += 22;
        rowToggle(d, x, y, w, "Raycast", PeoClient.CFG.nukerRaycast); y += 30;
        rowToggle(d, x, y, w, "Flatten", PeoClient.CFG.nukerFlatten); y += 30;
        rowToggle(d, x, y, w, "Rotate", PeoClient.CFG.nukerRotate); y += 30;
        rowToggle(d, x, y, w, "NoParticles", PeoClient.CFG.nukerNoParticles); y += 30;

        section(d, x, y, "Render"); y += 22;
        rowToggle(d, x, y, w, "Highlight", PeoClient.CFG.nukerHighlight); y += 30;
        rowValue(d, x, y, w, "Highlight mode", "Opacity / Expand"); y += 30;
        rowToggle(d, x, y, w, "RangeHighlight", PeoClient.CFG.nukerRangeHighlight); y += 30;
        rowValue(d, x, y, w, "Range width", "3.0");
        return y + 30;
    }

    private int xraySettings(DrawContext d, int x, int y, int w) {
        section(d, x, y, "X-Ray"); y += 22;
        rowValue(d, x, y, w, "Ores / blocks", PeoClient.CFG.xrayBlocks.size() + " selected"); y += 30;
        rowToggle(d, x, y, w, "Only show exposed", PeoClient.CFG.xrayExposedOnly); y += 30;
        rowValue(d, x, y, w, "Opacity", opacityText()); y += 30;
        text(d, "Default list follows Wurst's X-Ray target set.", x, y, 0xFF7F8D9B, false); y += 18;
        text(d, "Includes ores, chests, spawners and utility blocks.", x, y, 0xFF7F8D9B, false); y += 26;
        rowToggle(d, x, y, w, "Fluids", true); y += 30;
        text(d, "Water/lava are treated as X-Ray targets.", x, y, 0xFF7F8D9B, false);
        return y + 28;
    }

    private String opacityText() {
        int a = Math.max(0, Math.min(255, PeoClient.CFG.xrayBackgroundOpacity));
        return a == 0 ? "Off" : Math.round(a * 100f / 255f) + "%";
    }

    private int fullbrightSettings(DrawContext d, int x, int y, int w) {
        section(d, x, y, "Fullbright"); y += 22;
        rowValue(d, x, y, w, "Method", PeoClient.CFG.fullbrightMethod); y += 30;
        rowToggle(d, x, y, w, "Fade", PeoClient.CFG.fullbrightFade); y += 30;
        rowValue(d, x, y, w, "Default brightness",
                String.format(Locale.ROOT, "%.0f%%", PeoClient.CFG.fullbrightDefaultBrightness * 100)); y += 30;
        text(d, "Gamma = forced brightness 1600%.", x, y, 0xFF7F8D9B, false); y += 18;
        text(d, "Night Vision = Wurst-style alternate method.", x, y, 0xFF7F8D9B, false);
        return y + 28;
    }

    private int cleanerSettings(DrawContext d, int x, int y, int w) {
        section(d, x, y, "Inventory cleanup"); y += 22;
        rowToggle(d, x, y, w, "Greedy", PeoClient.CFG.cleanerGreedy); y += 30;
        rowToggle(d, x, y, w, "Merge stacks", PeoClient.CFG.cleanerMergeStacks); y += 30;
        rowToggle(d, x, y, w, "Touch hotbar", PeoClient.CFG.cleanerTouchHotbar); y += 30;
        rowValue(d, x, y, w, "Action delay", PeoClient.CFG.cleanerActionDelay + " ticks"); y += 30;
        rowValue(d, x, y, w, "Ack timeout", PeoClient.CFG.cleanerAckTimeout + " ticks"); y += 30;

        section(d, x, y, "Maximum amounts"); y += 22;
        rowValue(d, x, y, w, "Blocks", String.valueOf(PeoClient.CFG.maxBlocks)); y += 30;
        rowValue(d, x, y, w, "Arrows", String.valueOf(PeoClient.CFG.maxArrows)); y += 30;
        rowValue(d, x, y, w, "Throwables", String.valueOf(PeoClient.CFG.maxThrowables)); y += 30;
        rowValue(d, x, y, w, "Food points", String.valueOf(PeoClient.CFG.maxFoods)); y += 30;
        rowValue(d, x, y, w, "Water buckets", String.valueOf(PeoClient.CFG.maxWaterBuckets)); y += 30;
        rowValue(d, x, y, w, "Lava buckets", String.valueOf(PeoClient.CFG.maxLavaBuckets)); y += 30;
        rowValue(d, x, y, w, "Milk buckets", String.valueOf(PeoClient.CFG.maxMilkBuckets)); y += 30;

        section(d, x, y, "Hotbar / offhand"); y += 22;
        rowValue(d, x, y, w, "OffHandItem", PeoClient.CFG.offHandItem); y += 30;
        for (int i = 0; i < 9; i++) {
            String value = PeoClient.CFG.slotItems[Math.min(i, PeoClient.CFG.slotItems.length - 1)];
            rowValue(d, x, y, w, "SlotItem-" + (i + 1), value); y += 30;
        }
        rowValue(d, x, y, w, "ItemsBlacklist", PeoClient.CFG.itemsBlacklist.isBlank() ? "Empty" : "Configured");
        return y + 30;
    }

    private void section(DrawContext d, int x, int y, String title) {
        d.fill(x, y + 15, x + 100, y + 16, 0xFF294A63);
        text(d, title, x, y, 0xFF7FCFFF, true);
    }

    private void rowValue(DrawContext d, int x, int y, int w, String label, String value) {
        d.fill(x, y, x + w, y + 26, 0xA6131D27);
        text(d, label, x + 8, y + 8, 0xFFE2E8EE, false);
        int tw = textRenderer.getWidth(value);
        text(d, value, x + w - tw - 8, y + 8, 0xFFB9D4E8, false);
        d.drawBorder(x, y, w, 26, 0xFF20394D);
    }

    private void rowToggle(DrawContext d, int x, int y, int w, String label, boolean value) {
        rowValue(d, x, y, w, label, value ? "ON" : "OFF");
        int c = value ? 0xFF48E96A : 0xFF6E7B87;
        d.fill(x + w - 28, y + 8, x + w - 12, y + 18, c);
    }

    private List<String> filtered() {
        String q = query();
        List<String> out = new ArrayList<>();
        for (String n : MODULES) if (q.isBlank() || n.toLowerCase(Locale.ROOT).contains(q)) out.add(n);
        return out;
    }

    private String query() {
        return search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int top = 88, bottom = height - 24;
        int hackX = 14, hackW = 208;
        int settingsX = hackX + hackW + 10, settingsW = 330;
        int mainX = settingsX + settingsW + 12;
        int mainW = width - mainX - 14;

        // Hack list selection.
        if (mouseX >= hackX && mouseX <= hackX + hackW && mouseY >= top + 38 && mouseY < bottom) {
            List<String> list = filtered();
            int rowH = 16;
            int yy = top + 42 - (int) hackScroll;
            for (String name : list) {
                if (mouseY >= yy - 2 && mouseY <= yy + 14) {
                    if (implemented(name)) selected = name;
                    return true;
                }
                yy += rowH;
            }
        }

        // Module grid.
        if (mouseX >= mainX && mouseX <= mainX + mainW && mouseY >= top + 38 && mouseY < bottom) {
            List<String> list = filtered();
            int cols = mainW - 24 >= 720 ? 3 : 2;
            int gap = 8;
            int cardW = Math.max(150, (mainW - 24 - gap * (cols - 1)) / cols);
            int rowH = 34, cardH = 29;
            int yy = top + 42 - (int) moduleScroll;
            for (int i = 0; i < list.size(); i++) {
                int col = i % cols, row = i / cols;
                int cx = mainX + 12 + col * (cardW + gap);
                int cy = yy + row * rowH;
                if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                    String name = list.get(i);
                    if (implemented(name)) {
                        selected = name;
                        if (button == 0) toggle(name);
                    }
                    return true;
                }
            }
        }

        // Settings panel is deliberately LEFT of the module grid.
        if (mouseX >= settingsX && mouseX <= settingsX + settingsW && mouseY >= top + 38 && mouseY < bottom
                && implemented(selected)) {
            int y = top + 42 - (int) settingsScroll;
            if (hit(mouseY, y + 41)) { toggle(selected); return true; }
            y += 71;
            if (hit(mouseY, y)) { openBind(selected); return true; }
            y += 30;
            switch (selected) {
                case "Nuker [Multi]" -> { if (clickNuker(mouseY, y)) return true; }
                case "X-Ray" -> { if (clickXray(mouseY, y)) return true; }
                case "Fullbright" -> { if (clickFullbright(mouseY, y)) return true; }
                case "InventoryCleaner" -> { if (clickCleaner(mouseY, y)) return true; }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickNuker(double my, int y) {
        int i = 0;
        if (hit(my, y + 22 + i++ * 30)) { PeoClient.CFG.nukerMode = cycle(PeoClient.CFG.nukerMode, "Normal", "SurvMulti", "Multi", "Instant"); return save(); }
        if (hit(my, y + 22 + i++ * 30)) { PeoClient.CFG.nukerMulti = next(PeoClient.CFG.nukerMulti, 1, 10); return save(); }
        if (hit(my, y + 22 + i++ * 30)) { PeoClient.CFG.nukerCooldown = next(PeoClient.CFG.nukerCooldown, 0, 4); return save(); }
        if (hit(my, y + 22 + i++ * 30)) { PeoClient.CFG.nukerShape = cycle(PeoClient.CFG.nukerShape, "Cube", "Sphere"); return save(); }
        if (hit(my, y + 22 + i++ * 30)) { PeoClient.CFG.nukerRange = PeoClient.CFG.nukerRange >= 6 ? 1 : PeoClient.CFG.nukerRange + .5; return save(); }
        if (hit(my, y + 22 + i++ * 30)) { PeoClient.CFG.nukerSort = cycle(PeoClient.CFG.nukerSort, "Closest", "Furthest", "Softest", "Hardest", "None"); return save(); }
        if (hit(my, y + 224)) { PeoClient.CFG.nukerFilter = !PeoClient.CFG.nukerFilter; return save(); }
        if (hit(my, y + 254)) { PeoClient.CFG.nukerWhitelist = !PeoClient.CFG.nukerWhitelist; return save(); }
        if (hit(my, y + 336)) { PeoClient.CFG.nukerRaycast = !PeoClient.CFG.nukerRaycast; return save(); }
        if (hit(my, y + 366)) { PeoClient.CFG.nukerFlatten = !PeoClient.CFG.nukerFlatten; return save(); }
        if (hit(my, y + 396)) { PeoClient.CFG.nukerRotate = !PeoClient.CFG.nukerRotate; return save(); }
        if (hit(my, y + 426)) { PeoClient.CFG.nukerNoParticles = !PeoClient.CFG.nukerNoParticles; return save(); }
        if (hit(my, y + 478)) { PeoClient.CFG.nukerHighlight = !PeoClient.CFG.nukerHighlight; return save(); }
        if (hit(my, y + 538)) { PeoClient.CFG.nukerRangeHighlight = !PeoClient.CFG.nukerRangeHighlight; return save(); }
        return false;
    }

    private boolean clickXray(double my, int y) {
        if (hit(my, y + 52)) { PeoClient.CFG.xrayExposedOnly = !PeoClient.CFG.xrayExposedOnly; return save(); }
        if (hit(my, y + 82)) { PeoClient.CFG.xrayBackgroundOpacity = PeoClient.CFG.xrayBackgroundOpacity >= 252 ? 0 : PeoClient.CFG.xrayBackgroundOpacity + 28; return save(); }
        return false;
    }

    private boolean clickFullbright(double my, int y) {
        if (hit(my, y + 22)) { PeoClient.CFG.fullbrightMethod = cycle(PeoClient.CFG.fullbrightMethod, "Gamma", "Night Vision"); return save(); }
        if (hit(my, y + 52)) { PeoClient.CFG.fullbrightFade = !PeoClient.CFG.fullbrightFade; return save(); }
        if (hit(my, y + 82)) { PeoClient.CFG.fullbrightDefaultBrightness = PeoClient.CFG.fullbrightDefaultBrightness >= 1 ? 0 : PeoClient.CFG.fullbrightDefaultBrightness + .1; return save(); }
        return false;
    }

    private boolean clickCleaner(double my, int y) {
        if (hit(my, y + 22)) { PeoClient.CFG.cleanerGreedy = !PeoClient.CFG.cleanerGreedy; return save(); }
        if (hit(my, y + 52)) { PeoClient.CFG.cleanerMergeStacks = !PeoClient.CFG.cleanerMergeStacks; return save(); }
        if (hit(my, y + 82)) { PeoClient.CFG.cleanerTouchHotbar = !PeoClient.CFG.cleanerTouchHotbar; return save(); }
        if (hit(my, y + 112)) { PeoClient.CFG.cleanerActionDelay = next(PeoClient.CFG.cleanerActionDelay, 0, 10); return save(); }
        if (hit(my, y + 142)) { PeoClient.CFG.cleanerAckTimeout = PeoClient.CFG.cleanerAckTimeout >= 60 ? 5 : PeoClient.CFG.cleanerAckTimeout + 5; return save(); }
        return false;
    }

    private boolean hit(double mouseY, int y) { return mouseY >= y && mouseY <= y + 26; }

    private boolean save() { PeoClient.CFG.save(); return true; }

    private int next(int value, int min, int max) { return value >= max ? min : value + 1; }

    private String cycle(String value, String... values) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return values[(i + 1) % values.length];
        return values[0];
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int top = 88, bottom = height - 24;
        int hackX = 14, hackW = 208;
        int settingsX = hackX + hackW + 10, settingsW = 330;
        int mainX = settingsX + settingsW + 12;
        if (mouseY < top || mouseY > bottom) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        if (mouseX >= hackX && mouseX <= hackX + hackW) hackScroll = clampScroll(hackScroll - verticalAmount * 18, 0, filtered().size() * 16 - (height - top - 60));
        else if (mouseX >= settingsX && mouseX <= settingsX + settingsW) settingsScroll = clampScroll(settingsScroll - verticalAmount * 24, 0, Math.max(0, settingsContentHeight() - (height - top - 60)));
        else if (mouseX >= mainX) {
            int cols = width - mainX - 26 >= 720 ? 3 : 2;
            int rows = (int)Math.ceil(filtered().size() / (double)cols);
            moduleScroll = clampScroll(moduleScroll - verticalAmount * 28, 0, rows * 34 - (height - top - 60));
        }
        return true;
    }


    private int settingsContentHeight() {
        return switch (selected) {
            case "Nuker [Multi]" -> 920;
            case "X-Ray" -> 330;
            case "Fullbright" -> 210;
            case "InventoryCleaner" -> 660;
            default -> 120;
        };
    }
    private double clampScroll(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { listening = false; bindTarget = null; return true; }
            setBind(bindTarget, keyCode);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int colorFor(String s) {
        return switch (s) {
            case "Fullbright" -> 0xFF70FF70;
            case "X-Ray" -> 0xFF42D9FF;
            case "Nuker [Multi]" -> 0xFFFFB21C;
            case "InventoryCleaner" -> 0xFFFF63E8;
            default -> 0xFFE5ECF3;
        };
    }
}
