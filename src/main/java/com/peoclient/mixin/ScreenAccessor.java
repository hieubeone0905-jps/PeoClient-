package com.peoclient.mixin;

import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_437;
import net.minecraft.class_6379;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.gui.screen.Screen")
public interface ScreenAccessor {
    @Invoker("addDrawableChild")
    <T extends class_364 & class_4068 & class_6379> T peo$addDrawableChild(T drawableElement);
}
