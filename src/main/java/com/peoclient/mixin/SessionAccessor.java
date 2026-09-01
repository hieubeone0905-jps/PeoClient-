package com.peoclient.mixin;

import net.minecraft.class_320;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.session.Session")
public interface SessionAccessor {
    @Mutable
    @Accessor("username")
    void peo$setUsername(String username);
}
