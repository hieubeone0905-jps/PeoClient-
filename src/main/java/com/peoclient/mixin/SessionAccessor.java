package com.peoclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.class_320.class)
public interface SessionAccessor {
    @Mutable
    @Accessor("username")
    void peo$setUsername(String username);
}
