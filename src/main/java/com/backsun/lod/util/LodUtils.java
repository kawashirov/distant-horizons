package com.backsun.lod.util;

import com.backsun.lod.objects.LodRegion;
import com.backsun.lod.objects.RegionPos;

import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerWorld;

/**
 * This class holds methods that may be used in multiple places.
 * 
 * @author James Seibel
 * @version 03-19-2021
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
	
	/**
	 * Convert the given ChunkPos into a RegionPos.
	 */
	public static RegionPos convertChunkPosToRegionPos(ChunkPos pos)
	{
		RegionPos rPos = new RegionPos();
		rPos.x = pos.x / LodRegion.SIZE;
		rPos.z = pos.z / LodRegion.SIZE;
		
		// prevent issues if X/Z is negative and less than 16
		if (pos.x < 0)
		{
			rPos.x = (Math.abs(rPos.x) * -1) - 1; 
		}
		if (pos.z < 0)
		{
			rPos.z = (Math.abs(rPos.z) * -1) - 1; 
		}
		
		return rPos;
	}
}
