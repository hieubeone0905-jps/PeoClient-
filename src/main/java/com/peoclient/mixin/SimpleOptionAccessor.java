package com.peoclient.mixin;

import net.minecraft.class_7172;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_7172.class)
public interface SimpleOptionAccessor {
    @Accessor("value")
    void peo$setValue(Object value);
}
