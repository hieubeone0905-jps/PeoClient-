package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.class_1921;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_9974;

/**
 * Nuker work-area + BleachHack-style visual highlight.
 *
 * While RangeHighlight is ON, the area follows the player and is centered on
 * the player's current position. When it is switched OFF, the last center and
 * range remain locked for Nuker targeting, but the visual box is hidden.
 */
public final class NukerAreaLimiter {
    private static boolean locked;
    private static boolean lastHighlightState;
    private static class_243 center;
    private static double range;

    private NukerAreaLimiter() {}

    public static void registerRender() {
        WorldRenderEvents.LAST.register(context -> {
            class_310 mc = class_310.method_1551();
            if (mc.field_1724 == null || mc.field_1687 == null) return;

            class_4597 consumers = context.consumers();
            if (consumers == null) return;
            class_4588 vc = consumers.getBuffer(class_1921.method_23594());

            class_243 camera = context.camera().method_19326();

            // Range highlight: visible while enabled, centered on player while enabled.
            if (PeoClient.CFG.nukerRangeHighlight && locked && center != null) {
                class_238 box = new class_238(
                        center.field_1352 - range, center.field_1351 - range, center.field_1350 - range,
                        center.field_1352 + range, center.field_1351 + range, center.field_1350 + range
                ).method_989(-camera.field_1352, -camera.field_1351, -camera.field_1350);
                int[] rgb = parseColor(PeoClient.CFG.nukerRangeColor, 255, 0, 0);
                float alpha = 1.0f;
                class_9974.method_62295(context.matrixStack(), vc, box,
                        rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f, alpha);
            }

            // Active block highlight: equivalent purpose to BleachHack's Highlight setting.
            if (PeoClient.CFG.nukerHighlight) {
                int[] rgb = parseColor(PeoClient.CFG.nukerHighlightColor, 255, 128, 128);
                float progress = PeoClient.NukerLogic.getBreakingProgress();
                for (class_2338 pos : PeoClient.NukerLogic.getRenderBlocks()) {
                    if (mc.field_1687.method_8320(pos).method_26215()) continue;
                    class_238 full = new class_238(pos).method_989(-camera.field_1352, -camera.field_1351, -camera.field_1350);
                    class_238 draw = full;
                    float alpha = Math.max(0.15f, Math.min(1.0f, progress));
                    if ("Expand".equalsIgnoreCase(PeoClient.CFG.nukerHighlightMode)) {
                        class_243 c = full.method_1005();
                        double sx = Math.max(0.02, (full.field_1320 - full.field_1323) * Math.max(0.05, progress) * 0.5);
                        double sy = Math.max(0.02, (full.field_1325 - full.field_1322) * Math.max(0.05, progress) * 0.5);
                        double sz = Math.max(0.02, (full.field_1324 - full.field_1321) * Math.max(0.05, progress) * 0.5);
                        draw = new class_238(c.field_1352 - sx, c.field_1351 - sy, c.field_1350 - sz, c.field_1352 + sx, c.field_1351 + sy, c.field_1350 + sz);
                        alpha = 0.55f;
                    }
                    class_9974.method_62295(context.matrixStack(), vc, draw,
                            rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f, alpha);
                }
            }
        });
    }

    public static void tick(class_310 mc, boolean highlightEnabled, double configuredRange) {
        if (mc.field_1724 == null || mc.field_1687 == null) {
            reset();
            return;
        }

        double r = Math.max(1.0, Math.min(6.0, configuredRange));
        // While visible, keep the area centered on the player. This is what makes
        // the highlight behave like a player-centered mining range.
        if (highlightEnabled) {
            center = new class_243(mc.field_1724.method_23317(), mc.field_1724.method_23318(), mc.field_1724.method_23321());
            range = r;
            locked = true;
        }

        lastHighlightState = highlightEnabled;
    }

    public static boolean isLocked() {
        return locked && center != null;
    }

    public static boolean isVisible() {
        return locked && lastHighlightState && center != null;
    }

    public static boolean contains(class_2338 pos) {
        if (!isLocked()) return true;
        double x = pos.method_10263() + 0.5;
        double y = pos.method_10264() + 0.5;
        double z = pos.method_10260() + 0.5;
        return x >= center.field_1352 - range && x <= center.field_1352 + range
                && y >= center.field_1351 - range && y <= center.field_1351 + range
                && z >= center.field_1350 - range && z <= center.field_1350 + range;
    }

    public static class_243 getCenter() { return center; }
    public static double getRange() { return range; }

    private static int[] parseColor(String raw, int r, int g, int b) {
        try {
            String[] p = raw == null ? new String[0] : raw.split(",");
            if (p.length >= 3) {
                return new int[]{clamp255(Integer.parseInt(p[0].trim())),
                        clamp255(Integer.parseInt(p[1].trim())),
                        clamp255(Integer.parseInt(p[2].trim()))};
            }
        } catch (Exception ignored) {}
        return new int[]{r, g, b};
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    public static void reset() {
        locked = false;
        center = null;
        range = 0;
        lastHighlightState = false;
    }
}
