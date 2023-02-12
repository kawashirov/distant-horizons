package com.seibel.lod.forge.mixins.client;

import com.seibel.lod.common.wrappers.gui.updater.UpdateModScreen;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.jar.installer.ModrinthGetter;
import com.seibel.lod.core.jar.updater.SelfUpdater;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author coolGi
 */
@Mixin(Minecraft.class)
public class MixinMinecraft
{
    @Redirect(
            method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
    )
    public void onOpenScreen(Minecraft instance, Screen guiScreen) {
        if (!Config.Client.AutoUpdater.enableAutoUpdater.get()) { // Don't do anything if the user doesn't want it
            instance.setScreen(guiScreen); // Sets the screen back to the vanilla screen as if nothing ever happened
            return;
        }

        if (SelfUpdater.onStart()) {
            instance.setScreen(new UpdateModScreen(
                    new TitleScreen(false), // We don't want to use the vanilla title screen as it would fade the buttons
                    ModrinthGetter.getLatestIDForVersion(SingletonInjector.INSTANCE.get(IVersionConstants.class).getMinecraftVersion())
            ));
        } else {
            instance.setScreen(guiScreen); // Sets the screen back to the vanilla screen as if nothing ever happened
        }
    }

    @Inject(at = @At("HEAD"), method = "close()V", remap = false)
    public void close(CallbackInfo ci) {
        SelfUpdater.onClose();
    }
}
