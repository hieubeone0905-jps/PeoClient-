package com.peoclient.mixin;

import net.minecraft.class_320;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_320.class)
public interface SessionAccessor {
    @Mutable
    @Accessor("field_1982")
    void peo$setUsername(String username);
}
