package com.seibel.lod.fabric.mixins.events;

import com.seibel.lod.fabric.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class is used for world unloading events
 * @author Ran
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Shadow @Nullable public ClientLevel level;

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void unloadWorldEvent_sL(ClientLevel clientLevel, CallbackInfo ci) {
        if (level != null) Main.client_proxy.worldUnloadEvent(level);
    }

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void unloadWorldEvent_cL(Screen screen, CallbackInfo ci) {
        if (this.level != null) Main.client_proxy.worldUnloadEvent(this.level);
    }
}
