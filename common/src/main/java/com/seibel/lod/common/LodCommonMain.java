package com.seibel.lod.common;

import com.seibel.lod.common.forge.LodForgeMethodCaller;
import com.seibel.lod.common.wrappers.DependencySetup;
import com.seibel.lod.common.wrappers.config.ConfigGui;
import com.seibel.lod.core.ModInfo;

/**
 * This is the common main class
 * @author Ran
 */
public class LodCommonMain {
    public static boolean forge = false;
    public static LodForgeMethodCaller forgeMethodCaller;

    public static void startup(LodForgeMethodCaller caller) {
        if (caller != null) {
            LodCommonMain.forge = true;
            forgeMethodCaller = caller;
        }

        DependencySetup.createInitialBindings();
    }


    // TODO[CONFIG]: Find a better way to initialise everything
    public static void initConfig() {
        ConfigGui.init(ModInfo.ID, Config.class);
        ConfigGui.init(ModInfo.ID, Config.Client.class);
        ConfigGui.init(ModInfo.ID, Config.Client.Graphics.class);
        ConfigGui.init(ModInfo.ID, Config.Client.Graphics.Quality.class);
        ConfigGui.init(ModInfo.ID, Config.Client.Graphics.FogQuality.class);
        ConfigGui.init(ModInfo.ID, Config.Client.Graphics.AdvancedGraphics.class);
        ConfigGui.init(ModInfo.ID, Config.Client.WorldGenerator.class);
        ConfigGui.init(ModInfo.ID, Config.Client.Advanced.class);
        ConfigGui.init(ModInfo.ID, Config.Client.Advanced.Threading.class);
        ConfigGui.init(ModInfo.ID, Config.Client.Advanced.Debugging.class);
        ConfigGui.init(ModInfo.ID, Config.Client.Advanced.Buffers.class);
    }
}
