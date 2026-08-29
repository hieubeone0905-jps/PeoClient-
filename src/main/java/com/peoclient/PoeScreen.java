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
 * PeoClient Hub.
 *
 * The layout is intentionally simple: Hack List on the left, module settings in
 * the center-left, and the module list on the right. Every scrolling surface is
 * clipped to its own panel so settings can never draw over the header or another
 * panel.
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
        int searchW = Math.min(420, Math.max(260, width - 520));
        search = new TextFieldWidget(textRenderer, width / 2 - searchW / 2, 18, searchW, 22,
                Text.literal("Search"));
        search.setMaxLength(64);
        search.setPlaceholder(Text.literal("Search modules..."));
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

    private void select(String name) {
        if (!name.equals(selected)) {
            selected = name;
            settingsScroll = 0;
        }
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
        d.fill(0, 0, width, height, 0xD90A0F14);
        d.fill(0, 0, width, 74, 0xF20B1118);
        d.fill(0, 72, width, 75, 0xFF315873);

        drawText(d, "P", 18, 20, 0xFFFFFFFF, true);
        drawText(d, "PeoClient", 42, 16, 0xFFFFFFFF, true);
        drawText(d, "1.21.4", 42, 34, 0xFFB7C1CA, false);
        drawText(d, "Right Shift", width - 120, 18, 0xFFB7C1CA, false);
        drawText(d, listening ? "Press a key (ESC cancels)" : "Left = toggle   •   Right = settings",
                width - 320, 42, listening ? 0xFFFFFFFF : 0xFF9CA8B2, false);

        super.render(d, mouseX, mouseY, delta);

        int top = 90;
        int bottom = height - 24;
        int gap = 12;
        int leftW = Math.min(310, Math.max(260, width / 4));
        int rightW = Math.min(230, Math.max(210, width / 6));
        int settingsX = 14 + leftW + gap;
        int rightX = width - rightW - 14;
        int settingsW = Math.max(360, rightX - settingsX - gap);

        panel(d, 14, top, leftW, bottom - top, 0xD20D151D, 0xFF2A4154);
        panel(d, settingsX, top, settingsW, bottom - top, 0xD20D151D, 0xFF2A4154);
        panel(d, rightX, top, rightW, bottom - top, 0xD20D151D, 0xFF2A4154);

        drawText(d, "HACK LIST", 14 + 16, top + 18, 0xFFFFFFFF, true);
        drawText(d, "SETTINGS", settingsX + 16, top + 18, 0xFFFFFFFF, true);
        drawText(d, "MODULES", rightX + 14, top + 18, 0xFFFFFFFF, true);

        int listTop = top + 40;
        int listBottom = bottom - 8;
        d.enableScissor(14 + 1, listTop, 14 + leftW - 1, listBottom);
        drawHackList(d, 14 + 14, listTop, leftW - 28, listBottom - listTop);
        d.disableScissor();

        d.enableScissor(settingsX + 1, listTop, settingsX + settingsW - 1, listBottom);
        drawSettings(d, settingsX + 14, listTop, settingsW - 28, listBottom - listTop);
        d.disableScissor();

        d.enableScissor(rightX + 1, listTop, rightX + rightW - 1, listBottom);
        drawModules(d, rightX + 12, listTop, rightW - 24, listBottom - listTop);
        d.disableScissor();

        drawText(d, "Wheel: scroll panel", 14, height - 16, 0xFF73808B, false);
        drawText(d, "ESC: close", width - 80, height - 16, 0xFF73808B, false);
    }

    private void panel(DrawContext d, int x, int y, int w, int h, int fill, int border) {
        d.fill(x, y, x + w, y + h, fill);
        d.drawBorder(x, y, w, h, border);
    }

    private void drawText(DrawContext d, String s, int x, int y, int color, boolean bold) {
        d.drawTextWithShadow(textRenderer, s, x, y, color);
        if (bold) d.drawTextWithShadow(textRenderer, s, x + 1, y, color);
    }

    private void drawHackList(DrawContext d, int x, int y, int w, int h) {
        List<String> filtered = filtered();
        int rowH = 20;
        int yy = y - (int) hackScroll;
        for (String name : filtered) {
            if (yy + rowH >= y && yy <= y + h) {
                boolean real = implemented(name);
                boolean on = enabled(name);
                if (name.equals(selected)) d.fill(x - 8, yy - 2, x + w, yy + 17, 0xFF1C3547);
                drawText(d, name, x, yy + 2, on || real ? 0xFFFFFFFF : 0xFF7E8A94, on || name.equals(selected));
                if (on) d.fill(x + w - 8, yy + 6, x + w - 2, yy + 12, 0xFFFFFFFF);
            }
            yy += rowH;
        }
    }

    private void drawModules(DrawContext d, int x, int y, int w, int h) {
        List<String> filtered = filtered();
        int rowH = 34;
        int cardH = 30;
        int yy = y - (int) moduleScroll;
        for (String name : filtered) {
            if (yy + cardH >= y && yy <= y + h) {
                boolean real = implemented(name);
                boolean on = enabled(name);
                boolean sel = name.equals(selected);
                int bg = sel ? 0xFF233E52 : (on ? 0xFF193040 : 0xA8141D24);
                int border = sel ? 0xFF6A91AD : 0xFF294354;
                d.fill(x, yy, x + w, yy + cardH, bg);
                d.drawBorder(x, yy, w, cardH, border);
                drawText(d, name, x + 10, yy + 9, real ? 0xFFFFFFFF : 0xFF7E8A94, real && (on || sel));
                if (on) d.fill(x + 8, yy + 26, x + w - 8, yy + 28, 0xFFFFFFFF);
                drawText(d, real ? "»" : "·", x + w - 18, yy + 8, 0xFFBAC4CC, false);
            }
            yy += rowH;
        }
    }

    private void drawSettings(DrawContext d, int x, int y, int w, int h) {
        if (!implemented(selected)) {
            drawText(d, "Select an implemented module.", x, y + 18, 0xFFFFFFFF, true);
            drawText(d, "The settings panel stays here and", x, y + 40, 0xFF9AA7B1, false);
            drawText(d, "never renders outside its bounds.", x, y + 58, 0xFF9AA7B1, false);
            return;
        }

        int yy = y - (int) settingsScroll;
        drawText(d, selected, x, yy + 2, 0xFFFFFFFF, true); yy += 20;
        drawText(d, description(selected), x, yy, 0xFF9AA7B1, false); yy += 24;

        rowToggle(d, x, yy, w, "Enable", enabled(selected)); yy += 34;
        rowValue(d, x, yy, w, "Keybind", keyName(selected)); yy += 34;

        switch (selected) {
            case "Nuker [Multi]" -> drawNuker(d, x, yy, w);
            case "X-Ray" -> drawXray(d, x, yy, w);
            case "Fullbright" -> drawFullbright(d, x, yy, w);
            case "InventoryCleaner" -> drawCleaner(d, x, yy, w);
        }
    }

    private String description(String name) {
        return switch (name) {
            case "Nuker [Multi]" -> "Breaks blocks around you.";
            case "X-Ray" -> "Shows selected blocks through the world.";
            case "Fullbright" -> "Keeps dark areas bright.";
            default -> "Sorts and removes unwanted inventory items.";
        };
    }

    private void drawNuker(DrawContext d, int x, int y, int w) {
        int yy = y;
        yy = section(d, x, yy, "Mining");
        yy = rowValue(d, x, yy, w, "Mode", PeoClient.CFG.nukerMode);
        yy = rowValue(d, x, yy, w, "Multi", PeoClient.CFG.nukerMulti + " blocks");
        yy = rowValue(d, x, yy, w, "Cooldown", PeoClient.CFG.nukerCooldown + " ticks");
        yy = rowValue(d, x, yy, w, "Shape", PeoClient.CFG.nukerShape);
        yy = rowValue(d, x, yy, w, "Range", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.nukerRange));
        yy = rowValue(d, x, yy, w, "Sort", PeoClient.CFG.nukerSort);
        yy = section(d, x, yy + 4, "Filter");
        yy = rowToggle(d, x, yy, w, "Filter", PeoClient.CFG.nukerFilter);
        yy = rowValue(d, x, yy, w, "Filter mode", PeoClient.CFG.nukerWhitelist ? "Whitelist" : "Blacklist");
        yy = rowValue(d, x, yy, w, "Edit blocks", PeoClient.CFG.nukerFilterIds.isBlank() ? "Empty list" : "Custom list");
        yy = section(d, x, yy + 4, "Mining behavior");
        yy = rowToggle(d, x, yy, w, "Raycast", PeoClient.CFG.nukerRaycast);
        yy = rowToggle(d, x, yy, w, "Flatten", PeoClient.CFG.nukerFlatten);
        yy = rowToggle(d, x, yy, w, "Rotate", PeoClient.CFG.nukerRotate);
        yy = rowToggle(d, x, yy, w, "No particles", PeoClient.CFG.nukerNoParticles);
        yy = section(d, x, yy + 4, "Render");
        yy = rowToggle(d, x, yy, w, "Highlight", PeoClient.CFG.nukerHighlight);
        yy = rowValue(d, x, yy, w, "Highlight mode", "Opacity / expand");
        yy = rowToggle(d, x, yy, w, "Range highlight", PeoClient.CFG.nukerRangeHighlight);
        rowValue(d, x, yy, w, "Range width", "3.0");
    }

    private void drawXray(DrawContext d, int x, int y, int w) {
        int yy = section(d, x, y, "X-Ray");
        yy = rowValue(d, x, yy, w, "Blocks", PeoClient.CFG.xrayBlocks.size() + " selected");
        yy = rowToggle(d, x, yy, w, "Only show exposed", PeoClient.CFG.xrayExposedOnly);
        yy = rowValue(d, x, yy, w, "Opacity", opacityText());
        yy = rowToggle(d, x, yy, w, "Fluids", PeoClient.CFG.xrayFluids);
        drawText(d, "Tip: right-click the module to select settings.", x, yy + 6, 0xFF7F8B95, false);
    }

    private String opacityText() {
        int a = Math.max(0, Math.min(255, PeoClient.CFG.xrayBackgroundOpacity));
        return a == 0 ? "Off" : Math.round(a * 100f / 255f) + "%";
    }

    private void drawFullbright(DrawContext d, int x, int y, int w) {
        int yy = section(d, x, y, "Fullbright");
        yy = rowValue(d, x, yy, w, "Method", PeoClient.CFG.fullbrightMethod);
        yy = rowToggle(d, x, yy, w, "Fade", PeoClient.CFG.fullbrightFade);
        rowValue(d, x, yy, w, "Default brightness",
                String.format(Locale.ROOT, "%.0f%%", PeoClient.CFG.fullbrightDefaultBrightness * 100));
    }

    private void drawCleaner(DrawContext d, int x, int y, int w) {
        int yy = section(d, x, y, "Inventory cleanup");
        yy = rowToggle(d, x, yy, w, "Greedy", PeoClient.CFG.cleanerGreedy);
        yy = rowToggle(d, x, yy, w, "Merge stacks", PeoClient.CFG.cleanerMergeStacks);
        yy = rowToggle(d, x, yy, w, "Touch hotbar", PeoClient.CFG.cleanerTouchHotbar);
        yy = rowValue(d, x, yy, w, "Action delay", PeoClient.CFG.cleanerActionDelay + " ticks");
        yy = rowValue(d, x, yy, w, "Ack timeout", PeoClient.CFG.cleanerAckTimeout + " ticks");
        yy = section(d, x, yy + 4, "Maximum amounts");
        yy = rowValue(d, x, yy, w, "Blocks", String.valueOf(PeoClient.CFG.maxBlocks));
        yy = rowValue(d, x, yy, w, "Arrows", String.valueOf(PeoClient.CFG.maxArrows));
        yy = rowValue(d, x, yy, w, "Throwables", String.valueOf(PeoClient.CFG.maxThrowables));
        yy = rowValue(d, x, yy, w, "Food points", String.valueOf(PeoClient.CFG.maxFoods));
        yy = rowValue(d, x, yy, w, "Water buckets", String.valueOf(PeoClient.CFG.maxWaterBuckets));
        yy = rowValue(d, x, yy, w, "Lava buckets", String.valueOf(PeoClient.CFG.maxLavaBuckets));
        yy = rowValue(d, x, yy, w, "Milk buckets", String.valueOf(PeoClient.CFG.maxMilkBuckets));
        yy = section(d, x, yy + 4, "Hotbar / offhand");
        yy = rowValue(d, x, yy, w, "Off hand", PeoClient.CFG.offHandItem);
        for (int i = 0; i < 9; i++) {
            String value = PeoClient.CFG.slotItems[Math.min(i, PeoClient.CFG.slotItems.length - 1)];
            yy = rowValue(d, x, yy, w, "Slot " + (i + 1), value);
        }
        rowValue(d, x, yy, w, "Items blacklist", PeoClient.CFG.itemsBlacklist.isBlank() ? "Empty" : "Configured");
    }

    private int section(DrawContext d, int x, int y, String title) {
        drawText(d, title, x, y + 2, 0xFFFFFFFF, true);
        d.fill(x, y + 18, x + Math.min(150, 100 + textRenderer.getWidth(title) / 2), y + 19, 0xFF3B586D);
        return y + 24;
    }

    private int rowValue(DrawContext d, int x, int y, int w, String label, String value) {
        d.fill(x, y, x + w, y + 28, 0xB7152029);
        d.drawBorder(x, y, w, 28, 0xFF294354);
        drawText(d, label, x + 10, y + 9, 0xFFFFFFFF, false);
        int tw = textRenderer.getWidth(value);
        drawText(d, value, Math.max(x + 10, x + w - tw - 10), y + 9, 0xFFDDE5EB, false);
        return y + 34;
    }

    private int rowToggle(DrawContext d, int x, int y, int w, String label, boolean value) {
        int next = rowValue(d, x, y, w, label, value ? "ON" : "OFF");
        int tx = x + w - 42;
        int color = value ? 0xFFFFFFFF : 0xFF6E7A84;
        d.fill(tx, y + 8, tx + 26, y + 20, color);
        drawText(d, value ? "ON" : "OFF", tx + 3, y + 9, value ? 0xFF0A1014 : 0xFFFFFFFF, true);
        return next;
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

    private int[] layout() {
        int gap = 12;
        int leftW = Math.min(310, Math.max(260, width / 4));
        int rightW = Math.min(230, Math.max(210, width / 6));
        int settingsX = 14 + leftW + gap;
        int rightX = width - rightW - 14;
        int settingsW = Math.max(360, rightX - settingsX - gap);
        return new int[]{14, leftW, settingsX, settingsW, rightX, rightW, 90, height - 24};
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        int[] l = layout();
        int leftX = l[0], leftW = l[1], settingsX = l[2], settingsW = l[3], rightX = l[4], rightW = l[5], top = l[6], bottom = l[7];
        int listTop = top + 40;

        if (mouseY < listTop || mouseY >= bottom) return super.mouseClicked(mouseX, mouseY, button);

        if (mouseX >= leftX && mouseX <= leftX + leftW) {
            int rowH = 20;
            int yy = listTop - (int) hackScroll;
            for (String name : filtered()) {
                if (mouseY >= yy && mouseY < yy + rowH) {
                    if (implemented(name)) select(name);
                    return true;
                }
                yy += rowH;
            }
            return true;
        }

        if (mouseX >= rightX && mouseX <= rightX + rightW) {
            int rowH = 34;
            int yy = listTop - (int) moduleScroll;
            for (String name : filtered()) {
                if (mouseY >= yy && mouseY < yy + 30) {
                    if (implemented(name)) {
                        select(name);
                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) toggle(name);
                    }
                    return true;
                }
                yy += rowH;
            }
            return true;
        }

        if (mouseX >= settingsX && mouseX <= settingsX + settingsW && implemented(selected)) {
            int y = listTop - (int) settingsScroll;
            if (mouseY >= y + 44 && mouseY < y + 72) { toggle(selected); return true; }
            if (mouseY >= y + 78 && mouseY < y + 106) { openBind(selected); return true; }

            switch (selected) {
                case "Nuker [Multi]" -> clickNuker(mouseY, y + 112, settingsW);
                case "X-Ray" -> clickXray(mouseY, y + 112);
                case "Fullbright" -> clickFullbright(mouseY, y + 112);
                case "InventoryCleaner" -> clickCleaner(mouseY, y + 112);
            }
            return true;
        }

        return true;
    }

    private void clickNuker(double my, int y, int w) {
        int p = y + 24; // first row after the Mining section heading
        if (hit(my, p)) { PeoClient.CFG.nukerMode = cycle(PeoClient.CFG.nukerMode, "Normal", "SurvMulti", "Multi", "Instant"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerMulti = next(PeoClient.CFG.nukerMulti, 1, 10); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerCooldown = next(PeoClient.CFG.nukerCooldown, 0, 4); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerShape = cycle(PeoClient.CFG.nukerShape, "Cube", "Sphere"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerRange = PeoClient.CFG.nukerRange >= 6 ? 1 : PeoClient.CFG.nukerRange + .5; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerSort = cycle(PeoClient.CFG.nukerSort, "Closest", "Furthest", "Softest", "Hardest", "None"); save(); return; } p += 38;
        p += 24;
        if (hit(my, p)) { PeoClient.CFG.nukerFilter = !PeoClient.CFG.nukerFilter; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerWhitelist = !PeoClient.CFG.nukerWhitelist; save(); return; } p += 68;
        p += 4;
        if (hit(my, p)) { PeoClient.CFG.nukerRaycast = !PeoClient.CFG.nukerRaycast; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerFlatten = !PeoClient.CFG.nukerFlatten; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerRotate = !PeoClient.CFG.nukerRotate; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerNoParticles = !PeoClient.CFG.nukerNoParticles; save(); return; } p += 62;
        if (hit(my, p)) { PeoClient.CFG.nukerHighlight = !PeoClient.CFG.nukerHighlight; save(); return; } p += 68;
        if (hit(my, p)) { PeoClient.CFG.nukerRangeHighlight = !PeoClient.CFG.nukerRangeHighlight; save(); }
    }

    private void clickXray(double my, int y) {
        int p = y;
        p += 58;
        if (hit(my, p)) { PeoClient.CFG.xrayExposedOnly = !PeoClient.CFG.xrayExposedOnly; save(); return; }
        p += 34;
        if (hit(my, p)) { PeoClient.CFG.xrayBackgroundOpacity = PeoClient.CFG.xrayBackgroundOpacity >= 252 ? 0 : PeoClient.CFG.xrayBackgroundOpacity + 28; save(); return; }
        p += 34;
        if (hit(my, p)) { PeoClient.CFG.xrayFluids = !PeoClient.CFG.xrayFluids; save(); }
    }

    private void clickFullbright(double my, int y) {
        int p = y + 24;
        if (hit(my, p)) { PeoClient.CFG.fullbrightMethod = cycle(PeoClient.CFG.fullbrightMethod, "Gamma", "Night Vision"); save(); return; }
        p += 34;
        if (hit(my, p)) { PeoClient.CFG.fullbrightFade = !PeoClient.CFG.fullbrightFade; save(); return; }
        p += 34;
        if (hit(my, p)) { PeoClient.CFG.fullbrightDefaultBrightness = PeoClient.CFG.fullbrightDefaultBrightness >= 1 ? 0 : PeoClient.CFG.fullbrightDefaultBrightness + .1; save(); }
    }

    private void clickCleaner(double my, int y) {
        int p = y + 24;
        for (int i = 0; i < 3; i++) {
            if (hit(my, p)) {
                if (i == 0) PeoClient.CFG.cleanerGreedy = !PeoClient.CFG.cleanerGreedy;
                if (i == 1) PeoClient.CFG.cleanerMergeStacks = !PeoClient.CFG.cleanerMergeStacks;
                if (i == 2) PeoClient.CFG.cleanerTouchHotbar = !PeoClient.CFG.cleanerTouchHotbar;
                save(); return;
            }
            p += 34;
        }
        if (hit(my, p)) { PeoClient.CFG.cleanerActionDelay = next(PeoClient.CFG.cleanerActionDelay, 0, 10); save(); return; }
        p += 34;
        if (hit(my, p)) { PeoClient.CFG.cleanerAckTimeout = PeoClient.CFG.cleanerAckTimeout >= 60 ? 5 : PeoClient.CFG.cleanerAckTimeout + 5; save(); }
    }

    private boolean hit(double mouseY, int y) { return mouseY >= y && mouseY < y + 28; }
    private void save() { PeoClient.CFG.save(); }
    private int next(int value, int min, int max) { return value >= max ? min : value + 1; }

    private String cycle(String value, String... values) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return values[(i + 1) % values.length];
        return values[0];
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int[] l = layout();
        int leftX = l[0], leftW = l[1], settingsX = l[2], settingsW = l[3], rightX = l[4], rightW = l[5], top = l[6], bottom = l[7];
        if (mouseY < top + 40 || mouseY > bottom) return true;
        if (mouseX >= leftX && mouseX <= leftX + leftW) {
            double max = Math.max(0, filtered().size() * 20 - (bottom - top - 48));
            hackScroll = clampScroll(hackScroll - verticalAmount * 26, 0, max);
        } else if (mouseX >= settingsX && mouseX <= settingsX + settingsW) {
            double max = Math.max(0, settingsContentHeight() - (bottom - top - 48));
            settingsScroll = clampScroll(settingsScroll - verticalAmount * 30, 0, max);
        } else if (mouseX >= rightX && mouseX <= rightX + rightW) {
            double max = Math.max(0, filtered().size() * 34 - (bottom - top - 48));
            moduleScroll = clampScroll(moduleScroll - verticalAmount * 30, 0, max);
        }
        return true;
    }

    private int settingsContentHeight() {
        return switch (selected) {
            case "Nuker [Multi]" -> 760;
            case "X-Ray" -> 240;
            case "Fullbright" -> 170;
            case "InventoryCleaner" -> 910;
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
}
