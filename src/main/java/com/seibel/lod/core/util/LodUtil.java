/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
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

package com.seibel.lod.core.util;

import java.awt.Color;
import java.io.File;
import java.util.HashSet;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.config.HorizontalResolution;
import com.seibel.lod.core.enums.config.VanillaOverdraw;
import com.seibel.lod.core.objects.VertexOptimizer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.opengl.DefaultLodVertexFormats;
import com.seibel.lod.core.objects.opengl.LodVertexFormat;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * This class holds methods and constants that may be used in multiple places.
 * 
 * @author James Seibel
 * @version 11-13-2021
 */
public class LodUtil
{
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	/**
	 * Vanilla render distances less than or equal to this will not allow partial
	 * overdraw. The VanillaOverdraw will either be ALWAYS or NEVER.
	 */
	public static final int MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW = 4;
	
	/**
	 * Vanilla render distances less than or equal to this will cause the overdraw to
	 * run at a smaller fraction of the vanilla render distance.
	 */
	public static final int MINIMUM_RENDER_DISTANCE_FOR_FAR_OVERDRAW = 11;
	
	
	
	
	/** The maximum number of LODs that can be rendered vertically */
	public static final int MAX_NUMBER_OF_VERTICAL_LODS = 32;
	
	/**
	 * alpha used when drawing chunks in debug mode
	 */
	public static final int DEBUG_ALPHA = 255; // 0 - 255
	public static final Color COLOR_DEBUG_BLACK = new Color(0, 0, 0, DEBUG_ALPHA);
	public static final Color COLOR_DEBUG_WHITE = new Color(255, 255, 255, DEBUG_ALPHA);
	public static final Color COLOR_INVISIBLE = new Color(0, 0, 0, 0);
	
	public static final int CEILED_DIMENSION_MAX_RENDER_DISTANCE = 64; // 0 - 255
	
	/**
	 * In order of nearest to farthest: <br>
	 * Red, Orange, Yellow, Green, Cyan, Blue, Magenta, white, gray, black
	 */
	public static final Color[] DEBUG_DETAIL_LEVEL_COLORS = new Color[] { Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.WHITE, Color.GRAY, Color.BLACK };
	
	
	public static final byte DETAIL_OPTIONS = 10;
	
	/** 512 blocks wide */
	public static final byte REGION_DETAIL_LEVEL = DETAIL_OPTIONS - 1;
	/** 16 blocks wide */
	public static final byte CHUNK_DETAIL_LEVEL = 4;
	/** 1 block wide */
	public static final byte BLOCK_DETAIL_LEVEL = 0;
	
	public static final short MAX_VERTICAL_DATA = 4;
	
	/**
	 * measured in Blocks <br>
	 * detail level max - 1
	 */
	public static final short REGION_WIDTH = 1 << REGION_DETAIL_LEVEL;
	/**
	 * measured in Blocks <br>
	 * detail level 4
	 */
	public static final short CHUNK_WIDTH = 16;
	/**
	 * measured in Blocks <br>
	 * detail level 0
	 */
	public static final short BLOCK_WIDTH = 1;
	
	
	/** number of chunks wide */
	public static final int REGION_WIDTH_IN_CHUNKS = REGION_WIDTH / CHUNK_WIDTH;
	
	
	/**
	 * This regex finds any characters that are invalid for use in a windows
	 * (and by extension mac and linux) file path
	 */
	public static final String INVALID_FILE_CHARACTERS_REGEX = "[\\\\/:*?\"<>|]";
	
	/**
	 * 64 MB by default is the maximum amount of memory that
	 * can be directly allocated. <br><br>
	 * <p>
	 * James knows there are commands to change that amount
	 * (specifically "-XX:MaxDirectMemorySize"), but
	 * He has no idea how to access that amount. <br>
	 * So for now this will be the hard limit. <br><br>
	 * <p>
	 * https://stackoverflow.com/questions/50499238/bytebuffer-allocatedirect-and-xmx
	 */
	public static final int MAX_ALLOCATABLE_DIRECT_MEMORY = 64 * 1024 * 1024;
	
