package com.seibel.lod.fabric;

import com.seibel.lod.common.wrappers.DependencySetup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

@Environment(EnvType.CLIENT)
public class FabricClientMain implements ClientModInitializer
{
	public static FabricClientProxy client_proxy;
	public static FabricServerProxy server_proxy;
	
	
	// Do if implements ClientModInitializer
	// This loads the mod before minecraft loads which causes a lot of issues
	@Override
	public void onInitializeClient()
	{
		DependencySetup.createClientBindings();
		FabricMain.init();
		
		server_proxy = new FabricServerProxy(false);
		server_proxy.registerEvents();
		
		client_proxy = new FabricClientProxy();
		client_proxy.registerEvents();
		
		ClientLifecycleEvents.CLIENT_STARTED.register((mc) -> FabricMain.postInit());
	}
	
}
