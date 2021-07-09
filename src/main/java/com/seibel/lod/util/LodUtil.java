package com.seibel.lod.util;

import java.awt.Color;
import java.io.File;

import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.RegionPos;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

/**
 * This class holds methods that may be used in multiple places.
 * 
 * @author James Seibel
 * @version 06-27-2021
 */
public class LodUtil
{
	private static Minecraft mc = Minecraft.getInstance();
	
	
	
	/**
	 * Gets the first valid ServerWorld.
	 * 
	 * @return null if there are no ServerWorlds
	 */
	public static ServerWorld getFirstValidServerWorld()
	{
		if (mc.hasSingleplayerServer())
			return null;
		
		Iterable<ServerWorld> worlds = mc.getSingleplayerServer().getAllLevels();
		
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
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null)
			return null;
		
		Iterable<ServerWorld> worlds = server.getAllLevels();
		ServerWorld returnWorld = null;
		
		for (ServerWorld world : worlds)
		{
			if(world.dimensionType() == dimension)
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
		rPos.x = pos.x / 512;
		rPos.z = pos.z / 512;
		
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
	
	
	
	public static String getCurrentDimensionID()
	{

		Minecraft mc = Minecraft.getInstance();
		
		if(mc.hasSingleplayerServer())
		{
			// this will return the world save location
			// and the dimension folder
			
			if(mc.level == null)
				return "";
			
			ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(mc.level.dimensionType());
			if(serverWorld == null)
				return "";
			
			ServerChunkProvider provider = serverWorld.getChunkSource();
			if(provider == null)
				return "";
			
			return provider.dataStorage.dataFolder.toString();
		}
		else
		{
			ServerData server = mc.getCurrentServer();
			return server.name + ", IP " + 
					server.ip + ", GameVersion " + 
					server.version.getString() + File.separatorChar
					+ "dim_" + mc.level.dimensionType().effectsLocation().getPath() + File.separatorChar;
		}
	}

	
	/**
	 * If on single player this will return the name of the user's
	 * world and the dimensional save folder, if in multiplayer 
	 * it will return the server name, game version, and dimension.<br>
	 * <br>
	 * This can be used to determine where to save files for a given
	 * dimension.
	 */
	public static String getDimensionIDFromWorld(IWorld world)
	{
		Minecraft mc = Minecraft.getInstance();
		
		if(mc.hasSingleplayerServer())
		{
			// this will return the world save location
			// and the dimension folder
			
			ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(world.dimensionType());
			if(serverWorld == null)
				throw new NullPointerException("getDimensionIDFromWorld wasn't able to get the ServerWorld for the dimension " + world.dimensionType().effectsLocation().getPath());
			
			ServerChunkProvider provider = serverWorld.getChunkSource();
			if(provider == null)
				throw new NullPointerException("getDimensionIDFromWorld wasn't able to get the ServerChunkProvider for the dimension " + world.dimensionType().effectsLocation().getPath());
			
			return provider.dataStorage.dataFolder.toString();
		}
		else
		{
			ServerData server = mc.getCurrentServer();
			return server.name + ", IP " + 
					server.ip + ", GameVersion " + 
					server.version.getString() + File.separatorChar
					+ "dim_" + world.dimensionType().effectsLocation().getPath() + File.separatorChar;
		}
	}
	
	/**
	 * If on single player this will return the name of the user's
	 * world, if in multiplayer it will return the server name
	 * and game version.
	 */
	public static String getWorldID(IWorld world)
	{
		if(mc.hasSingleplayerServer())
		{
			// chop off the dimension ID as it is not needed/wanted
			String dimId = getDimensionIDFromWorld(world);
			
			// get the world name
			int saveIndex = dimId.indexOf("saves") + 1 + "saves".length();
			int slashIndex = dimId.indexOf(File.separatorChar, saveIndex);
			dimId = dimId.substring(saveIndex, slashIndex);
			return dimId;
		}
		else
		{
			ServerData server = mc.getCurrentServer();
			return server.name + ", IP " + 
					server.ip + ", GameVersion " + 
					server.version.getString();
		}
	}
	
	
	
	/**
	 * Convert a BlockColors int into a Color object.
	 */
	public static Color intToColor(int num)
	{
		int filter = 0b11111111;
		
		int red = (num >> 16 ) & filter;
		int green = (num >> 8 ) & filter;
		int blue = num & filter;
		
		return new Color(red, green, blue);
	}
	
	/**
	 * Convert a Color into a BlockColors object.
	 */
	public static int colorToInt(Color color)
	{
		return color.getRGB();
	}
}
