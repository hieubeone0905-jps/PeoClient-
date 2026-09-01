package com.peoclient;

import com.peoclient.modules.AntiVipProMaxModule;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2248;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import net.minecraft.class_7923;

/** Wurst-inspired, restrained PeoClient hub with independently clipped panels. */
public final class PoeScreen extends class_437 {
    private class_342 search;
    private String selected = "Nuker [Multi]";
    private boolean listening;
    private String bindTarget;
    private double hackScroll;
    private double settingsScroll;
    private boolean draggingSettingsScrollbar;
    private boolean draggingHackScrollbar;
    private double dragScrollbarOffset;
    private String draggingSlider;

    static final List<String> MODULES = Arrays.asList(
            "Fullbright", "InventoryCleaner", "Nuker [Multi]", "X-Ray", "AntiVipProMax",
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
        super(class_2561.method_43470("PeoClient 1.21.4 V1"));
    }

    @Override
    protected void method_25426() {
        int searchW = Math.min(420, Math.max(260, field_22789 - 520));
        search = new class_342(field_22793, field_22789 / 2 - searchW / 2, 17, searchW, 22,
                class_2561.method_43470("Search"));
        search.method_1880(64);
        search.method_47404(class_2561.method_43470("Search modules..."));
        method_37063(search);
    }

    private boolean implemented(String name) {
        return name.equals("Fullbright") || name.equals("InventoryCleaner")
                || name.equals("Nuker [Multi]") || name.equals("X-Ray") || name.equals("AntiVipProMax");
    }

    private boolean enabled(String name) {
        return switch (name) {
            case "Fullbright" -> PeoClient.CFG.fullbright;
            case "InventoryCleaner" -> PeoClient.CFG.cleaner;
            case "Nuker [Multi]" -> PeoClient.CFG.nuker;
            case "X-Ray" -> PeoClient.CFG.xray;
            case "AntiVipProMax" -> AntiVipProMaxModule.isEnabled();
            default -> false;
        };
    }

