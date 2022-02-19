package com.seibel.lod.fabric.mixins;

import com.seibel.lod.fabric.Main;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class MixinDedicatedServer {
    @Inject(method = "initServer", at = @At("TAIL"))
    public void initServer(CallbackInfoReturnable<Boolean> cir) {
        Main.initServer();
    }
}
