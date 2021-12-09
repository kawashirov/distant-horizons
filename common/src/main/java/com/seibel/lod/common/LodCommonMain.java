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


    public static void initConfig() {
        ConfigGui.init(ModInfo.ID, Config.class);
    }
}
