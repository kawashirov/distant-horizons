package com.seibel.lod.fabric.mixins;

import com.seibel.lod.fabric.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void startMod(GameConfig gameConfig, CallbackInfo ci) {
        Main.init();
    }
}
