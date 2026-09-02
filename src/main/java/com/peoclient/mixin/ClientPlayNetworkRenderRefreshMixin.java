package com.peoclient.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Kept as a separate mixin slot for compatibility with existing configs.
 * Render repair is performed in ClientWorldRenderUpdateMixin at the exact
 * ClientWorld#setBlockState call, where old/new states are available.
 */
@Mixin(targets = "net.minecraft.class_634")
public final class ClientPlayNetworkRenderRefreshMixin {
}
