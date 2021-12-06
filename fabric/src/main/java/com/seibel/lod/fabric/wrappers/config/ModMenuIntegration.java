package com.seibel.lod.fabric.wrappers.config;

import com.seibel.lod.common.wrappers.config.ConfigGui;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.common.Config;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * For making the config show up in modmenu
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    // For the custom config code
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> Config.getScreen(parent, ModInfo.ID, "");
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        HashMap<String, ConfigScreenFactory<?>> map = new HashMap<>();
        // For now it goes to the client option by default
        Config.configClass.forEach((modid, cClass) -> map.put(modid, parent -> ConfigGui.getScreen(parent, modid, "client")));
        return map;
    }
}
