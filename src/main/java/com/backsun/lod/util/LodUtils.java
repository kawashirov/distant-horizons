package com.backsun.lod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerWorld;

/**
 * This class holds methods that may be used in multiple places.
 * 
 * @author James Seibel
 * @version 02-26-2021
 */
public class LodUtils
{
	private static Minecraft mc = Minecraft.getInstance();
	
	
	
	/**
	 * Gets the first valid ServerWorld.
	 * 
	 * @return null if there are no ServerWorlds
	 */
	public static ServerWorld getFirstValidServerWorld()
	{
		if (mc.getIntegratedServer() == null)
			return null;
		
		Iterable<ServerWorld> worlds = mc.getIntegratedServer().getWorlds();
		
		for (ServerWorld world : worlds)
			return world;
				
		return null;
	}
	
	/**
	 * Gets the ServerWorld for the relevant dimension.
	 * 
	 * @return null if there is no ServerWorld for the given dimension
	 */
	public static ServerWorld getServerWorldFromDimension(DimensionType dimension)
	{
		IntegratedServer server = mc.getIntegratedServer();
		if (server == null)
			return null;
		
		Iterable<ServerWorld> worlds = server.getWorlds();
		ServerWorld returnWorld = null;
		
		for (ServerWorld world : worlds)
		{
			if(world.getDimensionType() == dimension)
			{
				returnWorld = world;
				break;
			}
		}
				
		return returnWorld;
	}
}
