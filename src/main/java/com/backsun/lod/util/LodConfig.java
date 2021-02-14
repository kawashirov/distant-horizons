package com.backsun.lod.util;

import com.backsun.lod.util.enums.FogDistance;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 
 * @author James Seibel
 * @version 02-14-2021
 */
@Config(modid = Reference.MOD_ID)
public class LodConfig
{
	// save the config file when it is changed
	@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
	private static class EventHandler
	{
		@SubscribeEvent
		public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event)
		{
			if (event.getModID().equals(Reference.MOD_ID))
			{
				ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
			}
		}
	}
	
	
	@Config.Comment(
			{"Enable LODs", 
			"If true LODs will be drawn, if false LODs will "
			+ "not be rendered. However they will "
			+ "still be generated and stored in your world's save folder."})
	public static boolean drawLODs = true;
	
	@Config.Comment(
			{"Fog Distance", 
			"What distance should Fog be drawn on the LODs?"})
	public static FogDistance fogDistance = FogDistance.NEAR_AND_FAR;
	
	@Config.Comment(
			{"Draw Debugging Checkerboard", 
			"If false the LODs will draw with their normal world colors."
			+ "If true they will draw as a black and white checkerboard."
			+ "This can be used for debugging or imagining you are playing a "
			+ "giant game of chess ;)"})
	public static boolean drawCheckerBoard = false;
	
	
}
