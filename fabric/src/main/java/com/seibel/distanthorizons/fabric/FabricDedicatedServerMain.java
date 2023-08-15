package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.LodCommonMain;
import com.seibel.distanthorizons.common.wrappers.DependencySetup;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftDedicatedServerWrapper;
import com.seibel.distanthorizons.core.config.eventHandlers.presets.ThreadPresetConfigEventHandler;
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
	public void onInitializeServer()
	{
		DependencySetup.createServerBindings();
		FabricMain.init();
		
		// FIXME this prevents returning uninitialized Config values
		//  resulting from a circular reference mid-initialization in a static class
		// ThreadPresetConfigEventHandler <-> Config
		ThreadPresetConfigEventHandler.INSTANCE.toString();
		
		server_proxy = new FabricServerProxy(true);
		server_proxy.registerEvents();
		
		ServerLifecycleEvents.SERVER_STARTING.register((server) ->
		{
			if (this.hasPostSetupDone)
			{
				return;
			}
			
			this.hasPostSetupDone = true;
			LodUtil.assertTrue(server instanceof DedicatedServer);
			
			MinecraftDedicatedServerWrapper.INSTANCE.dedicatedServer = (DedicatedServer) server;
			LodCommonMain.initConfig();
			FabricMain.postInit();
			
			LOGGER.info("Dedicated server initialized at " + server.getServerDirectory());
		});
	}
	
}
