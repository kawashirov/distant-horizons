package com.seibel.lod.common;

import com.seibel.lod.common.forge.LodForgeMethodCaller;
import com.seibel.lod.common.networking.NetworkInterface;
import com.seibel.lod.common.wrappers.DependencySetup;
import com.seibel.lod.common.wrappers.config.ConfigGui;

/**
 * This is the common main class
 * @author Ran
 */
public class LodCommonMain {
    public static boolean forge = false;
    public static boolean serverSided;
    public static LodForgeMethodCaller forgeMethodCaller;
    public static NetworkInterface networkInterface;

    public static void startup(LodForgeMethodCaller caller, boolean serverSided, NetworkInterface networkInterface) {
        LodCommonMain.serverSided = serverSided;
        if (caller != null) {
            LodCommonMain.forge = true;
            forgeMethodCaller = caller;
        }

        DependencySetup.createInitialBindings();

        LodCommonMain.networkInterface = networkInterface;
        if (!serverSided) {
            networkInterface.register_Client();
        } else {
            networkInterface.register_Server();
        }
    }


    public static void initConfig() {
        ConfigGui.init(Config.class);
    }
}
