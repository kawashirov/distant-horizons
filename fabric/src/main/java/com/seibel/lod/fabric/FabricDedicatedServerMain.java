package com.seibel.lod.fabric;

import com.seibel.lod.common.wrappers.DependencySetup;
import com.seibel.lod.common.wrappers.minecraft.MinecraftDedicatedServerWrapper;
import com.seibel.lod.core.util.LodUtil;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.dedicated.DedicatedServer;

public class FabricDedicatedServerMain implements DedicatedServerModInitializer {
    public static FabricServerProxy server_proxy;
    public boolean hasPostSetupDone = false;

    @Override
    public void onInitializeServer() {
        DependencySetup.createServerBindings();
        FabricMain.init();

        server_proxy = new FabricServerProxy(true);
        server_proxy.registerEvents();

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            if (hasPostSetupDone) return;
            hasPostSetupDone = true;
            FabricMain.postInit();
            LodUtil.assertTrue(server instanceof DedicatedServer);
            MinecraftDedicatedServerWrapper.INSTANCE.dedicatedServer = (DedicatedServer) server;
        });
    }
}
