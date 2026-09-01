package com.peoclient.mixin;

import net.minecraft.class_7172;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.option.SimpleOption")
public interface SimpleOptionAccessor {
    @Accessor("value")
    void peo$setValue(Object value);
}
