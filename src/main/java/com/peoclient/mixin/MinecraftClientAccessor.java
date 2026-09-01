package com.peoclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.Proxy;
import net.minecraft.class_310;

@Mixin(class_310.class)
public interface MinecraftClientAccessor {
    @Mutable
    @Accessor("field_1739")
    void peo$setNetworkProxy(Proxy proxy);
}
