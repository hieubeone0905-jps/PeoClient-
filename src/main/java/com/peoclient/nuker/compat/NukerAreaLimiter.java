package com.peoclient.nuker.compat;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Persistent Nuker work-area.
 *
 * When RangeHighlight is switched ON, the current player position and range
 * are captured as a work area. Turning the visual highlight OFF only hides
 * the box; the work area remains active until it is re-enabled (which
 * captures a new area).
 */
public final class NukerAreaLimiter {
    private static boolean locked;
    private static boolean lastHighlightState;
    private static Vec3d center;
    private static double range;

    private NukerAreaLimiter() {}

    public static void registerRender() {
        WorldRenderEvents.LAST.register(context -> {
            if (!locked || lastHighlightState || center == null) return;

            // Hidden state: intentionally do not draw the box.
        });

        WorldRenderEvents.LAST.register(context -> {
            if (!locked || !lastHighlightState || center == null) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;
            VertexConsumer vertexConsumer = consumers.getBuffer(RenderLayer.getLines());

            Vec3d cameraPos = context.camera().getPos();
            Box box = new Box(
                    center.x - range, center.y - range, center.z - range,
                    center.x + range, center.y + range, center.z + range
            ).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            WorldRenderer.drawBox(
                    context.matrixStack(), vertexConsumer, box,
                    1.0f, 0.25f, 0.25f, 1.0f
            );
        });
    }

    public static void tick(MinecraftClient mc, boolean highlightEnabled, double configuredRange) {
        if (mc.player == null || mc.world == null) {
            locked = false;
            center = null;
            lastHighlightState = false;
            return;
        }

        if (highlightEnabled && !lastHighlightState) {
            lock(mc, configuredRange);
        }

        lastHighlightState = highlightEnabled;
    }

    private static void lock(MinecraftClient mc, double configuredRange) {
        center = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        range = Math.max(1.0, Math.min(6.0, configuredRange));
        locked = true;
    }

    public static boolean isLocked() {
        return locked && center != null;
    }

    public static boolean contains(BlockPos pos) {
        if (!isLocked()) return true;
        return pos.getX() + 0.5 >= center.x - range
                && pos.getX() + 0.5 <= center.x + range
                && pos.getY() + 0.5 >= center.y - range
                && pos.getY() + 0.5 <= center.y + range
                && pos.getZ() + 0.5 >= center.z - range
                && pos.getZ() + 0.5 <= center.z + range;
    }

    public static void reset() {
        locked = false;
        center = null;
        range = 0;
        lastHighlightState = false;
    }
}
