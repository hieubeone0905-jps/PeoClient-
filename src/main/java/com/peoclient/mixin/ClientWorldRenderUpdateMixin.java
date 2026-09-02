package com.peoclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.class_638;

/** Vanilla render-path test: leave ClientWorld#setBlockState completely untouched. */
@Mixin(class_638.class)
public final class ClientWorldRenderUpdateMixin {}
