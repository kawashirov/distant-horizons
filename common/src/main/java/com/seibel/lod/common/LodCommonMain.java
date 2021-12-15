package com.seibel.lod.common;

import com.seibel.lod.common.forge.LodForgeMethodCaller;
import com.seibel.lod.common.wrappers.DependencySetup;
import com.seibel.lod.common.wrappers.config.ConfigGui;
import com.seibel.lod.common.wrappers.config.TinyConfig;
import com.seibel.lod.core.ModInfo;

/**
 * This is the common main class
 * @author Ran
 */
public class LodCommonMain {
    public static boolean forge = false;
    public static boolean serverSided;
    public static LodForgeMethodCaller forgeMethodCaller;

    public static void startup(LodForgeMethodCaller caller, boolean serverSided) {
        LodCommonMain.serverSided = serverSided;
        if (caller != null) {
            LodCommonMain.forge = true;
            forgeMethodCaller = caller;
        }

        DependencySetup.createInitialBindings();
    }


    public static void initConfig() {
        ConfigGui.init(ModInfo.ID, Config.class);
    }
//    public static void initConfig() {
//        TinyConfig.init(ModInfo.ID, Config.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.Advanced.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.Advanced.Buffers.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.Advanced.Debugging.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.Advanced.Threading.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.Graphics.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.Graphics.AdvancedGraphics.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.Graphics.FogQuality.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.Graphics.Quality.class);
//        TinyConfig.init(ModInfo.ID, Config.Client.WorldGenerator.class);
//    }
}
