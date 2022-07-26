package com.seibel.lod;

import com.seibel.lod.common.wrappers.DependencySetup;
import com.seibel.lod.common.wrappers.minecraft.MinecraftDedicatedServerWrapper;
import com.seibel.lod.core.api.internal.a7.SharedApi;
import com.seibel.lod.core.util.LodUtil;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.dedicated.DedicatedServer;

@Environment(EnvType.SERVER)
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
            LodUtil.assertTrue(server instanceof DedicatedServer);
            MinecraftDedicatedServerWrapper.INSTANCE.dedicatedServer = (DedicatedServer) server;
            FabricMain.postInit();
            SharedApi.LOGGER.info("Dedicated server inited at {}", server.getServerDirectory());
        });
    }
}
