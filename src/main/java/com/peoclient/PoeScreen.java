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

/** Wurst-inspired, restrained PeoClient hub with independently clipped panels. */
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
        search = new TextFieldWidget(textRenderer, width / 2 - searchW / 2, 17, searchW, 22,
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
            moduleScroll = 0;
        }
    }

    private void openBind(String module) {
        if (!implemented(module)) return;
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
        drawText(d, "1.21.4", 42, 34, 0xFFAAB5BE, false);
        drawText(d, "Right Shift • Hub", width - 190, 18, 0xFFFFFFFF, false);
        drawText(d, listening ? "Press a key (ESC cancels)" : "Left click = toggle  •  Right click = settings",
                width - 360, 42, 0xFFB6C0C8, false);

        super.render(d, mouseX, mouseY, delta);

        int[] l = layout();
        int leftX = l[0], leftW = l[1], settingsX = l[2], settingsW = l[3], rightX = l[4], rightW = l[5], top = l[6], bottom = l[7];
        int panelH = bottom - top;
        panel(d, leftX, top, leftW, panelH);
        panel(d, settingsX, top, settingsW, panelH);
        panel(d, rightX, top, rightW, panelH);

        drawText(d, "HACK LIST", leftX + 16, top + 18, 0xFFFFFFFF, true);
        drawText(d, "SETTINGS", settingsX + 16, top + 18, 0xFFFFFFFF, true);
        drawText(d, "MODULES", rightX + 14, top + 18, 0xFFFFFFFF, true);

        int listTop = top + 40;
        int listBottom = bottom - 8;
        d.enableScissor(leftX + 1, listTop, leftX + leftW - 1, listBottom);
        drawHackList(d, leftX + 14, listTop, leftW - 28, listBottom - listTop);
        d.disableScissor();

        d.enableScissor(settingsX + 1, listTop, settingsX + settingsW - 1, listBottom);
        drawSettings(d, settingsX + 14, listTop, settingsW - 28);
        d.disableScissor();

        d.enableScissor(rightX + 1, listTop, rightX + rightW - 1, listBottom);
        drawModules(d, rightX + 12, listTop, rightW - 24, listBottom - listTop);
        d.disableScissor();

        drawText(d, "Wheel: scroll hovered panel", 14, height - 16, 0xFF73808B, false);
        drawText(d, "ESC: close", width - 80, height - 16, 0xFF73808B, false);
    }

    private void panel(DrawContext d, int x, int y, int w, int h) {
        d.fill(x, y, x + w, y + h, 0xD20D151D);
        d.drawBorder(x, y, w, h, 0xFF2A4154);
    }

    private void drawText(DrawContext d, String s, int x, int y, int color, boolean bold) {
        d.drawText(textRenderer,
                Text.literal(s).styled(style -> style.withBold(bold)),
                x, y, color);
    }

    private void drawHackList(DrawContext d, int x, int y, int w, int h) {
        int rowH = 20;
        int yy = y - (int) hackScroll;
        for (String name : filtered()) {
            if (yy + rowH >= y && yy <= y + h) {
                boolean real = implemented(name);
                boolean on = enabled(name);
                if (name.equals(selected)) d.fill(x - 8, yy - 2, x + w, yy + 17, 0xFF1C3547);
                drawText(d, name, x, yy + 2, real ? 0xFFFFFFFF : 0xFF7E8A94, real && (on || name.equals(selected)));
                if (on) d.fill(x + w - 8, yy + 6, x + w - 2, yy + 12, 0xFFFFFFFF);
            }
            yy += rowH;
        }
    }

    private void drawModules(DrawContext d, int x, int y, int w, int h) {
        int rowH = 34;
        int cardH = 30;
        int yy = y - (int) moduleScroll;
        for (String name : filtered()) {
            if (yy + cardH >= y && yy <= y + h) {
                boolean real = implemented(name);
                boolean on = enabled(name);
                boolean sel = name.equals(selected);
                d.fill(x, yy, x + w, yy + cardH, sel ? 0xFF233E52 : 0xA8141D24);
                d.drawBorder(x, yy, w, cardH, sel ? 0xFF6A91AD : 0xFF294354);
                drawText(d, name, x + 10, yy + 9, real ? 0xFFFFFFFF : 0xFF7E8A94, real && (on || sel));
                if (on) drawText(d, "ON", x + w - 28, yy + 9, 0xFFFFFFFF, true);
            }
            yy += rowH;
        }
    }

    private void drawSettings(DrawContext d, int x, int y, int w) {
        if (!implemented(selected)) {
            drawText(d, "Select an implemented module.", x, y + 18, 0xFFFFFFFF, true);
            drawText(d, "This panel has its own scroll and clipping.", x, y + 40, 0xFF98A5AE, false);
            return;
        }

        int yy = y - (int) settingsScroll;
        drawText(d, selected, x, yy + 2, 0xFFFFFFFF, true);
        yy += 20;
        drawText(d, description(selected), x, yy, 0xFF98A5AE, false);
        yy += 26;
        yy = rowToggle(d, x, yy, w, "Enable", enabled(selected));
        yy = rowValue(d, x, yy, w, "Keybind", keyName(selected));

        switch (selected) {
            case "Nuker [Multi]" -> drawNuker(d, x, yy, w);
            case "InventoryCleaner" -> drawCleaner(d, x, yy, w);
            case "X-Ray" -> drawXray(d, x, yy, w);
            case "Fullbright" -> drawFullbright(d, x, yy, w);
        }
    }

    private int drawNuker(DrawContext d, int x, int y, int w) {
        y = section(d, x, y, "Mining");
        y = rowValue(d, x, y, w, "Mode", PeoClient.CFG.nukerMode);
        y = rowValue(d, x, y, w, "Multi", PeoClient.CFG.nukerMulti + " blocks");
        y = rowValue(d, x, y, w, "Cooldown", PeoClient.CFG.nukerCooldown + " ticks");
        y = rowValue(d, x, y, w, "Shape", PeoClient.CFG.nukerShape);
        y = rowValue(d, x, y, w, "Range", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.nukerRange));
        y = rowValue(d, x, y, w, "Sort", PeoClient.CFG.nukerSort);

        y = section(d, x, y, "Filter");
        y = rowToggle(d, x, y, w, "Filter", PeoClient.CFG.nukerFilter);
        y = rowValue(d, x, y, w, "Filter mode", PeoClient.CFG.nukerWhitelist ? "Whitelist" : "Blacklist");
        y = rowValue(d, x, y, w, "Edit blocks", shortList(PeoClient.CFG.nukerFilterIds));

        y = section(d, x, y, "Mining behaviour");
        y = rowToggle(d, x, y, w, "Raycast", PeoClient.CFG.nukerRaycast);
        y = rowToggle(d, x, y, w, "Flatten", PeoClient.CFG.nukerFlatten);
        y = rowToggle(d, x, y, w, "Rotate", PeoClient.CFG.nukerRotate);
        y = rowToggle(d, x, y, w, "NoParticles", PeoClient.CFG.nukerNoParticles);

        y = section(d, x, y, "Highlight");
        y = rowToggle(d, x, y, w, "Highlight", PeoClient.CFG.nukerHighlight);
        y = rowValue(d, x, y, w, "Mode", PeoClient.CFG.nukerHighlightMode);
        y = rowValue(d, x, y, w, "Color", PeoClient.CFG.nukerHighlightColor);
        y = rowToggle(d, x, y, w, "RangeHighlight", PeoClient.CFG.nukerRangeHighlight);
        y = rowValue(d, x, y, w, "Width", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.nukerRangeWidth));
        y = rowValue(d, x, y, w, "Range color", PeoClient.CFG.nukerRangeColor);
        return y;
    }

    private int drawCleaner(DrawContext d, int x, int y, int w) {
        y = section(d, x, y, "Inventory quotas");
        y = rowValue(d, x, y, w, "MaximumBlocks", String.valueOf(PeoClient.CFG.maxBlocks));
        y = rowValue(d, x, y, w, "MaximumArrows", String.valueOf(PeoClient.CFG.maxArrows));
        y = rowValue(d, x, y, w, "MaximumThrowables", String.valueOf(PeoClient.CFG.maxThrowables));
        y = rowValue(d, x, y, w, "MaximumFoodPoints", String.valueOf(PeoClient.CFG.maxFoods));
        y = rowValue(d, x, y, w, "MaximumWaterBuckets", String.valueOf(PeoClient.CFG.maxWaterBuckets));
        y = rowValue(d, x, y, w, "MaximumLavaBuckets", String.valueOf(PeoClient.CFG.maxLavaBuckets));
        y = rowValue(d, x, y, w, "MaximumMilkBuckets", String.valueOf(PeoClient.CFG.maxMilkBuckets));
        y = rowValue(d, x, y, w, "ItemsBlacklist", shortList(PeoClient.CFG.itemsBlacklist));
        y = rowToggle(d, x, y, w, "Greedy", PeoClient.CFG.cleanerGreedy);

        y = section(d, x, y, "Offhand");
        y = rowValue(d, x, y, w, "OffHandItem", PeoClient.CFG.offHandItem);

        y = section(d, x, y, "Hotbar slots");
        String[] slots = PeoClient.CFG.slotItems;
        for (int i = 0; i < 9; i++) y = rowValue(d, x, y, w, "SlotItem-" + (i + 1), i < slots.length ? safe(slots[i], "NONE") : "NONE");

        y = section(d, x, y, "PeoClient extensions");
        y = rowToggle(d, x, y, w, "Merge partial stacks", PeoClient.CFG.cleanerMergeStacks);
        y = rowValue(d, x, y, w, "Action delay", PeoClient.CFG.cleanerActionDelay + " ticks");
        y = rowValue(d, x, y, w, "Server ack timeout", PeoClient.CFG.cleanerAckTimeout + " ticks");
        y = rowToggle(d, x, y, w, "Touch hotbar", PeoClient.CFG.cleanerTouchHotbar);
        return y;
    }

    private int drawXray(DrawContext d, int x, int y, int w) {
        y = section(d, x, y, "Visibility");
        y = rowToggle(d, x, y, w, "Exposed only", PeoClient.CFG.xrayExposedOnly);
        y = rowToggle(d, x, y, w, "Fluids", PeoClient.CFG.xrayFluids);
        y = rowValue(d, x, y, w, "Background opacity", PeoClient.CFG.xrayBackgroundOpacity + "/255");
        y = rowValue(d, x, y, w, "FullBright", PeoClient.CFG.xrayFullBright ? "ON" : "OFF");
        return y;
    }

    private int drawFullbright(DrawContext d, int x, int y, int w) {
        y = section(d, x, y, "Brightness");
        y = rowValue(d, x, y, w, "Method", PeoClient.CFG.fullbrightMethod);
        y = rowToggle(d, x, y, w, "Fade", PeoClient.CFG.fullbrightFade);
        y = rowValue(d, x, y, w, "Default brightness", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.fullbrightDefaultBrightness));
        y = rowValue(d, x, y, w, "Brightness", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.fullbrightBrightness));
        return y;
    }

    private int section(DrawContext d, int x, int y, String title) {
        drawText(d, title, x, y + 2, 0xFFFFFFFF, true);
        d.fill(x, y + 18, x + Math.min(170, 88 + textRenderer.getWidth(title)), y + 19, 0xFF3B586D);
        return y + 26;
    }

    private int rowValue(DrawContext d, int x, int y, int w, String label, String value) {
        d.fill(x, y, x + w, y + 28, 0xB7152029);
        d.drawBorder(x, y, w, 28, 0xFF294354);
        drawText(d, label, x + 10, y + 9, 0xFFFFFFFF, false);
        int tw = textRenderer.getWidth(value);
        drawText(d, value, Math.max(x + 100, x + w - tw - 10), y + 9, 0xFFE1E6EA, false);
        return y + 34;
    }

    private int rowToggle(DrawContext d, int x, int y, int w, String label, boolean value) {
        rowValue(d, x, y, w, label, "");
        int bx = x + w - 39;
        d.fill(bx, y + 7, bx + 30, y + 21, value ? 0xFFFFFFFF : 0xFF697782);
        drawText(d, value ? "ON" : "OFF", bx + 4, y + 9, value ? 0xFF0A1014 : 0xFFFFFFFF, true);
        return y + 34;
    }

    private String description(String module) {
        return switch (module) {
            case "Nuker [Multi]" -> "Automatically breaks blocks around you.";
            case "InventoryCleaner" -> "Cleans and sorts your inventory automatically.";
            case "Fullbright" -> "Makes dark areas bright.";
            case "X-Ray" -> "Shows selected blocks through the world.";
            default -> "";
        };
    }

    private String shortList(String value) {
        if (value == null || value.isBlank()) return "Empty list";
        return value.length() <= 28 ? value : value.substring(0, 25) + "...";
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
        int margin = 18;
        int gap = 14;
        int usable = width - margin * 2 - gap * 2;
        int leftW = Math.max(250, (int)(usable * 0.25));
        int rightW = Math.max(250, (int)(usable * 0.22));
        int settingsW = Math.max(360, usable - leftW - rightW);
        int settingsX = margin + leftW + gap;
        int rightX = settingsX + settingsW + gap;
        return new int[]{margin, leftW, settingsX, settingsW, rightX, rightW, 92, height - 28};
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
                        else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) settingsScroll = 0;
                    }
                    return true;
                }
                yy += rowH;
            }
            return true;
        }

        if (mouseX >= settingsX && mouseX <= settingsX + settingsW && implemented(selected)) {
            int y = listTop - (int) settingsScroll;
            int firstRowsTop = y + 46;
            if (hit(mouseY, firstRowsTop)) { toggle(selected); return true; }
            if (hit(mouseY, firstRowsTop + 34)) { openBind(selected); return true; }
            int contentY = firstRowsTop + 68;
            switch (selected) {
                case "Nuker [Multi]" -> clickNuker(mouseY, contentY);
                case "InventoryCleaner" -> clickCleaner(mouseY, contentY);
                case "X-Ray" -> clickXray(mouseY, contentY);
                case "Fullbright" -> clickFullbright(mouseY, contentY);
            }
            return true;
        }
        return true;
    }

    private void clickNuker(double my, int y) {
        int p = y + 26; // Mining heading
        if (hit(my, p)) { PeoClient.CFG.nukerMode = cycle(PeoClient.CFG.nukerMode, "Normal", "SurvMulti", "Multi", "Instant"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerMulti = step(PeoClient.CFG.nukerMulti, 1, 10); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerCooldown = step(PeoClient.CFG.nukerCooldown, 0, 4); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerShape = cycle(PeoClient.CFG.nukerShape, "Cube", "Sphere"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerRange = PeoClient.CFG.nukerRange >= 6.0 ? 1.0 : PeoClient.CFG.nukerRange + 0.5; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerSort = cycle(PeoClient.CFG.nukerSort, "Closest", "Furthest", "Softest", "Hardest", "None"); save(); return; } p += 60;

        if (hit(my, p)) { PeoClient.CFG.nukerFilter = !PeoClient.CFG.nukerFilter; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerWhitelist = !PeoClient.CFG.nukerWhitelist; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerFilterIds = cycleList(PeoClient.CFG.nukerFilterIds, "", "minecraft:stone", "minecraft:dirt", "minecraft:stone,minecraft:dirt"); save(); return; } p += 60;

        if (hit(my, p)) { PeoClient.CFG.nukerRaycast = !PeoClient.CFG.nukerRaycast; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerFlatten = !PeoClient.CFG.nukerFlatten; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerRotate = !PeoClient.CFG.nukerRotate; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerNoParticles = !PeoClient.CFG.nukerNoParticles; save(); return; } p += 60;

        if (hit(my, p)) { PeoClient.CFG.nukerHighlight = !PeoClient.CFG.nukerHighlight; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerHighlightMode = cycle(PeoClient.CFG.nukerHighlightMode, "Opacity", "Expand"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerHighlightColor = cycleList(PeoClient.CFG.nukerHighlightColor, "255,128,128", "255,255,255", "255,200,80"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerRangeHighlight = !PeoClient.CFG.nukerRangeHighlight; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerRangeWidth = PeoClient.CFG.nukerRangeWidth >= 10 ? .1 : PeoClient.CFG.nukerRangeWidth + .5; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerRangeColor = cycleList(PeoClient.CFG.nukerRangeColor, "255,0,0", "255,255,255", "0,200,255"); save(); }
    }

    private void clickCleaner(double my, int y) {
        int p = y + 26;
        int[] maximums = {
                PeoClient.CFG.maxBlocks, PeoClient.CFG.maxArrows, PeoClient.CFG.maxThrowables,
                PeoClient.CFG.maxFoods, PeoClient.CFG.maxWaterBuckets, PeoClient.CFG.maxLavaBuckets,
                PeoClient.CFG.maxMilkBuckets
        };
        int[] mins = {0,0,0,0,0,0,0};
        int[] maxs = {2500,2500,600,2000,16,16,16};
        for (int i = 0; i < 7; i++) {
            if (hit(my, p)) { int nv = maximums[i] >= maxs[i] ? mins[i] : Math.min(maxs[i], maximums[i] + (i < 4 ? 32 : 1)); setCleanerMax(i, nv); save(); return; }
            p += 34;
        }
        if (hit(my, p)) { PeoClient.CFG.itemsBlacklist = cycleList(PeoClient.CFG.itemsBlacklist, "", "minecraft:dirt", "minecraft:dirt,minecraft:cobblestone"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.cleanerGreedy = !PeoClient.CFG.cleanerGreedy; save(); return; } p += 60;

        if (hit(my, p)) { PeoClient.CFG.offHandItem = cycle(PeoClient.CFG.offHandItem, "SWORD", "WEAPON", "SPEAR", "MACE", "BOW", "CROSSBOW", "AXE", "PICKAXE", "SHOVEL", "HOE", "ROD", "SHIELD", "WATER", "LAVA", "MILK", "PEARL", "GAPPLE", "FOOD", "POTION", "BLOCK", "THROWABLES", "IGNORE", "NONE"); save(); return; } p += 60;
        for (int i = 0; i < 9; i++) {
            if (hit(my, p)) { String old = PeoClient.CFG.slotItems[i]; PeoClient.CFG.slotItems[i] = cycle(old, "SWORD", "WEAPON", "SPEAR", "MACE", "BOW", "CROSSBOW", "AXE", "PICKAXE", "SHOVEL", "HOE", "ROD", "SHIELD", "WATER", "LAVA", "MILK", "PEARL", "GAPPLE", "FOOD", "POTION", "BLOCK", "THROWABLES", "IGNORE", "NONE"); save(); return; }
            p += 34;
        }
        p += 60;
        if (hit(my, p)) { PeoClient.CFG.cleanerMergeStacks = !PeoClient.CFG.cleanerMergeStacks; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.cleanerActionDelay = step(PeoClient.CFG.cleanerActionDelay, 0, 10); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.cleanerAckTimeout = PeoClient.CFG.cleanerAckTimeout >= 60 ? 5 : PeoClient.CFG.cleanerAckTimeout + 5; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.cleanerTouchHotbar = !PeoClient.CFG.cleanerTouchHotbar; save(); }
    }

    private void setCleanerMax(int i, int v) {
        switch (i) {
            case 0 -> PeoClient.CFG.maxBlocks = v;
            case 1 -> PeoClient.CFG.maxArrows = v;
            case 2 -> PeoClient.CFG.maxThrowables = v;
            case 3 -> PeoClient.CFG.maxFoods = v;
            case 4 -> PeoClient.CFG.maxWaterBuckets = v;
            case 5 -> PeoClient.CFG.maxLavaBuckets = v;
            case 6 -> PeoClient.CFG.maxMilkBuckets = v;
        }
    }

    private void clickXray(double my, int y) {
        int p = y + 60;
        if (hit(my, p)) { PeoClient.CFG.xrayExposedOnly = !PeoClient.CFG.xrayExposedOnly; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.xrayFluids = !PeoClient.CFG.xrayFluids; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.xrayBackgroundOpacity = PeoClient.CFG.xrayBackgroundOpacity >= 255 ? 0 : PeoClient.CFG.xrayBackgroundOpacity + 32; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.xrayFullBright = !PeoClient.CFG.xrayFullBright; save(); }
    }

    private void clickFullbright(double my, int y) {
        int p = y + 60;
        if (hit(my, p)) { PeoClient.CFG.fullbrightMethod = cycle(PeoClient.CFG.fullbrightMethod, "Gamma", "Night Vision"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.fullbrightFade = !PeoClient.CFG.fullbrightFade; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.fullbrightDefaultBrightness = PeoClient.CFG.fullbrightDefaultBrightness >= 1 ? 0 : PeoClient.CFG.fullbrightDefaultBrightness + .1; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.fullbrightBrightness = PeoClient.CFG.fullbrightBrightness >= 16 ? 1 : PeoClient.CFG.fullbrightBrightness + 1; save(); }
    }

    private boolean hit(double mouseY, int y) {
        return mouseY >= y && mouseY < y + 28;
    }

    private void save() { PeoClient.CFG.save(); }

    private int step(int value, int min, int max) {
        return value >= max ? min : value + 1;
    }

    private String cycle(String value, String... values) {
        for (int i = 0; i < values.length; i++) if (values[i].equalsIgnoreCase(value)) return values[(i + 1) % values.length];
        return values[0];
    }

    private String cycleList(String value, String... values) {
        return cycle(value, values);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int[] l = layout();
        int leftX = l[0], leftW = l[1], settingsX = l[2], settingsW = l[3], rightX = l[4], rightW = l[5], top = l[6], bottom = l[7];
        if (mouseY < top + 40 || mouseY > bottom) return true;
        int viewport = bottom - top - 48;
        if (mouseX >= leftX && mouseX <= leftX + leftW) {
            double max = Math.max(0, filtered().size() * 20 - viewport);
            hackScroll = clamp(hackScroll - verticalAmount * 26, 0, max);
        } else if (mouseX >= settingsX && mouseX <= settingsX + settingsW) {
            double max = Math.max(0, settingsContentHeight() - viewport);
            settingsScroll = clamp(settingsScroll - verticalAmount * 30, 0, max);
        } else if (mouseX >= rightX && mouseX <= rightX + rightW) {
            double max = Math.max(0, filtered().size() * 34 - viewport);
            moduleScroll = clamp(moduleScroll - verticalAmount * 30, 0, max);
        }
        return true;
    }

    private int settingsContentHeight() {
        return switch (selected) {
            case "Nuker [Multi]" -> 820;
            case "InventoryCleaner" -> 980;
            case "X-Ray" -> 220;
            case "Fullbright" -> 220;
            default -> 120;
        };
    }

    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { listening = false; bindTarget = null; return true; }
            setBind(bindTarget, keyCode);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
