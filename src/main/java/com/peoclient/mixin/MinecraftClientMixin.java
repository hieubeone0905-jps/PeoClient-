
package com.peoclient.mixin;
import com.peoclient.PeoClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method="tick", at=@At("HEAD"))
    private void peo$gamma(CallbackInfo ci) {
        MinecraftClient mc=(MinecraftClient)(Object)this;
        if (mc.player==null) return;
        if (PeoClient.CFG.fullbright && PeoClient.CFG.fullbrightMode==1) {
            mc.options.getGamma().setValue(Math.min(1.0, PeoClient.CFG.fullbrightGamma/12.0));
        } else if (!PeoClient.CFG.fullbright && PeoClient.CFG.savedGamma>=0) {
            mc.options.getGamma().setValue(PeoClient.CFG.savedGamma);
        }
    }
}
