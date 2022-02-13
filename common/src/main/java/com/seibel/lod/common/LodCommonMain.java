package com.seibel.lod.common;

import com.seibel.lod.common.forge.LodForgeMethodCaller;
import com.seibel.lod.common.networking.NetworkInterface;
import com.seibel.lod.common.wrappers.DependencySetup;
import com.seibel.lod.common.wrappers.config.ConfigGui;
import com.seibel.lod.core.Config;
import com.seibel.lod.core.config.ConfigBase;

/**
 * This is the common main class
 * @author Ran
 */
public class LodCommonMain {
    public static boolean forge = false;
    public static boolean serverSided;
    public static LodForgeMethodCaller forgeMethodCaller;
    public static NetworkInterface networkInterface;

    public static final boolean IsNewConfig = false;

    public static void startup(LodForgeMethodCaller caller, boolean serverSided) {
        LodCommonMain.serverSided = serverSided;
        if (caller != null) {
            LodCommonMain.forge = true;
            forgeMethodCaller = caller;
        }

        DependencySetup.createInitialBindings();
    }


    public static void initConfig() {
        if (!IsNewConfig)
            ConfigGui.init(OldConfig.class);
    }

    public static void registerNetworking(NetworkInterface networkInterface) {
        LodCommonMain.networkInterface = networkInterface;
    }
}
