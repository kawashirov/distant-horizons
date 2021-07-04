package com.seibel.lod;

import org.lwjgl.opengl.GL;

import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.render.LodRenderer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Initialize and setup the Mod.
 * <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 * 
 * @author James Seibel
 * @version 7-3-2021
 */
@Mod(ModInfo.MODID)
public class LodMain
{
	public static LodMain instance;
	
	public static ClientProxy client_proxy;
	
	
	@SuppressWarnings("deprecation")
	private void init(final FMLCommonSetupEvent event)
	{
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LodConfig.clientSpec);
		
		
		Thread setFancyFog = new Thread(() ->
		{ 
			LodRenderer.fancyFogAvailable = GL.getCapabilities().GL_NV_fog_distance;
			
			if (!LodRenderer.fancyFogAvailable)
			{
				ClientProxy.LOGGER.info("This GPU does not support GL_NV_fog_distance. This means that fancy fog options will not be available.");
			}
		});
		
		// This will be run on the main thread when it is able.
		// If it wasn't run on the main thread GL.getCapabilities()
		// would fail.
		DeferredWorkQueue.runLater(setFancyFog);
	}
	
	
    public LodMain()
    {
        // Register the methods
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientStart);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onClientStart(final FMLClientSetupEvent event)
    {
    	client_proxy = new ClientProxy();
		MinecraftForge.EVENT_BUS.register(client_proxy);
    }
    
    
    
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event)
    {
        // this is called when the server starts
    }
    
}
