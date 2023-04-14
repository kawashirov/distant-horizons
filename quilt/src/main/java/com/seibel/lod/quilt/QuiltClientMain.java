package com.seibel.lod.quilt;

import com.seibel.lod.common.wrappers.DependencySetup;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.minecraft.ClientOnly;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientLifecycleEvents;

@ClientOnly
public class QuiltClientMain implements ClientModInitializer
{
	public static FabricClientProxy client_proxy;
	public static FabricServerProxy server_proxy;
	
	
	// Do if implements ClientModInitializer
	// This loads the mod before minecraft loads which causes a lot of issues
	@Override
	public void onInitializeClient(ModContainer mod) {
		DependencySetup.createClientBindings();
		FabricMain.init();

		server_proxy = new FabricServerProxy(false);
		server_proxy.registerEvents();

		client_proxy = new FabricClientProxy();
		client_proxy.registerEvents();

		ClientLifecycleEvents.READY.register((mc) -> FabricMain.postInit());
	}
}
