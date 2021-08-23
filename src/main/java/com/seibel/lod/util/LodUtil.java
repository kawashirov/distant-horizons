/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.seibel.lod.util;

import java.awt.Color;
import java.io.File;
import java.util.HashSet;

import com.seibel.lod.objects.LevelPos;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.RegionPos;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

/**
 * This class holds methods and constants that may be used in multiple places.
 * 
 * @author James Seibel
 * @version 8-11-2021
 */
public class LodUtil
{
	private static Minecraft mc = Minecraft.getInstance();
	
	
	/** alpha used when drawing chunks in debug mode */
	public static final int DEBUG_ALPHA = 255; // 0 - 255
	public static final Color COLOR_DEBUG_BLACK = new Color(0, 0, 0, DEBUG_ALPHA);
	public static final Color COLOR_DEBUG_WHITE = new Color(255, 255, 255, DEBUG_ALPHA);
	public static final Color COLOR_INVISIBLE = new Color(0,0,0,0);
	
	/** a gray-purple color */
	public static final int MYCELIUM_COLOR_INT = LodUtil.colorToInt(Color.decode("#6E6166"));
	
	/**
	 * In order of nearest to farthest: <br>
	 * Red, Orange, Yellow, Green, Cyan, Blue, Magenta, white, gray, black
	 */
	public static final Color DEBUG_DETAIL_LEVEL_COLORS[] = new Color[] { Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.WHITE, Color.GRAY, Color.BLACK };
	
	
	/** 512 blocks wide */
	public static final byte REGION_DETAIL_LEVEL = 9;
	/** 16 blocks wide */
	public static final byte CHUNK_DETAIL_LEVEL = 4;
	/** 1 block wide */
	public static final byte BLOCK_DETAIL_LEVEL = 0;


	public static final byte DETAIL_OPTIONS = 10;
	
	/** measured in Blocks <br>
	 * detail level 9 */
	public static final short REGION_WIDTH = 512;
	/** measured in Blocks <br>
	 * detail level 4 */
	public static final short CHUNK_WIDTH = 16;
	/** measured in Blocks <br> 
	 * detail level 0 */
	public static final short BLOCK_WIDTH = 1;
	
	
	/** number of chunks wide */
	public static final int REGION_WIDTH_IN_CHUNKS = 32;
	
	
	/** If we ever need to use a heightmap for any reason, use this one. */
	public static final Heightmap.Type DEFAULT_HEIGHTMAP = Heightmap.Type.WORLD_SURFACE_WG;
	
	
	
	
	
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
	 * Convert a 2D absolute position into a quad tree relative position. 
	 */
	public static RegionPos convertGenericPosToRegionPos(int x, int z, int detailLevel)
	{
		int relativePosX = Math.floorDiv(x, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel));
		int relativePosZ = Math.floorDiv(z, (int) Math.pow(2, LodUtil.REGION_DETAIL_LEVEL - detailLevel));
		
		return new RegionPos(relativePosX, relativePosZ);
	}
	
	/**
	 * Convert a 2D absolute position into a quad tree relative position.
	 */
	public static int convertLevelPos(int pos, int currectDetailLevel, int targetDetailLevel)
	{
		int newPos = Math.floorDiv(pos, (int) Math.pow(2, targetDetailLevel - currectDetailLevel));
		
		return newPos;
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
	 * world, if in multiplayer it will return the server name, IP,
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
	
	
	/**
	 * Clamps the given value between the min and max values.
	 * May behave strangely if min > max.
	 */
	public static int clamp(int min, int value, int max)
	{
		return Math.min(max, Math.max(value, min));
	}
	
	/**
	 * Clamps the given value between the min and max values.
	 * May behave strangely if min > max.
	 */
	public static float clamp(float min, float value, float max)
	{
		return Math.min(max, Math.max(value, min));
	}
	
	/**
	 * Clamps the given value between the min and max values.
	 * May behave strangely if min > max.
	 */
	public static double clamp(double min, double value, double max)
	{
		return Math.min(max, Math.max(value, min));
	}
	
	
	
	 /**
     * Get a HashSet of all ChunkPos within the normal render distance
     * that should not be rendered.
     */
		public static HashSet<ChunkPos> getNearbyLodChunkPosToSkip(LodDimension lodDim, BlockPos playerPos)
		{
			int chunkRenderDist = mc.options.renderDistance;
			ChunkPos centerChunk = new ChunkPos(playerPos);
			
			// skip chunks that are already going to be rendered by Minecraft
			HashSet<ChunkPos> posToSkip = getRenderedChunks();
			
			// go through each chunk within the normal view distance
			for (int x = centerChunk.x - chunkRenderDist; x < centerChunk.x + chunkRenderDist; x++)
			{
				for (int z = centerChunk.z - chunkRenderDist; z < centerChunk.z + chunkRenderDist; z++)
				{
					LevelPos levelPos = new LevelPos(LodUtil.CHUNK_DETAIL_LEVEL, x, z);
					if (!lodDim.doesDataExist(levelPos))
						continue;
					
					LodDataPoint data = lodDim.getData(levelPos);
					if (data == null)
						continue;
					
					short lodAverageHeight = data.height;
					if (playerPos.getY() <= lodAverageHeight)
					{
						// don't draw Lod's that are taller than the player
						// to prevent LODs being drawn on top of the player
						posToSkip.add(new ChunkPos(x, z));
					}
				}
			}
			
			return posToSkip;
		}

    /**
     * This method returns the ChunkPos of all chunks that Minecraft
     * is going to render this frame. <br><br>
     * <p>
     * Note: This isn't perfect. It will return some chunks that are outside
     * the clipping plane. (For example, if you are high above the ground some chunks
     * will be incorrectly added, even though they are outside render range).
     */
	public static HashSet<ChunkPos> getRenderedChunks()
	{
		HashSet<ChunkPos> loadedPos = new HashSet<>();
		
		Minecraft mc = Minecraft.getInstance();
		
		// Wow those are some long names!
		
		// go through every RenderInfo to get the compiled chunks
		for (WorldRenderer.LocalRenderInformationContainer worldrenderer$localrenderinformationcontainer : mc.levelRenderer.renderChunks)
		{
			if (!worldrenderer$localrenderinformationcontainer.chunk.getCompiledChunk().hasNoRenderableLayers())
			{
				// add the ChunkPos for every empty compiled chunk
				BlockPos bpos = worldrenderer$localrenderinformationcontainer.chunk.getOrigin();
				
				loadedPos.add(new ChunkPos(bpos));
			}
		}
		
		return loadedPos;
	}
	
	
}
