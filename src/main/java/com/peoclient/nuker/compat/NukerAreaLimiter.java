package com.peoclient.nuker.compat;

import com.peoclient.PeoClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

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
    private static Vec3d center;
    private static double range;

    private NukerAreaLimiter() {}

    public static void registerRender() {
        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;
            VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());

            Vec3d camera = context.camera().getPos();

            // Range highlight: visible while enabled, centered on player while enabled.
            if (PeoClient.CFG.nukerRangeHighlight && locked && center != null) {
                Box box = new Box(
                        center.x - range, center.y - range, center.z - range,
                        center.x + range, center.y + range, center.z + range
                ).offset(-camera.x, -camera.y, -camera.z);
                int[] rgb = parseColor(PeoClient.CFG.nukerRangeColor, 255, 0, 0);
                float alpha = 1.0f;
                VertexRendering.drawBox(context.matrixStack(), vc, box,
                        rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f, alpha);
            }

            // Active block highlight: equivalent purpose to BleachHack's Highlight setting.
            if (PeoClient.CFG.nukerHighlight) {
                int[] rgb = parseColor(PeoClient.CFG.nukerHighlightColor, 255, 128, 128);
                float progress = PeoClient.NukerLogic.getBreakingProgress();
                for (BlockPos pos : PeoClient.NukerLogic.getRenderBlocks()) {
                    if (mc.world.getBlockState(pos).isAir()) continue;
                    Box full = new Box(pos).offset(-camera.x, -camera.y, -camera.z);
                    Box draw = full;
                    float alpha = Math.max(0.15f, Math.min(1.0f, progress));
                    if ("Expand".equalsIgnoreCase(PeoClient.CFG.nukerHighlightMode)) {
                        Vec3d c = full.getCenter();
                        double sx = Math.max(0.02, (full.maxX - full.minX) * Math.max(0.05, progress) * 0.5);
                        double sy = Math.max(0.02, (full.maxY - full.minY) * Math.max(0.05, progress) * 0.5);
                        double sz = Math.max(0.02, (full.maxZ - full.minZ) * Math.max(0.05, progress) * 0.5);
                        draw = new Box(c.x - sx, c.y - sy, c.z - sz, c.x + sx, c.y + sy, c.z + sz);
                        alpha = 0.55f;
                    }
                    VertexRendering.drawBox(context.matrixStack(), vc, draw,
                            rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f, alpha);
                }
            }
        });
    }

    public static void tick(MinecraftClient mc, boolean highlightEnabled, double configuredRange) {
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        double r = Math.max(1.0, Math.min(6.0, configuredRange));
        // While visible, keep the area centered on the player. This is what makes
        // the highlight behave like a player-centered mining range.
        if (highlightEnabled) {
            center = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
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

    public static boolean contains(BlockPos pos) {
        if (!isLocked()) return true;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        return x >= center.x - range && x <= center.x + range
                && y >= center.y - range && y <= center.y + range
                && z >= center.z - range && z <= center.z + range;
    }

    public static Vec3d getCenter() { return center; }
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
