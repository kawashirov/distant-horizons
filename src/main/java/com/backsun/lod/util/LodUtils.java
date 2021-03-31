package com.backsun.lod.util;

import com.backsun.lod.objects.LodRegion;
import com.backsun.lod.objects.RegionPos;

import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

/**
 * This class holds methods that may be used in multiple places.
 * 
 * @author James Seibel
 * @version 03-31-2021
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
	
	/**
	 * Return whether the given chunk
	 * has any data in it.
	 */
	public static boolean chunkHasBlockData(IChunk chunk)
	{
		ChunkSection[] blockStorage = chunk.getSections();
		
		for(ChunkSection section : blockStorage)
		{
			if(section != null && !section.isEmpty())
			{
				return true;
			}
		}
		
		return false;
	}
		
	/**
	 * If on single player this will return the name of the user's
	 * world, if in multiplayer it will return the server name
	 * and game version.
	 */
	public static String getCurrentWorldID()
	{
		Minecraft mc = Minecraft.getInstance();
		
		if(mc.isIntegratedServerRunning())
		{
			ServerWorld serverWorld = LodUtils.getFirstValidServerWorld();
			if (serverWorld == null)
				return "";
			
			ServerChunkProvider provider = serverWorld.getChunkProvider();
			if(provider != null)
				return provider.getSavedData().folder.toString();
			
			return "";
		}
		else
		{
			return mc.getCurrentServerData().serverName + "_version_" + mc.getCurrentServerData().gameVersion;
		}
	}
	
	
}
