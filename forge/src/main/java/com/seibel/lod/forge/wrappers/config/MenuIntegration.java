package com.seibel.lod.forge.wrappers.config;

import com.seibel.lod.common.Config;
import com.seibel.lod.core.ModInfo;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fmlclient.ConfigGuiHandler;

public class MenuIntegration {
    public static void registerModsPage() {
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((client, parent) -> Config.getScreen(parent,  "")));
    }
}
