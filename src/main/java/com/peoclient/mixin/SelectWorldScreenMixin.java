package com.peoclient.mixin;

import com.peoclient.AccountScreen;
import com.peoclient.PeoClient;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public final class SelectWorldScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void peo$addAccountButton(CallbackInfo ci) {
        SelectWorldScreen screen = (SelectWorldScreen) (Object) this;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        ((ScreenAccessor) screen).peo$addDrawableChild(ButtonWidget.builder(
                net.minecraft.text.Text.literal("Peo Account: " + PeoClient.getDisplayUsername()),
                b -> mc.setScreen(new AccountScreen(screen)))
                .dimensions(10, 10, 190, 20).build());
    }
}