	/** the format of data stored in the GPU buffers */
	public static final LodVertexFormat LOD_VERTEX_FORMAT = DefaultLodVertexFormats.POSITION_COLOR;
	
	
	
	
	
	/**
	 * Gets the ServerWorld for the relevant dimension.
	 * @return null if there is no ServerWorld for the given dimension
	 */
	public static IWorldWrapper getServerWorldFromDimension(IDimensionTypeWrapper newDimension)
	{
		if(!MC.hasSinglePlayerServer())
			return null;
		
		Iterable<IWorldWrapper> worlds = MC.getAllServerWorlds();
		IWorldWrapper returnWorld = null;
		
		for (IWorldWrapper world : worlds)
		{
			if (world.getDimensionType() == newDimension)
			{
				returnWorld = world;
				break;
			}
		}
		
		return returnWorld;
	}
	
	/** Convert a 2D absolute position into a quad tree relative position. */
	public static RegionPos convertGenericPosToRegionPos(int x, int z, int detailLevel)
	{
		int relativePosX = Math.floorDiv(x, 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel));
		int relativePosZ = Math.floorDiv(z, 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel));
		
		return new RegionPos(relativePosX, relativePosZ);
	}
	
	/** Convert a 2D absolute position into a quad tree relative position. */
	public static int convertLevelPos(int pos, int currentDetailLevel, int targetDetailLevel)
	{
		return pos / (1 << (targetDetailLevel - currentDetailLevel));
	}
	
	
	/**
	 * If on single player this will return the name of the user's
	 * world, if in multiplayer it will return the server name, IP,
	 * and game version.
	 */
	public static String getWorldID(IWorldWrapper world)
	{
		if (MC.hasSinglePlayerServer())
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
			return getServerId();
		}
	}
	
	
	/**
	 * If on single player this will return the name of the user's
	 * world and the dimensional save folder, if in multiplayer
	 * it will return the server name, ip, game version, and dimension.<br>
	 * <br>
	 * This can be used to determine where to save files for a given
	 * dimension.
	 */
	public static String getDimensionIDFromWorld(IWorldWrapper world)
	{
		if (MC.hasSinglePlayerServer())
		{
			// this will return the world save location
			// and the dimension folder
			
			IWorldWrapper serverWorld = LodUtil.getServerWorldFromDimension(world.getDimensionType());
			if (serverWorld == null)
				throw new NullPointerException("getDimensionIDFromWorld wasn't able to get the WorldWrapper for the dimension " + world.getDimensionType().getDimensionName());
			
			return serverWorld.getSaveFolder().toString();
		}
		else
		{
			return getServerId() + File.separatorChar + "dim_" + world.getDimensionType().getDimensionName() + File.separatorChar;
		}
	}
	
	/** returns the server name, IP and game version. */
	public static String getServerId()
	{
		String serverName = MC.getCurrentServerName().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
		String serverIp = MC.getCurrentServerIp().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
		String serverMcVersion = MC.getCurrentServerVersion().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
		
		return serverName + ", IP " + serverIp + ", GameVersion " + serverMcVersion;
	}
	
	
	/** Convert a BlockColors int into a Color object */
	public static Color intToColor(int num)
	{
		int filter = 0b11111111;
		
		int red = (num >> 16) & filter;
		int green = (num >> 8) & filter;
		int blue = num & filter;
		
		return new Color(red, green, blue);
	}
	
	/** Convert a Color into a BlockColors object. */
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
	public static HashSet<AbstractChunkPosWrapper> getNearbyLodChunkPosToSkip(LodDimension lodDim, AbstractBlockPosWrapper blockPosWrapper)
	{
		int chunkRenderDist = MC_RENDER.getRenderDistance();
		AbstractChunkPosWrapper centerChunk = FACTORY.createChunkPos(blockPosWrapper);
		
		int skipRadius;
		VanillaOverdraw overdraw = CONFIG.client().graphics().advancedGraphics().getVanillaOverdraw();
		HorizontalResolution drawRes = CONFIG.client().graphics().quality().getDrawResolution();
		
		// apply distance based rules for dynamic overdraw
		if (overdraw == VanillaOverdraw.DYNAMIC
				&& chunkRenderDist <= MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW)
		{
			// The vanilla render distance isn't far enough 
			// for partial skipping to make sense...
			if (!lodDim.dimension.hasCeiling() && (drawRes == HorizontalResolution.BLOCK))
			{
				// ...and the dimension is open, so we don't have to worry about
				// LODs rendering on top of the player,
				// and the user is using a high horizontal resolution,
				// so the overdraw shouldn't be noticeable
				overdraw = VanillaOverdraw.ALWAYS;
			}
			else
			{
				// ...but we are underground, so we don't want
				// LODs rendering on top of the player,
				// Or the user is using a LOW horizontal resolution
				// and overdraw would be very noticeable.
				overdraw = VanillaOverdraw.NEVER;
			}
		}
		
		
		// determine the skipping type based
		// on the overdraw type
		switch (overdraw)
		{
		case ALWAYS:
			// don't skip any positions
			return new HashSet<>();
		
		case DYNAMIC:
			
			if (chunkRenderDist > MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW 
				&& chunkRenderDist <= MINIMUM_RENDER_DISTANCE_FOR_FAR_OVERDRAW)
			{
				// This is a small render distance (but greater than the minimum partial
				// distance), skip positions that are greater than 2/3 the render distance
				skipRadius = (int) Math.ceil(chunkRenderDist * (2.0/3.0));
			}
			else
			{
				// This is a large render distance. Skip positions that are greater than
				// 4/5ths the render distance
				skipRadius = (int) Math.ceil(chunkRenderDist * (4.0 / 5.0));
			}
			break;
		
		default:
		case BORDER:
		case NEVER:
			// skip chunks in render distance that are rendered
			// by vanilla minecraft
			skipRadius = 0;
			break;
		}
		
		
		// get the chunks that are going to be rendered by Minecraft
		HashSet<AbstractChunkPosWrapper> posToSkip = MC_RENDER.getRenderedChunks();
		
		
		// remove everything outside the skipRadius,
		// if the skipRadius is being used
		if (skipRadius != 0)
		{
			for (int x = centerChunk.getX() - chunkRenderDist; x < centerChunk.getX() + chunkRenderDist; x++)
			{
				for (int z = centerChunk.getZ() - chunkRenderDist; z < centerChunk.getZ() + chunkRenderDist; z++)
				{
					if (x <= centerChunk.getX() - skipRadius || x >= centerChunk.getX() + skipRadius
							|| z <= centerChunk.getZ() - skipRadius || z >= centerChunk.getZ() + skipRadius)
						posToSkip.remove(FACTORY.createChunkPos(x, z));
				}
			}
		}
		return posToSkip;
	}
	
	
	/**
	 * This method find if a given chunk is a border chunk of the renderable ones
	 * @param vanillaRenderedChunks matrix of the vanilla rendered chunks
	 * @param x relative (to the matrix) x chunk to check
	 * @param z relative (to the matrix) z chunk to check
	 * @return true if and only if the chunk is a border of the renderable chunks
	 */
	public static boolean isBorderChunk(boolean[][] vanillaRenderedChunks, int x, int z)
	{
		if (x < 0 || z < 0 || x >= vanillaRenderedChunks.length || z >= vanillaRenderedChunks[0].length)
			return false;
		int tempX;
		int tempZ;
		for (LodDirection lodDirection : VertexOptimizer.ADJ_DIRECTIONS)
		{
			tempX = x + VertexOptimizer.DIRECTION_NORMAL_MAP.get(lodDirection).x;
			tempZ = z + VertexOptimizer.DIRECTION_NORMAL_MAP.get(lodDirection).z;
			if (vanillaRenderedChunks[x][z] || (!(tempX < 0 || tempZ < 0 || tempX >= vanillaRenderedChunks.length || tempZ >= vanillaRenderedChunks[0].length)
				&& !vanillaRenderedChunks[tempX][tempZ]))
				return true;
		}
		return false;
	}
	
	
	/** This is copied from Minecraft's MathHelper class */
	public static float fastInvSqrt(float numb)
	{
		float half = 0.5F * numb;
		int i = Float.floatToIntBits(numb);
		i = 1597463007 - (i >> 1);
		numb = Float.intBitsToFloat(i);
		return numb * (1.5F - half * numb * numb);
	}
}