    private void toggle(String name) {
        switch (name) {
            case "Fullbright" -> PeoClient.toggleFullbright(field_22787);
            case "InventoryCleaner" -> PeoClient.CFG.cleaner = !PeoClient.CFG.cleaner;
            case "Nuker [Multi]" -> PeoClient.CFG.nuker = !PeoClient.CFG.nuker;
            case "X-Ray" -> PeoClient.toggleXray(field_22787);
            case "AntiVipProMax" -> AntiVipProMaxModule.toggle();
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
        if (!PeoClient.MODULE_KEYS.containsKey(module)) return;
        listening = true;
        bindTarget = module;
    }

    private void setBind(String module, int keyCode) {
        PeoClient.setModuleKey(module, keyCode);
        listening = false;
        bindTarget = null;
    }

    private String keyName(String module) {
        return PeoClient.MODULE_KEYS.containsKey(module)
                ? PeoClient.MODULE_KEYS.get(module).method_16007().getString()
                : "NONE";
    }

    @Override
    public void method_25394(class_332 d, int mouseX, int mouseY, float delta) {
        d.method_25294(0, 0, field_22789, field_22790, 0xD90A0F14);
        d.method_25294(0, 0, field_22789, 74, 0xF20B1118);
        d.method_25294(0, 72, field_22789, 75, 0xFF315873);

        drawText(d, "P", 18, 20, 0xFFFFFFFF, true);
        drawText(d, "PeoClient", 42, 16, 0xFFFFFFFF, true);
        drawText(d, "1.21.4", 42, 34, 0xFFAAB5BE, false);
        drawText(d, "Right Shift • Hub", Math.max(18, field_22789 - 190), 18, 0xFFFFFFFF, false);
        drawText(d, listening ? "Press a key (ESC cancels)" : "Left click = toggle  •  Right click = settings",
                Math.max(18, field_22789 - 380), 42, 0xFFB6C0C8, false);

        super.method_25394(d, mouseX, mouseY, delta);

        int[] l = layout();
        int leftX = l[0], leftW = l[1], settingsX = l[2], settingsW = l[3], top = l[4], bottom = l[5];
        int panelH = bottom - top;
        panel(d, leftX, top, leftW, panelH);
        panel(d, settingsX, top, settingsW, panelH);

        drawText(d, "HACK LIST", leftX + 16, top + 18, 0xFFFFFFFF, true);
        drawText(d, "SETTINGS", settingsX + 16, top + 18, 0xFFFFFFFF, true);

        int listTop = top + 40;
        int listBottom = bottom - 8;
        d.method_44379(leftX + 1, listTop, leftX + leftW - 1, listBottom);
        drawHackList(d, leftX + 14, listTop, leftW - 28, listBottom - listTop);
        d.method_44380();

        d.method_44379(settingsX + 1, listTop, settingsX + settingsW - 1, listBottom);
        drawSettings(d, settingsX + 14, listTop, settingsW - 28);
        d.method_44380();
        drawScrollbar(d, leftX + leftW - 7, listTop, 6, listBottom - listTop, filtered().size() * 20, listBottom - listTop, hackScroll);
        drawScrollbar(d, settingsX + settingsW - 7, listTop, 6, listBottom - listTop, settingsContentHeight(), listBottom - listTop, settingsScroll);

        drawText(d, "Wheel: scroll hovered panel", 14, field_22790 - 16, 0xFF73808B, false);
        drawText(d, "ESC: close", Math.max(18, field_22789 - field_22793.method_1727("ESC: close") - 14), field_22790 - 16, 0xFF73808B, false);
    }

    private void panel(class_332 d, int x, int y, int w, int h) {
        d.method_25294(x, y, x + w, y + h, 0xD20D151D);
        d.method_49601(x, y, w, h, 0xFF2A4154);
    }

    private void drawText(class_332 d, String s, int x, int y, int color, boolean bold) {
        d.method_51439(field_22793,
                class_2561.method_43470(s).method_27694(style -> style.method_10982(bold)),
                x, y, color, false);
    }

    private void drawHackList(class_332 d, int x, int y, int w, int h) {
        int rowH = 20;
        int yy = y - (int) hackScroll;
        for (String name : filtered()) {
            if (yy + rowH >= y && yy <= y + h) {
                boolean real = implemented(name);
                boolean on = enabled(name);
                if (name.equals(selected)) d.method_25294(x - 8, yy - 2, x + w, yy + 17, 0xFF1C3547);
                drawText(d, name, x, yy + 2, real ? 0xFFFFFFFF : 0xFF7E8A94, real && (on || name.equals(selected)));
                if (on) d.method_25294(x + w - 8, yy + 6, x + w - 2, yy + 12, 0xFFFFFFFF);
            }
            yy += rowH;
        }
    }

    private void drawSettings(class_332 d, int x, int y, int w) {
        int yy = y - (int) settingsScroll;
        drawText(d, selected, x, yy + 2, 0xFFFFFFFF, true);
        yy += 20;
        String desc = description(selected);
        drawText(d, desc.isBlank() ? "Module is not implemented in this build." : desc, x, yy, 0xFF98A5AE, false);
        yy += 26;
        if (implemented(selected)) {
            yy = rowToggle(d, x, yy, w, "Enable", enabled(selected));
            yy = rowValue(d, x, yy, w, "Keybind", keyName(selected));
            switch (selected) {
                case "Nuker [Multi]" -> drawNuker(d, x, yy, w);
                case "InventoryCleaner" -> drawCleaner(d, x, yy, w);
                case "X-Ray" -> drawXray(d, x, yy, w);
                case "Fullbright" -> drawFullbright(d, x, yy, w);
                case "AntiVipProMax" -> drawAntiVipProMax(d, x, yy, w);
            }
        } else {
            rowValue(d, x, yy, w, "Keybind", keyName(selected));
        }
    }

    private int drawNuker(class_332 d, int x, int y, int w) {
        y = section(d, x, y, "Mining");
        y = rowValue(d, x, y, w, "Mode", PeoClient.CFG.nukerMode);
        y = sliderRow(d, x, y, w, "Multi", PeoClient.CFG.nukerMulti, 0, 10, "%.0f blocks");
        y = sliderRow(d, x, y, w, "Cooldown", PeoClient.CFG.nukerCooldown, 0, 20, "%.0f ticks");
        y = rowValue(d, x, y, w, "Shape", PeoClient.CFG.nukerShape);
        y = sliderRow(d, x, y, w, "Range", PeoClient.CFG.nukerRange, 0.0, 15.0, "%.1f");
        y = rowValue(d, x, y, w, "Sort", PeoClient.CFG.nukerSort);

        y = section(d, x, y, "Filter");
        y = rowToggle(d, x, y, w, "Filter", PeoClient.CFG.nukerFilter);
        y = rowValue(d, x, y, w, "Filter mode", PeoClient.CFG.nukerWhitelist ? "Whitelist" : "Blacklist");
        y = rowValue(d, x, y, w, "Edit blocks", selectedNukerCount() + " selected");
        y = drawSelectedNukerBlocks(d, x, y, w);

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
        y = sliderRow(d, x, y, w, "Width", PeoClient.CFG.nukerRangeWidth, 0.1, 10.0, "%.1f");
        y = rowValue(d, x, y, w, "Range color", PeoClient.CFG.nukerRangeColor);
        return y;
    }

    private int drawSelectedNukerBlocks(class_332 d, int x, int y, int w) {
        List<String> ids = nukerFilterIds();
        if (ids.isEmpty()) return y + 6;
        int visible = Math.min(ids.size(), 5);
        for (int i = 0; i < visible; i++) {
            String raw = ids.get(i);
            class_2960 id = class_2960.method_12829(raw);
            String name = raw;
            class_1799 stack = class_1799.field_8037;
            if (id != null && class_7923.field_41175.method_10250(id)) {
                class_2248 block = class_7923.field_41175.method_63535(id);
                name = block.method_8389().method_7854().method_7964().getString();
                stack = block.method_8389().method_7854();
            }
            d.method_25294(x, y, x + w, y + 32, 0x9A121C24);
            d.method_49601(x, y, w, 32, 0xFF294354);
            if (!stack.method_7960()) d.method_51427(stack, x + 6, y + 7);
            drawText(d, fitText(name, Math.max(80, w - 120)), x + 34, y + 6, 0xFFFFFFFF, false);
            drawText(d, fitText(raw, Math.max(80, w - 120)), x + 34, y + 18, 0xFF8E9AA3, false);
            drawText(d, "X", x + w - 18, y + 10, 0xFFFFFFFF, true);
            y += 36;
        }
        if (ids.size() > visible) {
            drawText(d, "+" + (ids.size() - visible) + " more...", x + 8, y + 2, 0xFF98A5AE, false);
            y += 20;
        }
        return y + 4;
    }

    private List<String> nukerFilterIds() {
        List<String> out = new ArrayList<>();
        String raw = PeoClient.CFG.nukerFilterIds == null ? "" : PeoClient.CFG.nukerFilterIds;
        for (String s : raw.split("[,\\n\\s]+")) {
            String t = s.trim();
            if (!t.isBlank() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    private int selectedNukerCount() {
        return nukerFilterIds().size();
    }

    private int drawCleaner(class_332 d, int x, int y, int w) {
        y = section(d, x, y, "Inventory quotas");
        y = rowValue(d, x, y, w, "MaximumBlocks", String.valueOf(PeoClient.CFG.maxBlocks));
        y = rowValue(d, x, y, w, "MaximumArrows", String.valueOf(PeoClient.CFG.maxArrows));
        y = rowValue(d, x, y, w, "MaximumThrowables", String.valueOf(PeoClient.CFG.maxThrowables));
        y = rowValue(d, x, y, w, "MaximumFoodPoints", String.valueOf(PeoClient.CFG.maxFoods));
        y = rowValue(d, x, y, w, "MaximumWaterBuckets", String.valueOf(PeoClient.CFG.maxWaterBuckets));
        y = rowValue(d, x, y, w, "MaximumLavaBuckets", String.valueOf(PeoClient.CFG.maxLavaBuckets));
        y = rowValue(d, x, y, w, "MaximumMilkBuckets", String.valueOf(PeoClient.CFG.maxMilkBuckets));
        y = rowValue(d, x, y, w, "ItemsBlacklist", shortList(PeoClient.CFG.itemsBlacklist));

        y = section(d, x, y, "Drop filter");
        y = rowValue(d, x, y, w, "Edit items", PeoClient.CFG.cleanerDropFilter.size() + " items selected");
        y = rowToggle(d, x, y, w, "Filtered items only", PeoClient.CFG.cleanerFilterOnly);
        y = rowValue(d, x, y, w, "Drop mode", PeoClient.CFG.cleanerFilterOnly
                ? "Only selected items" : "Normal cleaner");

        y = section(d, x, y, "General cleaner");
        y = rowToggle(d, x, y, w, "Greedy", PeoClient.CFG.cleanerGreedy);

        y = section(d, x, y, "Offhand");
        y = rowValue(d, x, y, w, "OffHandItem", PeoClient.CFG.offHandItem);

        y = section(d, x, y, "Hotbar slots");
        String[] slots = PeoClient.CFG.slotItems;
        for (int i = 0; i < 9; i++) y = rowValue(d, x, y, w, "SlotItem-" + (i + 1), i < slots.length ? safe(slots[i], "NONE") : "NONE");

        y = section(d, x, y, "PeoClient extensions");
        y = rowToggle(d, x, y, w, "Merge partial stacks", PeoClient.CFG.cleanerMergeStacks);
        y = rowValue(d, x, y, w, "Action delay", PeoClient.CFG.cleanerActionDelay + " ticks");
        y = rowValue(d, x, y, w, "Server ack timeout", PeoClient.CFG.cleanerAckTimeout + " ticks (lower = faster)");
        y = rowToggle(d, x, y, w, "Touch hotbar", PeoClient.CFG.cleanerTouchHotbar);
        return y;
    }

    private int drawXray(class_332 d, int x, int y, int w) {
        y = section(d, x, y, "Blocks");
        y = rowValue(d, x, y, w, "Edit blocks", shortSet(PeoClient.CFG.xrayBlocks));
        y += 8;
        y = section(d, x, y, "Visibility");
        y = rowToggle(d, x, y, w, "Sky only (AFK / low lag)", PeoClient.CFG.xraySkyOnly);
        y = rowToggle(d, x, y, w, "Exposed only", PeoClient.CFG.xrayExposedOnly);
        y = rowToggle(d, x, y, w, "Fluids", PeoClient.CFG.xrayFluids);
        y = rowValue(d, x, y, w, "Background opacity", PeoClient.CFG.xrayBackgroundOpacity + "/255");
        y = rowValue(d, x, y, w, "FullBright", PeoClient.CFG.xrayFullBright ? "ON" : "OFF");
        return y;
    }

    private int drawFullbright(class_332 d, int x, int y, int w) {
        y = section(d, x, y, "Brightness");
        y = rowValue(d, x, y, w, "Method", PeoClient.CFG.fullbrightMethod);
        y = rowToggle(d, x, y, w, "Fade", PeoClient.CFG.fullbrightFade);
        y = rowValue(d, x, y, w, "Default brightness", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.fullbrightDefaultBrightness));
        y = rowValue(d, x, y, w, "Brightness", String.format(Locale.ROOT, "%.1f", PeoClient.CFG.fullbrightBrightness));
        return y;
    }

    private int section(class_332 d, int x, int y, String title) {
        drawText(d, title, x, y + 2, 0xFFFFFFFF, true);
        d.method_25294(x, y + 18, x + Math.min(170, 88 + field_22793.method_1727(title)), y + 19, 0xFF3B586D);
        return y + 26;
    }

    private int rowValue(class_332 d, int x, int y, int w, String label, String value) {
        d.method_25294(x, y, x + w, y + 28, 0xB7152029);
        d.method_49601(x, y, w, 28, 0xFF294354);
        drawText(d, label, x + 10, y + 9, 0xFFFFFFFF, false);

        // Keep long setting values inside the panel. This is especially important
        // for block-filter lists, long proxy/account values, and translated names.
        int labelReserve = Math.max(150, field_22793.method_1727(label) + 22);
        int valueLeft = x + labelReserve;
        int valueRight = x + w - 10;
        int maxValueWidth = Math.max(80, valueRight - valueLeft);
        String shown = fitText(value, maxValueWidth);
        int tw = field_22793.method_1727(shown);
        drawText(d, shown, Math.max(valueLeft, valueRight - tw), y + 9, 0xFFE1E6EA, false);
        return y + 34;
    }

    private String fitText(String value, int maxWidth) {
        if (value == null || value.isBlank()) return "";
        if (field_22793.method_1727(value) <= maxWidth) return value;
        String suffix = "...";
        int room = Math.max(0, maxWidth - field_22793.method_1727(suffix));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            String candidate = out.toString() + value.charAt(i);
            if (field_22793.method_1727(candidate) > room) break;
            out.append(value.charAt(i));
        }
        return out + suffix;
    }

    private int sliderRow(class_332 d, int x, int y, int w, String label, double value, double min, double max, String format) {
        d.method_25294(x, y, x + w, y + 28, 0xB7152029);
        d.method_49601(x, y, w, 28, 0xFF294354);
        drawText(d, label, x + 10, y + 9, 0xFFFFFFFF, false);
        int sliderX = x + Math.max(130, Math.min(175, field_22793.method_1727(label) + 28));
        int sliderW = Math.max(90, w - (sliderX - x) - 72);
        int trackY = y + 14;
        d.method_25294(sliderX, trackY - 2, sliderX + sliderW, trackY + 2, 0xFF304B5C);
        double norm = (value - min) / (max - min);
        norm = Math.max(0.0, Math.min(1.0, norm));
        int knobX = sliderX + (int)Math.round(norm * sliderW);
        d.method_25294(sliderX, trackY - 2, knobX, trackY + 2, 0xFF8EA6B5);
        d.method_25294(knobX - 3, trackY - 5, knobX + 3, trackY + 5, 0xFFFFFFFF);
        String shown = String.format(Locale.ROOT, format, value);
        drawText(d, shown, x + w - field_22793.method_1727(shown) - 10, y + 9, 0xFFE1E6EA, false);
        return y + 34;
    }

    private int rowToggle(class_332 d, int x, int y, int w, String label, boolean value) {
        rowValue(d, x, y, w, label, "");
        int bx = x + w - 39;
        d.method_25294(bx, y + 7, bx + 30, y + 21, value ? 0xFFFFFFFF : 0xFF697782);
        drawText(d, value ? "ON" : "OFF", bx + 4, y + 9, value ? 0xFF0A1014 : 0xFFFFFFFF, true);
        return y + 34;
    }

    private String description(String module) {
        return switch (module) {
            case "Nuker [Multi]" -> "Automatically breaks blocks around you.";
            case "InventoryCleaner" -> "Cleans and sorts your inventory automatically.";
            case "Fullbright" -> "Makes dark areas bright.";
            case "X-Ray" -> "Shows selected blocks through the world.";
            case "AntiVipProMax" -> "Nuker compatibility/settings module; keeps existing Nuker logic unchanged.";
            default -> "";
        };
    }

    private int drawAntiVipProMax(class_332 d, int x, int y, int w) {
        y = section(d, x, y, "AntiVipProMax");
        y = rowToggle(d, x, y, w, "Grim Mode", AntiVipProMaxModule.isGrimMode());
        y = rowToggle(d, x, y, w, "Vulcan Mode", AntiVipProMaxModule.isVulcanMode());
        y = sliderRow(d, x, y, w, "Intensity", AntiVipProMaxModule.getIntensity(), 1, 10, "%.0f");
        y = rowToggle(d, x, y, w, "Auto Adjust", AntiVipProMaxModule.isAutoAdjust());
        y = rowValue(d, x, y, w, "Status", AntiVipProMaxModule.getStatus());
        y = rowValue(d, x, y, w, "Mode", "Compatibility/status only; no anti-cheat bypass");
        return y;
    }

    private String shortList(String value) {
        if (value == null || value.isBlank()) return "Empty list";
        return value.length() <= 28 ? value : value.substring(0, 25) + "...";
    }

    private String shortSet(Set<String> values) {
        if (values == null || values.isEmpty()) return "Empty list";
        String value = String.join(", ", values);
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
        return search == null ? "" : search.method_1882().trim().toLowerCase(Locale.ROOT);
    }

    private int[] layout() {
        // Wurst-like two-column layout: both panels always remain fully inside
        // the viewport, with the settings panel getting the remaining space.
        int margin = Math.max(18, Math.min(32, field_22789 / 40));
        int gap = Math.max(14, Math.min(20, field_22789 / 90));
        int top = 92;
        int bottom = Math.max(top + 260, field_22790 - 28);
        int usable = Math.max(520, field_22789 - margin * 2 - gap);

        int leftW = Math.round(usable * 0.29f);
        leftW = Math.max(300, Math.min(390, leftW));

        int settingsW = usable - leftW;
        if (settingsW < 420) {
            int neededLeft = Math.max(260, usable - 420);
            leftW = Math.min(leftW, neededLeft);
            settingsW = usable - leftW;
        }

        int leftX = margin;
        int settingsX = leftX + leftW + gap;
        // Final safety clamp so neither border can ever be drawn beyond the screen.
        settingsW = Math.max(1, Math.min(settingsW, field_22789 - margin - settingsX));
        return new int[]{leftX, leftW, settingsX, settingsW, top, bottom};
    }

    @Override
    public boolean method_25402(double mouseX, double mouseY, int button) {
        super.method_25402(mouseX, mouseY, button);
        int[] l = layout();
        int leftX = l[0], leftW = l[1], settingsX = l[2], settingsW = l[3], top = l[4], bottom = l[5];
        int listTop = top + 40;
        if (mouseY < listTop || mouseY >= bottom) return super.method_25402(mouseX, mouseY, button);

        int viewport = bottom - listTop;
        if (button == 0 && mouseX >= leftX + leftW - 10 && mouseX < leftX + leftW && canScroll(filtered().size() * 20, viewport)) {
            double max = Math.max(0, filtered().size() * 20 - viewport);
            int[] thumb = scrollbarThumb(listTop, viewport, filtered().size() * 20, viewport, hackScroll);
            if (mouseY >= thumb[0] && mouseY <= thumb[1]) {
                draggingHackScrollbar = true;
                dragScrollbarOffset = mouseY - thumb[0];
                return true;
            }
        }
        if (button == 0 && mouseX >= settingsX + settingsW - 10 && mouseX < settingsX + settingsW && canScroll(settingsContentHeight(), viewport)) {
            int[] thumb = scrollbarThumb(listTop, viewport, settingsContentHeight(), viewport, settingsScroll);
            if (mouseY >= thumb[0] && mouseY <= thumb[1]) {
                draggingSettingsScrollbar = true;
                dragScrollbarOffset = mouseY - thumb[0];
                return true;
            }
        }

        if (mouseX >= leftX && mouseX <= leftX + leftW - 10) {
            int rowH = 20;
            int yy = listTop - (int) hackScroll;
            for (String name : filtered()) {
                if (mouseY >= yy && mouseY < yy + rowH) {
                    select(name);
                    return true;
                }
                yy += rowH;
            }
            return true;
        }

        if (mouseX >= settingsX && mouseX <= settingsX + settingsW) {
            int y = listTop - (int) settingsScroll;
            int firstRowsTop = y + 46;
            if (implemented(selected)) {
                if (hit(mouseY, firstRowsTop)) { toggle(selected); return true; }
                if (hit(mouseY, firstRowsTop + 34)) { openBind(selected); return true; }
            } else if (hit(mouseY, firstRowsTop)) {
                openBind(selected);
                return true;
            }
            int contentY = firstRowsTop + (implemented(selected) ? 68 : 34);
            switch (selected) {
                case "Nuker [Multi]" -> {
                    int np = contentY + 26;
                    if (hit(mouseY, np + 34)) draggingSlider = "NukerMulti";
                    else if (hit(mouseY, np + 68)) draggingSlider = "NukerCooldown";
                    else if (hit(mouseY, np + 136)) draggingSlider = "NukerRange";
                    else {
                        int wp = np + 26;
                        wp += 34 * 5; // Mode, Multi, Cooldown, Shape, Range, Sort
                        wp += 60;     // Filter section header gap
                        wp += 34 * 2; // Filter + Filter mode
                        wp += 60;     // Edit blocks + selected-list area header
                        wp += Math.min(nukerFilterIds().size(), 5) * 36;
                        if (nukerFilterIds().size() > 5) wp += 20;
                        wp += 34 * 4; // Raycast, Flatten, Rotate, NoParticles
                        wp += 60;     // Highlight section
                        wp += 34 * 4; // Highlight, Mode, Color, RangeHighlight
                        if (hit(mouseY, wp)) draggingSlider = "NukerWidth";
                    }
                    clickNuker(mouseX, mouseY, settingsX, settingsW, contentY);
                }
                case "InventoryCleaner" -> clickCleaner(mouseY, contentY);
                case "X-Ray" -> clickXray(mouseY, contentY);
                case "Fullbright" -> clickFullbright(mouseY, contentY);
                case "AntiVipProMax" -> clickAntiVipProMax(mouseX, mouseY, settingsX, settingsW, contentY);
            }
            return true;
        }
        return true;
    }

    private double sliderValue(double mouseX, int x, int w, double min, double max, String label) {
        int sliderX = x + Math.max(130, Math.min(175, field_22793.method_1727(label) + 28));
        int sliderW = Math.max(90, w - (sliderX - x) - 72);
        double t = class_3532.method_15350((mouseX - sliderX) / (double) sliderW, 0.0, 1.0);
        return min + t * (max - min);
    }

    private double roundSlider(double value, double min, double max, double step) {
        double v = class_3532.method_15350(value, min, max);
        return Math.round(v / step) * step;
    }

    private void clickAntiVipProMax(double mx, double my, int x, int w, int y) {
        int p = y + 26;
        if (hit(my, p)) { AntiVipProMaxModule.setGrimMode(!AntiVipProMaxModule.isGrimMode()); save(); return; } p += 34;
        if (hit(my, p)) { AntiVipProMaxModule.setVulcanMode(!AntiVipProMaxModule.isVulcanMode()); save(); return; } p += 34;
        if (hit(my, p)) {
            AntiVipProMaxModule.setIntensity((int)Math.round(sliderValue(mx, x, w, 1, 10, "Intensity")));
            save(); return;
        } p += 34;
        if (hit(my, p)) { AntiVipProMaxModule.setAutoAdjust(!AntiVipProMaxModule.isAutoAdjust()); save(); }
    }

    private void clickNuker(double mx, double my, int x, int w, int y) {
        int p = y + 26;
        if (hit(my, p)) { PeoClient.CFG.nukerMode = cycle(PeoClient.CFG.nukerMode, "Normal", "SurvMulti", "Multi", "Instant"); save(); return; } p += 34;

        if (hit(my, p)) {
            double v = sliderValue(mx, x, w, 0, 10, "Multi");
            PeoClient.CFG.nukerMulti = (int) Math.round(v);
            save(); return;
        } p += 34;

        if (hit(my, p)) {
            double v = sliderValue(mx, x, w, 0, 20, "Cooldown");
            PeoClient.CFG.nukerCooldown = (int) Math.round(v);
            save(); return;
        } p += 34;

        if (hit(my, p)) { PeoClient.CFG.nukerShape = cycle(PeoClient.CFG.nukerShape, "Cube", "Sphere"); save(); return; } p += 34;

        if (hit(my, p)) {
            PeoClient.CFG.nukerRange = roundSlider(sliderValue(mx, x, w, 0.0, 15.0, "Range"), 0.0, 15.0, 0.1);
            save(); return;
        } p += 34;

        if (hit(my, p)) { PeoClient.CFG.nukerSort = cycle(PeoClient.CFG.nukerSort, "Closest", "Furthest", "Softest", "Hardest", "None"); save(); return; } p += 60;

        if (hit(my, p)) { PeoClient.CFG.nukerFilter = !PeoClient.CFG.nukerFilter; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerWhitelist = !PeoClient.CFG.nukerWhitelist; save(); return; } p += 34;
        if (hit(my, p)) { field_22787.method_1507(new BlockPickerScreen(this, true)); return; } p += 60;

        List<String> ids = nukerFilterIds();
        int visible = Math.min(ids.size(), 5);
        for (int i = 0; i < visible; i++) {
            if (hit(my, p)) { ids.remove(i); PeoClient.CFG.nukerFilterIds = String.join(",", ids); save(); return; }
            p += 36;
        }
        if (ids.size() > visible) p += 20;

        if (hit(my, p)) { PeoClient.CFG.nukerRaycast = !PeoClient.CFG.nukerRaycast; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerFlatten = !PeoClient.CFG.nukerFlatten; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerRotate = !PeoClient.CFG.nukerRotate; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerNoParticles = !PeoClient.CFG.nukerNoParticles; save(); return; } p += 60;

        if (hit(my, p)) { PeoClient.CFG.nukerHighlight = !PeoClient.CFG.nukerHighlight; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerHighlightMode = cycle(PeoClient.CFG.nukerHighlightMode, "Opacity", "Expand"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerHighlightColor = cycleList(PeoClient.CFG.nukerHighlightColor, "255,128,128", "255,255,255", "255,200,80"); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.nukerRangeHighlight = !PeoClient.CFG.nukerRangeHighlight; save(); return; } p += 34;
        if (hit(my, p)) {
            PeoClient.CFG.nukerRangeWidth = roundSlider(sliderValue(mx, x, w, 0.1, 10.0, "Width"), 0.1, 10.0, 0.1);
            save(); return;
        } p += 34;
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
        if (hit(my, p)) { PeoClient.CFG.itemsBlacklist = cycleList(PeoClient.CFG.itemsBlacklist, "", "minecraft:dirt", "minecraft:dirt,minecraft:cobblestone"); save(); return; } p += 60;

        if (hit(my, p)) { field_22787.method_1507(new CleanerItemPickerScreen(this)); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.cleanerFilterOnly = !PeoClient.CFG.cleanerFilterOnly; save(); return; } p += 34;
        p += 60; // Drop mode row + next section header

        if (hit(my, p)) { PeoClient.CFG.cleanerGreedy = !PeoClient.CFG.cleanerGreedy; save(); return; } p += 60;

        if (hit(my, p)) { PeoClient.CFG.offHandItem = cycle(PeoClient.CFG.offHandItem, "SWORD", "WEAPON", "SPEAR", "MACE", "BOW", "CROSSBOW", "AXE", "PICKAXE", "SHOVEL", "HOE", "ROD", "SHIELD", "WATER", "LAVA", "MILK", "PEARL", "GAPPLE", "FOOD", "POTION", "BLOCK", "THROWABLES", "IGNORE", "NONE"); save(); return; } p += 60;
        for (int i = 0; i < 9; i++) {
            if (hit(my, p)) { String old = PeoClient.CFG.slotItems[i]; PeoClient.CFG.slotItems[i] = cycle(old, "SWORD", "WEAPON", "SPEAR", "MACE", "BOW", "CROSSBOW", "AXE", "PICKAXE", "SHOVEL", "HOE", "ROD", "SHIELD", "WATER", "LAVA", "MILK", "PEARL", "GAPPLE", "FOOD", "POTION", "BLOCK", "THROWABLES", "IGNORE", "NONE"); save(); return; }
            p += 34;
        }
        p += 60;
        if (hit(my, p)) { PeoClient.CFG.cleanerMergeStacks = !PeoClient.CFG.cleanerMergeStacks; save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.cleanerActionDelay = step(PeoClient.CFG.cleanerActionDelay, 0, 10); save(); return; } p += 34;
        if (hit(my, p)) { PeoClient.CFG.cleanerAckTimeout = PeoClient.CFG.cleanerAckTimeout >= 30 ? 2 : PeoClient.CFG.cleanerAckTimeout + 2; save(); return; } p += 34;
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
        int p = y + 26;
        if (hit(my, p)) { field_22787.method_1507(new BlockPickerScreen(this, false)); return; } p += 68;
        if (hit(my, p)) { PeoClient.CFG.xraySkyOnly = !PeoClient.CFG.xraySkyOnly; PeoClient.reload(field_22787); save(); return; } p += 34;
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

    private boolean canScroll(int contentHeight, int viewport) {
        return contentHeight > viewport;
    }

    private int[] scrollbarThumb(int top, int viewport, int contentHeight, int viewHeight, double scroll) {
        int trackH = Math.max(1, viewport);
        int thumbH = Math.max(24, (int) ((double) trackH * trackH / Math.max(trackH, contentHeight)));
        thumbH = Math.min(trackH, thumbH);
        int maxScroll = Math.max(1, contentHeight - viewHeight);
        int travel = Math.max(0, trackH - thumbH);
        int thumbTop = top + (int) Math.round(travel * (scroll / maxScroll));
        return new int[]{thumbTop, thumbTop + thumbH};
    }

    private void drawScrollbar(class_332 d, int x, int y, int w, int h, int contentHeight, int viewHeight, double scroll) {
        if (!canScroll(contentHeight, viewHeight)) return;
        d.method_25294(x, y, x + w, y + h, 0x401A252E);
        int[] thumb = scrollbarThumb(y, h, contentHeight, viewHeight, scroll);
        d.method_25294(x, thumb[0], x + w, thumb[1], 0xFF6C7D89);
        d.method_49601(x, thumb[0], w, Math.max(1, thumb[1] - thumb[0]), 0xFF9AA7B0);
    }

    @Override
    public boolean method_25403(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingSlider != null) {
            int[] l = layout();
            int sx = l[2], sw = l[3], top = l[4], bottom = l[5];
            int listTop = top + 40;
            int viewport = bottom - listTop;
            int y = listTop - (int) settingsScroll;
            int firstRowsTop = y + 46;
            int contentY = firstRowsTop + (implemented(selected) ? 68 : 34);
            int p = contentY + 26;
            if ("NukerMulti".equals(draggingSlider)) {
                PeoClient.CFG.nukerMulti = (int)Math.round(sliderValue(mouseX, sx, sw, 0, 10, "Multi"));
            } else if ("NukerCooldown".equals(draggingSlider)) {
                PeoClient.CFG.nukerCooldown = (int)Math.round(sliderValue(mouseX, sx, sw, 0, 20, "Cooldown"));
            } else if ("NukerRange".equals(draggingSlider)) {
                PeoClient.CFG.nukerRange = roundSlider(sliderValue(mouseX, sx, sw, 0.0, 15.0, "Range"), 0.0, 15.0, 0.1);
            } else if ("NukerWidth".equals(draggingSlider)) {
                PeoClient.CFG.nukerRangeWidth = roundSlider(sliderValue(mouseX, sx, sw, 0.1, 10.0, "Width"), 0.1, 10.0, 0.1);
            }
            PeoClient.CFG.save();
            return true;
        }
        int[] l = layout();
        int leftX = l[0], leftW = l[1], settingsX = l[2], settingsW = l[3], top = l[4], bottom = l[5];
        int listTop = top + 40;
        int viewport = bottom - listTop;
        if (button == 0 && draggingSettingsScrollbar) {
            int[] thumb = scrollbarThumb(listTop, viewport, settingsContentHeight(), viewport, settingsScroll);
            int thumbH = thumb[1] - thumb[0];
            int travel = Math.max(1, viewport - thumbH);
            int desiredTop = (int) mouseY - (int) dragScrollbarOffset;
            int relative = class_3532.method_15340(desiredTop - listTop, 0, travel);
            double max = Math.max(0, settingsContentHeight() - viewport);
            settingsScroll = max == 0 ? 0 : (double) relative / travel * max;
            return true;
        }
        if (button == 0 && draggingHackScrollbar) {
            int content = filtered().size() * 20;
            int[] thumb = scrollbarThumb(listTop, viewport, content, viewport, hackScroll);
            int thumbH = thumb[1] - thumb[0];
            int travel = Math.max(1, viewport - thumbH);
            int desiredTop = (int) mouseY - (int) dragScrollbarOffset;
            int relative = class_3532.method_15340(desiredTop - listTop, 0, travel);
            double max = Math.max(0, content - viewport);
            hackScroll = max == 0 ? 0 : (double) relative / travel * max;
            return true;
        }
        return super.method_25403(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean method_25406(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSettingsScrollbar = false;
            draggingHackScrollbar = false;
            draggingSlider = null;
        }
        return super.method_25406(mouseX, mouseY, button);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int[] l = layout();
        int leftX = l[0], leftW = l[1], settingsX = l[2], settingsW = l[3], top = l[4], bottom = l[5];
        if (mouseY < top + 40 || mouseY > bottom) return true;
        int viewport = bottom - top - 48;
        if (mouseX >= leftX && mouseX <= leftX + leftW) {
            double max = Math.max(0, filtered().size() * 20 - viewport);
            hackScroll = clamp(hackScroll - verticalAmount * 26, 0, max);
        } else if (mouseX >= settingsX && mouseX <= settingsX + settingsW) {
            double max = Math.max(0, settingsContentHeight() - viewport);
            // Larger movement makes the lower Nuker settings reachable even on
            // short screens, while retaining the same panel/layout.
            settingsScroll = clamp(settingsScroll - verticalAmount * 36, 0, max);
        }
        return true;
    }

    private int settingsContentHeight() {
        return switch (selected) {
            case "Nuker [Multi]" -> 1240;
            case "InventoryCleaner" -> 1060;
            case "X-Ray" -> 340;
            case "Fullbright" -> 220;
            case "AntiVipProMax" -> 300;
            default -> 120;
        };
    }

    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    @Override
    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { listening = false; bindTarget = null; return true; }
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                setBind(bindTarget, GLFW.GLFW_KEY_UNKNOWN);
                return true;
            }
            setBind(bindTarget, keyCode);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            method_25419();
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }
    /** Item-only picker for InventoryCleaner's explicit drop filter. */
    private final class CleanerItemPickerScreen extends class_437 {
        private final PoeScreen parent;
        private class_342 search;
        private double scroll;
        private boolean draggingScrollbar;
        private double dragOffset;

        CleanerItemPickerScreen(PoeScreen parent) {
            super(class_2561.method_43470("InventoryCleaner - Drop Filter"));
            this.parent = parent;
        }

        @Override
        protected void method_25426() {
            int w = Math.min(560, field_22789 - 40);
            search = new class_342(field_22793, field_22789 / 2 - w / 2, 34, w, 22,
                    class_2561.method_43470("Search items"));
            search.method_1880(96);
            search.method_47404(class_2561.method_43470("Type item name or minecraft:id..."));
            method_37063(search);
        }

        private List<ItemEntry> matches() {
            String q = search == null ? "" : search.method_1882().trim().toLowerCase(Locale.ROOT);
            List<ItemEntry> out = new ArrayList<>();
            for (class_2960 id : class_7923.field_41178.method_10235()) {
                class_1792 item = class_7923.field_41178.method_63535(id);
                if (item == null) continue;
                class_1799 stack = item.method_7854();
                String name = stack.method_7964().getString();
                String key = id.toString();
                if (q.isBlank() || name.toLowerCase(Locale.ROOT).contains(q) || key.contains(q)) {
                    out.add(new ItemEntry(id, name, stack));
                }
            }
            out.sort((a, b) -> {
                String aa = (a.name + " " + a.id).toLowerCase(Locale.ROOT);
                String bb = (b.name + " " + b.id).toLowerCase(Locale.ROOT);
                boolean ap = !q.isBlank() && aa.startsWith(q);
                boolean bp = !q.isBlank() && bb.startsWith(q);
                if (ap != bp) return ap ? -1 : 1;
                return a.name.compareToIgnoreCase(b.name);
            });
            return out;
        }

        private Set<String> selected() {
            return PeoClient.CFG.cleanerDropFilter == null
                    ? new LinkedHashSet<>() : new LinkedHashSet<>(PeoClient.CFG.cleanerDropFilter);
        }

        private void save(Set<String> selected) {
            PeoClient.CFG.cleanerDropFilter = new LinkedHashSet<>(selected);
            PeoClient.CFG.save();
        }

        @Override
        public void method_25394(class_332 d, int mouseX, int mouseY, float delta) {
            method_25420(d, mouseX, mouseY, delta);
            int left = Math.max(24, field_22789 / 2 - 290);
            int right = Math.min(field_22789 - 24, field_22789 / 2 + 290);
            int top = 72;
            int bottom = field_22790 - 34;

            d.method_25294(left - 8, top - 8, right + 8, bottom + 8, 0xEE0D151D);
            d.method_49601(left - 8, top - 8, right - left + 16, bottom - top + 16, 0xFF2A4154);
            d.method_51439(field_22793,
                    class_2561.method_43470("InventoryCleaner • Drop Filter")
                            .method_27694(st -> st.method_10982(true)),
                    left, 12, 0xFFFFFFFF, false);
            d.method_51439(field_22793,
                    class_2561.method_43470("Click an item to add/remove it from the filter."),
                    left, 80, 0xFF9CA8B1, false);
            d.method_51439(field_22793,
                    class_2561.method_43470("Selected: " + selected().size()),
                    right - 110, 80, 0xFFFFFFFF, false);

            List<ItemEntry> entries = matches();
            int rowH = 38;
            int viewportH = bottom - top;
            int contentH = Math.max(viewportH, entries.size() * rowH + 18);
            int maxScroll = Math.max(0, contentH - viewportH);
            scroll = class_3532.method_15350(scroll, 0, maxScroll);

            d.method_44379(left, top, right - 8, bottom);
            int y = top + 18 - (int) scroll;
            Set<String> selected = selected();
            for (ItemEntry e : entries) {
                if (y + rowH >= top && y <= bottom) {
                    boolean on = selected.contains(e.id.toString());
                    d.method_25294(left + 2, y, right - 18, y + rowH - 4,
                            on ? 0xFF1D3C4D : 0x9A121C24);
                    if (on) d.method_49601(left + 2, y, 3, rowH - 4, 0xFFFFFFFF);
                    d.method_51427(e.stack, left + 10, y + 7);
                    drawText(d, fitText(e.name, 280), left + 48, y + 5, 0xFFFFFFFF, false);
                    drawText(d, e.id.toString(), left + 48, y + 20, 0xFF8E9AA3, false);
                    drawText(d, on ? "ADDED" : "ADD", right - 72, y + 12, 0xFFFFFFFF, true);
                }
                y += rowH;
            }
            d.method_44380();

            if (contentH > viewportH) {
                int trackH = viewportH;
                int thumbH = Math.max(28, (int) Math.round(viewportH * (viewportH / (double) contentH)));
                int travel = Math.max(1, trackH - thumbH);
                int thumbY = top + (maxScroll == 0 ? 0
                        : (int) Math.round((scroll / maxScroll) * travel));
                d.method_25294(right - 8, top, right, bottom, 0x401A252E);
                d.method_25294(right - 8, thumbY, right, thumbY + thumbH, 0xFF6C7D89);
            }

            drawText(d, "ESC: back", left, field_22790 - 18, 0xFF73808B, false);
        }

        @Override
        public boolean method_25402(double mouseX, double mouseY, int button) {
            if (button != 0) return super.method_25402(mouseX, mouseY, button);
            int left = Math.max(24, field_22789 / 2 - 290);
            int right = Math.min(field_22789 - 24, field_22789 / 2 + 290);
            int top = 72;
            int bottom = field_22790 - 34;
            int viewportH = bottom - top;
            List<ItemEntry> entries = matches();
            int contentH = Math.max(viewportH, entries.size() * 38 + 18);

            if (mouseX >= right - 10 && mouseX <= right && mouseY >= top && mouseY <= bottom
                    && contentH > viewportH) {
                int thumbH = Math.max(28, (int) Math.round(viewportH * (viewportH / (double) contentH)));
                int travel = Math.max(1, viewportH - thumbH);
                int maxScroll = Math.max(0, contentH - viewportH);
                int thumbY = top + (maxScroll == 0 ? 0 : (int) Math.round((scroll / maxScroll) * travel));
                if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                    draggingScrollbar = true;
                    dragOffset = mouseY - thumbY;
                    return true;
                }
            }

            if (mouseX < left || mouseX > right - 12 || mouseY < top || mouseY > bottom) return true;
            int y = top + 18 - (int) scroll;
            Set<String> set = selected();
            for (ItemEntry e : entries) {
                if (mouseY >= y && mouseY < y + 34) {
                    String id = e.id.toString();
                    if (!set.add(id)) set.remove(id);
                    save(set);
                    return true;
                }
                y += 38;
                if (y > bottom + 38) break;
            }
            return true;
        }

        @Override
        public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            int top = 72;
            int bottom = field_22790 - 34;
            int viewportH = bottom - top;
            int max = Math.max(0, matches().size() * 38 + 18 - viewportH);
            scroll = class_3532.method_15350(scroll - verticalAmount * 38, 0, max);
            return true;
        }

        @Override
        public boolean method_25403(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (button == 0 && draggingScrollbar) {
                int top = 72;
                int bottom = field_22790 - 34;
                int viewportH = bottom - top;
                int contentH = Math.max(viewportH, matches().size() * 38 + 18);
                int thumbH = Math.max(28, (int) Math.round(viewportH * (viewportH / (double) contentH)));
                int travel = Math.max(1, viewportH - thumbH);
                int maxScroll = Math.max(0, contentH - viewportH);
                int desired = (int) (mouseY - dragOffset - top);
                int rel = class_3532.method_15340(desired, 0, travel);
                scroll = maxScroll == 0 ? 0 : (rel / (double) travel) * maxScroll;
                return true;
            }
            return super.method_25403(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean method_25406(double mouseX, double mouseY, int button) {
            if (button == 0) draggingScrollbar = false;
            return super.method_25406(mouseX, mouseY, button);
        }

        @Override
        public boolean method_25404(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                field_22787.method_1507(parent);
                return true;
            }
            return super.method_25404(keyCode, scanCode, modifiers);
        }

        private record ItemEntry(class_2960 id, String name, class_1799 stack) {}
    }

    /** Dedicated item/block picker for Nuker Filter. Search by translated item name or registry id,
     * show the actual item icon, and toggle block ids without requiring manual id entry. */
    private static final class BlockPickerScreen extends class_437 {
        private final PoeScreen parent;
        private class_342 search;
        private double scroll;
        private boolean draggingPickerScrollbar;
        private double pickerDragOffset;

        private final boolean nukerMode;

        BlockPickerScreen(PoeScreen parent, boolean nukerMode) {
            super(class_2561.method_43470(nukerMode ? "Nuker - Edit Blocks" : "X-Ray - Edit Blocks"));
            this.parent = parent;
            this.nukerMode = nukerMode;
        }

        @Override
        protected void method_25426() {
            int w = Math.min(520, field_22789 - 40);
            search = new class_342(field_22793, field_22789 / 2 - w / 2, 34, w, 22, class_2561.method_43470("Search blocks"));
            search.method_1880(96);
            search.method_47404(class_2561.method_43470("Type item/block name or minecraft:id..."));
            method_37063(search);
        }

        private List<Entry> matches() {
            String q = search == null ? "" : search.method_1882().trim().toLowerCase(Locale.ROOT);
            List<Entry> out = new ArrayList<>();
            for (class_2960 id : class_7923.field_41178.method_10235()) {
                class_1792 item = class_7923.field_41178.method_63535(id);
                if (!(item instanceof class_1747)) continue;
                class_1799 stack = item.method_7854();
                String name = stack.method_7964().getString();
                String key = id.toString();
                if (q.isBlank() || name.toLowerCase(Locale.ROOT).contains(q) || key.contains(q)) {
                    class_2248 block = ((class_1747) item).method_7711();
                    class_2960 blockId = class_7923.field_41175.method_10221(block);
                    out.add(new Entry(blockId, name, stack));
                }
            }
            out.sort((a, b) -> {
                String aa = (a.name + " " + a.id).toLowerCase(Locale.ROOT);
                String bb = (b.name + " " + b.id).toLowerCase(Locale.ROOT);
                boolean ap = !q.isBlank() && aa.startsWith(q);
                boolean bp = !q.isBlank() && bb.startsWith(q);
                if (ap != bp) return ap ? -1 : 1;
                return a.name.compareToIgnoreCase(b.name);
            });
            return out;
        }

        private Set<String> selectedSet() {
            if (!nukerMode) return new LinkedHashSet<>(PeoClient.CFG.xrayBlocks);
            Set<String> set = new LinkedHashSet<>();
            String raw = PeoClient.CFG.nukerFilterIds == null ? "" : PeoClient.CFG.nukerFilterIds;
            for (String s : raw.split("[,\n\s]+")) if (!s.isBlank()) set.add(s.trim());
            return set;
        }

        private void saveSet(Set<String> set) {
            if (!nukerMode) {
                PeoClient.CFG.xrayBlocks = new LinkedHashSet<>(set);
            } else {
                PeoClient.CFG.nukerFilterIds = String.join(",", set);
            }
            PeoClient.CFG.save();
            if (!nukerMode) PeoClient.reload(class_310.method_1551());
        }

        @Override
        public void method_25394(class_332 d, int mouseX, int mouseY, float delta) {
            method_25420(d, mouseX, mouseY, delta);
            int left = Math.max(24, field_22789 / 2 - 270);
            int right = Math.min(field_22789 - 24, field_22789 / 2 + 270);
            int top = 72;
            int bottom = field_22790 - 34;

            d.method_25294(left - 8, top - 8, right + 8, bottom + 8, 0xEE0D151D);
            d.method_49601(left - 8, top - 8, right - left + 16, bottom - top + 16, 0xFF2A4154);
            d.method_51439(field_22793, class_2561.method_43470((nukerMode ? "Nuker" : "X-Ray") + " • Edit blocks").method_27694(s -> s.method_10982(true)), left, 12, 0xFFFFFFFF, false);
            d.method_51439(field_22793, class_2561.method_43470("Click a block to add/remove it from the current filter."), left, 80, 0xFF9CA8B1, false);
            d.method_51439(field_22793, class_2561.method_43470("Selected: " + selectedSet().size()), right - 110, 80, 0xFFFFFFFF, false);

            List<Entry> entries = matches();
            int rowH = 34;
            int y = top + 18 - (int) scroll;
            Set<String> selected = selectedSet();
            for (Entry e : entries) {
                if (y + rowH >= top && y <= bottom) {
                    boolean on = selected.contains(e.id.toString());
                    int bg = on ? 0xFF1C3547 : 0xB7152029;
                    d.method_25294(left, y, right, y + rowH - 4, bg);
                    d.method_49601(left, y, right - left, rowH - 4, 0xFF294354);
                    d.method_51427(e.stack, left + 8, y + 5);
                    d.method_51439(field_22793, class_2561.method_43470(e.name).method_27694(s -> s.method_10982(on)), left + 38, y + 5, 0xFFFFFFFF, false);
                    d.method_51439(field_22793, class_2561.method_43470(e.id.toString()), left + 38, y + 18, 0xFF8E9AA3, false);
                    d.method_51439(field_22793, class_2561.method_43470(on ? "SELECTED" : "ADD"), right - 80, y + 11, 0xFFFFFFFF, false);
                }
                y += rowH;
            }
            int viewportH = bottom - top;
            int contentH = Math.max(viewportH, entries.size() * rowH + 18);
            if (contentH > viewportH) {
                int trackX = right - 6;
                int trackTop = top;
                int trackBottom = bottom;
                int thumbH = Math.max(24, (int)((viewportH / (double)contentH) * viewportH));
                int travel = Math.max(1, viewportH - thumbH);
                int maxScroll = Math.max(0, contentH - viewportH);
                int thumbY = trackTop + (maxScroll == 0 ? 0 : (int)((scroll / maxScroll) * travel));
                d.method_25294(trackX, trackTop, trackX + 4, trackBottom, 0xFF243641);
                d.method_25294(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFFE1E6EA);
            }
            if (entries.isEmpty()) {
                d.method_51439(field_22793, class_2561.method_43470("No matching blocks."), left + 10, top + 35, 0xFFFFFFFF, false);
            }
            d.method_51439(field_22793, class_2561.method_43470("Mouse wheel: scroll • ESC: back"), left, bottom + 10, 0xFF98A5AE, false);
            super.method_25394(d, mouseX, mouseY, delta);
        }

        @Override
        public boolean method_25402(double mouseX, double mouseY, int button) {
            if (super.method_25402(mouseX, mouseY, button)) return true;
            if (button != 0) return false;
            int left = Math.max(24, field_22789 / 2 - 270);
            int right = Math.min(field_22789 - 24, field_22789 / 2 + 270);
            int top = 72;
            int bottom = field_22790 - 34;
            if (mouseX < left || mouseX > right || mouseY < top || mouseY > bottom) return false;
            List<Entry> entries = matches();
            int rowH = 34;
            int viewportH = bottom - top;
            int contentH = Math.max(viewportH, entries.size() * rowH + 18);
            if (button == 0 && contentH > viewportH && mouseX >= right - 12) {
                int thumbH = Math.max(24, (int)((viewportH / (double)contentH) * viewportH));
                int travel = Math.max(1, viewportH - thumbH);
                int maxScroll = Math.max(0, contentH - viewportH);
                int thumbY = top + (maxScroll == 0 ? 0 : (int)((scroll / maxScroll) * travel));
                if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                    draggingPickerScrollbar = true;
                    pickerDragOffset = mouseY - thumbY;
                    return true;
                }
            }
            int y = top + 18 - (int) scroll;
            for (Entry e : matches()) {
                if (mouseY >= y && mouseY < y + rowH - 4) {
                    Set<String> set = selectedSet();
                    String id = e.id.toString();
                    if (!set.add(id)) set.remove(id);
                    saveSet(set);
                    return true;
                }
                y += rowH;
                if (y > bottom + rowH) break;
            }
            return true;
        }

        @Override
        public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            int rowH = 34;
            int visible = Math.max(1, (field_22790 - 120) / rowH);
            int max = Math.max(0, matches().size() - visible);
            scroll = class_3532.method_15350(scroll - verticalAmount * rowH, 0, max * rowH);
            return true;
        }

        @Override
        public boolean method_25403(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (button == 0 && draggingPickerScrollbar) {
                int top = 72;
                int bottom = field_22790 - 34;
                int viewportH = bottom - top;
                int contentH = Math.max(viewportH, matches().size() * 34 + 18);
                int thumbH = Math.max(24, (int)((viewportH / (double)contentH) * viewportH));
                int travel = Math.max(1, viewportH - thumbH);
                int maxScroll = Math.max(0, contentH - viewportH);
                int desired = (int)(mouseY - pickerDragOffset - top);
                int rel = class_3532.method_15340(desired, 0, travel);
                scroll = maxScroll == 0 ? 0 : (rel / (double)travel) * maxScroll;
                return true;
            }
            return super.method_25403(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean method_25406(double mouseX, double mouseY, int button) {
            if (button == 0) draggingPickerScrollbar = false;
            return super.method_25406(mouseX, mouseY, button);
        }

        @Override
        public boolean method_25404(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { field_22787.method_1507(parent); return true; }
            return super.method_25404(keyCode, scanCode, modifiers);
        }

        private record Entry(class_2960 id, String name, class_1799 stack) {}
    }

}
