package com.peoclient.mixin;

import com.peoclient.AccountScreen;
import com.peoclient.PeoClient;
import net.minecraft.class_4185;
import net.minecraft.class_442;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_442.class)
public final class TitleScreenMixin {
    @Inject(method = "method_25426", at = @At("TAIL"))
    private void peo$addAccountButton(CallbackInfo ci) {
        class_442 screen = (class_442) (Object) this;
        var mc = net.minecraft.class_310.method_1551();
        int width = mc.method_22683().method_4486();
        int height = mc.method_22683().method_4502();
        int x = width / 2 - 100;
        int y = Math.min(height - 62, height / 4 + 108);
        ((ScreenAccessor) screen).peo$addDrawableChild(class_4185.method_46430(
                net.minecraft.class_2561.method_43470("Peo Account: " + PeoClient.getDisplayUsername()),
                b -> net.minecraft.class_310.method_1551().method_1507(new AccountScreen(screen)))
                .method_46434(x, y + 2, 200, 20).method_46431());
    }
}
