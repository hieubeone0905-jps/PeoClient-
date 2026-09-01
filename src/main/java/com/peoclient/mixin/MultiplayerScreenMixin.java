package com.peoclient.mixin;

import com.peoclient.AccountScreen;
import com.peoclient.PeoClient;
import net.minecraft.class_4185;
import net.minecraft.class_500;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_500.class)
public final class MultiplayerScreenMixin {
    @Inject(method = "method_25426", at = @At("TAIL"))
    private void peo$addAccountButton(CallbackInfo ci) {
        class_500 screen = (class_500) (Object) this;
        var mc = net.minecraft.class_310.method_1551();
        ((ScreenAccessor) screen).peo$addDrawableChild(class_4185.method_46430(
                net.minecraft.class_2561.method_43470("Peo Account: " + PeoClient.getDisplayUsername()),
                b -> mc.method_1507(new AccountScreen(screen)))
                .method_46434(10, 10, 190, 20).method_46431());
    }
}
