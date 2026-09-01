package com.peoclient.mixin;

import com.peoclient.AccountScreen;
import com.peoclient.PeoClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public final class TitleScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void peo$addAccountButton(CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        int x = width / 2 - 100;
        int y = Math.min(height - 62, height / 4 + 108);
        ((ScreenAccessor) screen).peo$addDrawableChild(
                ButtonWidget.builder(Text.literal("Peo Account: " + PeoClient.getDisplayUsername()),
                        b -> mc.setScreen(new AccountScreen(screen)))
                        .dimensions(x, y + 2, 200, 20).build());
    }
}
