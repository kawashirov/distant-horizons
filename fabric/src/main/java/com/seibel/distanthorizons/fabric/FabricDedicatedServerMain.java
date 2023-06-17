package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftDedicatedServerWrapper;
import com.seibel.distanthorizons.core.util.LodUtil;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.SERVER)
public class FabricDedicatedServerMain implements DedicatedServerModInitializer
{
	private static final Logger LOGGER = LogManager.getLogger(FabricDedicatedServerMain.class.getSimpleName());
	
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
            LOGGER.info("Dedicated server inited at {}", server.getServerDirectory());
        });
    }
}
