package com.seibel.lod.fabric.mixins.client;

import com.seibel.lod.core.render.F3Screen;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class MixinDebugScreenOverlay {

    @Inject(method = "getSystemInformation", at = @At("RETURN"))
    private void addCustomF3(CallbackInfoReturnable<List<String>> cir) {
        List<String> messages = cir.getReturnValue();

        for (String i: F3Screen.f3List) {
            messages.add(i);
        }
    }

}
