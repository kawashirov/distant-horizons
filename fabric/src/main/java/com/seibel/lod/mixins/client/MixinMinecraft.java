package com.seibel.lod.mixins.client;

import com.seibel.lod.common.wrappers.gui.UpdateModScreen;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.api.internal.a7.ClientApi;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.jar.JarUtils;
import com.seibel.lod.core.jar.installer.ModrinthGetter;
import com.seibel.lod.core.jar.installer.WebDownloader;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * This is used to check for updates to the mod.
 *
 * @author coolGi
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {
    private static boolean deleteOldOnClose = false;

    @Redirect(
            method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
    )
    public void onOpenScreen(Minecraft instance, Screen guiScreen) {
        if (!Config.Client.AutoUpdater.enableAutoUpdater.get()) // Don't do anything if the user doesn't want it
            return;

        // Some init stuff
        // We use sha1 to check the version as our versioning system is diffrent to the one on modrinth
        if (!ModrinthGetter.init()) return;
        String jarSha = "";
        try { jarSha = JarUtils.getFileChecksum(MessageDigest.getInstance("SHA"), JarUtils.jarFile);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        String mcVersion = SingletonInjector.INSTANCE.get(IVersionConstants.class).getMinecraftVersion();

        // Check the sha's of both our stuff
        if (jarSha.equals(ModrinthGetter.getLatestShaForVersion(mcVersion)))
            return;


        ClientApi.LOGGER.info("New version ("+ModrinthGetter.getLatestNameForVersion(mcVersion)+") of "+ModInfo.READABLE_NAME+" is available");
        if (Config.Client.AutoUpdater.promptForUpdate.get()) {
            Objects.requireNonNull(Minecraft.getInstance()).setScreen(new UpdateModScreen(
                    new TitleScreen(false), // We don't want to use the vanilla title screen as it would fade the buttons
                    ModrinthGetter.getLatestNameForVersion(mcVersion)
            )); // Just uncommenting this to not annoy other devs for now
        } else {
            // Auto-update mod
            try {
                ClientApi.LOGGER.info("Attempting to auto update "+ModInfo.READABLE_NAME);
                WebDownloader.downloadAsFile(ModrinthGetter.getLatestDownloadForVersion(mcVersion), JarUtils.jarFile.getParentFile().toPath().resolve(ModInfo.NAME+"-"+ModrinthGetter.getLatestNameForVersion(mcVersion)+".jar").toFile());
                deleteOldOnClose = true;
                ClientApi.LOGGER.info(ModInfo.READABLE_NAME+" successfully updated. It will apply on game's relaunch");
            } catch (Exception e) {
                ClientApi.LOGGER.info("Failed to update "+ModInfo.READABLE_NAME+" to version "+ModrinthGetter.getLatestNameForVersion(mcVersion));
                e.printStackTrace();
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "close()V")
    public void close(CallbackInfo ci) {
        if (deleteOldOnClose || UpdateModScreen.modUpdated) {
            try {
                Files.delete(JarUtils.jarFile.toPath());
            } catch (Exception e) {
                ClientApi.LOGGER.warn("Failed to delete previous " + ModInfo.READABLE_NAME + " file, please delete it manually at [" + JarUtils.jarFile + "]");
                e.printStackTrace();
            }
        }
    }
}
