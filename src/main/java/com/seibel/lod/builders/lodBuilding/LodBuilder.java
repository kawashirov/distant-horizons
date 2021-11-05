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

package com.seibel.lod.builders.lodBuilding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.HorizontalResolution;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.util.ThreadMapUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;
import com.seibel.lod.wrappers.Block.BlockColorWrapper;
import com.seibel.lod.wrappers.Block.BlockPosWrapper;
import com.seibel.lod.wrappers.Block.BlockShapeWrapper;
import com.seibel.lod.wrappers.Chunk.ChunkPosWrapper;
import com.seibel.lod.wrappers.Chunk.ChunkWrapper;
import com.seibel.lod.wrappers.World.BiomeWrapper;
import com.seibel.lod.wrappers.World.LevelWrapper;

import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;

/**
 * This object is in charge of creating Lod related objects.
 *
 * @author Cola
 * @author Leonardo Amato
 * @author James Seibel
 * @version 10-22-2021
 */
public class LodBuilder
{
	private static final MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	private final ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));
	
	
	/** If no blocks are found in the area in determineBottomPointForArea return this */
	public static final short DEFAULT_DEPTH = 0;
	/** If no blocks are found in the area in determineHeightPointForArea return this */
	public static final short DEFAULT_HEIGHT = 0;
	/** Minecraft's max light value */
	public static final short DEFAULT_MAX_LIGHT = 15;
	
	
	/**
	 * How wide LodDimensions should be in regions <br>
	 * Is automatically set before the first frame in ClientProxy.
	 */
	public int defaultDimensionWidthInRegions = 0;
	
	//public static final boolean useExperimentalLighting = true;
	
	
	
	
	public LodBuilder()
	{
	
	}
	
	public void generateLodNodeAsync(ChunkWrapper chunk, LodWorld lodWorld, IWorld world)
	{
		generateLodNodeAsync(chunk, lodWorld, world, DistanceGenerationMode.SERVER);
	}
	
	public void generateLodNodeAsync(ChunkWrapper chunk, LodWorld lodWorld, IWorld world, DistanceGenerationMode generationMode)
	{
		if (lodWorld == null || lodWorld.getIsWorldNotLoaded())
			return;
		
		// don't try to create an LOD object
		// if for some reason we aren't
		// given a valid chunk object
		if (chunk == null)
			return;
		
		Thread thread = new Thread(() ->
		{
			try
			{
				// we need a loaded client world in order to
				// get the textures for blocks
				if (mc.getClientLevel() == null)
					return;
				
				// don't try to generate LODs if the user isn't in the world anymore
				// (this happens a lot when the user leaves a world/server)
				if (mc.getSinglePlayerServer() == null && mc.getCurrentServer() == null)
					return;
				
				DimensionType dim = world.dimensionType();
				
				// make sure the dimension exists
				LodDimension lodDim;
				if (lodWorld.getLodDimension(dim) == null)
				{
					lodDim = new LodDimension(dim, lodWorld, defaultDimensionWidthInRegions);
					lodWorld.addLodDimension(lodDim);
				}
				else
				{
					lodDim = lodWorld.getLodDimension(dim);
				}
				generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig(generationMode));
			}
			catch (IllegalArgumentException | NullPointerException e)
			{
				e.printStackTrace();
				// if the world changes while LODs are being generated
				// they will throw errors as they try to access things that no longer
				// exist.
			}
		});
		lodGenThreadPool.execute(thread);
	}
	
	/**
	 * Creates a LodNode for a chunk in the given world.
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 */
	public void generateLodNodeFromChunk(LodDimension lodDim, ChunkWrapper chunk) throws IllegalArgumentException
	{
		generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig());
	}
	
	/**
	 * Creates a LodNode for a chunk in the given world.
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 */
	public void generateLodNodeFromChunk(LodDimension lodDim, ChunkWrapper chunk, LodBuilderConfig config)
			throws IllegalArgumentException
	{
		if (chunk == null)
			throw new IllegalArgumentException("generateLodFromChunk given a null chunk");
		
		int startX;
		int startZ;
		
		
		LodRegion region = lodDim.getRegion(chunk.getPos().getRegionX(), chunk.getPos().getRegionZ());
		if (region == null)
			return;
		
		// this happens if a LOD is generated after the user leaves the world.
		if (MinecraftWrapper.INSTANCE.getWrappedClientLevel() == null)
			return;
		
		// determine how many LODs to generate horizontally
		byte minDetailLevel = region.getMinDetailLevel();
		HorizontalResolution detail = DetailDistanceUtil.getLodGenDetail(minDetailLevel);
		
		
		// determine how many LODs to generate vertically
		//VerticalQuality verticalQuality = LodConfig.CLIENT.graphics.qualityOption.verticalQuality.get();
		byte detailLevel = detail.detailLevel;
		
		
		// generate the LODs
		int posX;
		int posZ;
		for (int i = 0; i < detail.dataPointLengthCount * detail.dataPointLengthCount; i++)
		{
			startX = detail.startX[i];
			startZ = detail.startZ[i];
			
			long[] data;
			long[] dataToMergeVertical = createVerticalDataToMerge(detail, chunk, config, startX, startZ);
			data = DataPointUtil.mergeMultiData(dataToMergeVertical, DataPointUtil.worldHeight / 2 + 1, DetailDistanceUtil.getMaxVerticalData(detailLevel));
			
			
			//lodDim.clear(detailLevel, posX, posZ);
			if (data != null && data.length != 0)
			{
				posX = LevelPosUtil.convert((byte) 0, chunk.getPos().getX() * 16 + startX, detail.detailLevel);
				posZ = LevelPosUtil.convert((byte) 0, chunk.getPos().getZ() * 16 + startZ, detail.detailLevel);
				lodDim.addVerticalData(detailLevel, posX, posZ, data, false);
			}
		}
		lodDim.updateData(LodUtil.CHUNK_DETAIL_LEVEL, chunk.getPos().getX(), chunk.getPos().getZ());
	}
	
	/** creates a vertical DataPoint */
	private long[] createVerticalDataToMerge(HorizontalResolution detail, ChunkWrapper chunk, LodBuilderConfig config, int startX, int startZ)
	{
		// equivalent to 2^detailLevel
		int size = 1 << detail.detailLevel;
		
		long[] dataToMerge = ThreadMapUtil.getBuilderVerticalArray(detail.detailLevel);
		int verticalData = DataPointUtil.worldHeight / 2 + 1;
		
		ChunkPosWrapper chunkPos = chunk.getPos();
		int height;
		int depth;
		int color;
		int light;
		int lightSky;
		int lightBlock;
		int generation = config.distanceGenerationMode.complexity;
		
		int xRel;
		int zRel;
		int xAbs;
		int yAbs;
		int zAbs;
		boolean hasCeiling = mc.getClientLevel().dimensionType().hasCeiling();
		boolean hasSkyLight = mc.getClientLevel().dimensionType().hasSkyLight();
		boolean isDefault;
		BlockPosWrapper blockPos = new BlockPosWrapper();
		int index;
		
		for (index = 0; index < size * size; index++)
		{
			xRel = startX + index % size;
			zRel = startZ + index / size;
			xAbs = chunkPos.getMinBlockX() + xRel;
			zAbs = chunkPos.getMinBlockZ() + zRel;
			
			//Calculate the height of the lod
			yAbs = DataPointUtil.worldHeight + 1;
			int count = 0;
			boolean topBlock = true;
			while (yAbs > 0)
			{
				height = determineHeightPointFrom(chunk, config, xRel, yAbs, zRel, blockPos);
				
				// If the lod is at the default height, it must be void data
				if (height == DEFAULT_HEIGHT)
				{
					if (topBlock)
						dataToMerge[index * verticalData] = DataPointUtil.createVoidDataPoint(generation);
					break;
				}
				
				yAbs = height - 1;
				// We search light on above air block
				depth = determineBottomPointFrom(chunk, config, xRel, yAbs, zRel, blockPos);
				if (hasCeiling && topBlock)
				{
					yAbs = depth;
					blockPos.set(xAbs, yAbs, zAbs);
					light = getLightValue(chunk, blockPos, true, hasSkyLight, true);
					color = generateLodColor(chunk, config, xAbs, yAbs, zAbs, blockPos);
					blockPos.set(xAbs, yAbs - 1, zAbs);
				}
				else
				{
					blockPos.set(xAbs, yAbs, zAbs);
					light = getLightValue(chunk, blockPos, hasCeiling, hasSkyLight, topBlock);
					color = generateLodColor(chunk, config, xRel, yAbs, zRel, blockPos);
					blockPos.set(xAbs, yAbs + 1, zAbs);
				}
				lightBlock = light & 0b1111;
				lightSky = (light >> 4) & 0b1111;
				isDefault = ((light >> 8)) == 1;
				
				dataToMerge[index * verticalData + count] = DataPointUtil.createDataPoint(height, depth, color, lightSky, lightBlock, generation, isDefault);
				topBlock = false;
				yAbs = depth - 1;
				count++;
			}
		}
		return dataToMerge;
	}
	
	/**
	 * Find the lowest valid point from the bottom.
	 * Used when creating a vertical LOD.
	 */
	private short determineBottomPointFrom(ChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs, BlockPosWrapper blockPos)
	{
		short depth = DEFAULT_DEPTH;
		
		for (int y = yAbs; y >= 0; y--)
		{
			blockPos.set(xAbs, y, zAbs);
			if (!isLayerValidLodPoint(chunk, blockPos))
			{
				depth = (short) (y + 1);
				break;
			}
		}
		return depth;
	}
	
	/** Find the highest valid point from the Top */
	private short determineHeightPointFrom(ChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs, BlockPosWrapper blockPos)
	{
		short height = DEFAULT_HEIGHT;
		if (config.useHeightmap)
			height = (short) chunk.getHeightMapValue(xAbs, zAbs);
		else
		{
			for (int y = yAbs; y >= 0; y--)
			{
				blockPos.set(xAbs, y, zAbs);
				if (isLayerValidLodPoint(chunk, blockPos))
				{
					height = (short) (y + 1);
					break;
				}
			}
		}
		return height;
	}
	
	
	
	// =====================//
	// constructor helpers //
	// =====================//
	
	/**
	 * Generate the color for the given chunk using biome water color, foliage
	 * color, and grass color.
	 */
	private int generateLodColor(ChunkWrapper chunk, LodBuilderConfig config, int xRel, int yAbs, int zRel, BlockPosWrapper blockPos)
	{
		int colorInt;
		if (config.useBiomeColors)
		{
			// I have no idea why I need to bit shift to the right, but
			// if I don't the biomes don't show up correctly.
			colorInt = chunk.getBiome(xRel, yAbs, zRel).getColorForBiome(xRel, zRel);
		}
		else
		{
			blockPos.set(chunk.getPos().getMinBlockX() + xRel, yAbs, chunk.getPos().getMinBlockZ() + zRel);
			colorInt = getColorForBlock(chunk, blockPos);
			
			// if we are skipping non-full and non-solid blocks that means we ignore
			// snow, flowers, etc. Get the above block so we can still get the color
			// of the snow, flower, etc. that may be above this block
			int aboveColorInt = 0;
			if (LodConfig.CLIENT.worldGenerator.blockToAvoid.get().nonFull || LodConfig.CLIENT.worldGenerator.blockToAvoid.get().noCollision)
			{
				blockPos.set(chunk.getPos().getMinBlockX() + xRel, yAbs + 1, chunk.getPos().getMinBlockZ() + zRel);
				aboveColorInt = getColorForBlock(chunk, blockPos);
			}
			
			//if (colorInt == 0 && yAbs > 0)
			// if this block is invisible, check the block below it
			//	colorInt = generateLodColor(chunk, config, xRel, yAbs - 1, zRel, blockPos);
			
			// override this block's color if there was a block above this
			// and we were avoiding non-full/non-solid blocks
			if (aboveColorInt != 0)
				colorInt = aboveColorInt;
		}
		
		return colorInt;
	}
	
	/** Gets the light value for the given block position */
	private int getLightValue(ChunkWrapper chunk, BlockPosWrapper blockPos, boolean hasCeiling, boolean hasSkyLight, boolean topBlock)
	{
		int skyLight = 0;
		int blockLight;
		// 1 means the lighting is a guess
		int isDefault = 0;
		
		LevelWrapper world = MinecraftWrapper.INSTANCE.getWrappedServerLevel();
		
		int blockBrightness = chunk.getEmittedBrightness(blockPos);
		// get the air block above or below this block
		if (hasCeiling && topBlock)
			blockPos.set(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ());
		else
			blockPos.set(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ());
		
		
		
		if (world != null && !world.isEmpty())
		{
			// server world sky light (always accurate)
			blockLight = world.getBlockLight(blockPos);
			if (topBlock && !hasCeiling && hasSkyLight)
				skyLight = DEFAULT_MAX_LIGHT;
			else
			{
				if (hasSkyLight)
					skyLight = world.getSkyLight(blockPos);
				//else
				//	skyLight = 0;
			}
			if (!topBlock && skyLight == 15)
			{
				// we are on predicted terrain, and we don't know what the light here is,
				// lets just take a guess
				if (blockPos.getY() >= mc.getClientLevel().getSeaLevel() - 5)
				{
					skyLight = 12;
					isDefault = 1;
				}
				else
					skyLight = 0;
			}
		}
		else
		{
			world = MinecraftWrapper.INSTANCE.getWrappedClientLevel();
			if (world.isEmpty())
				return 0;
			// client world sky light (almost never accurate)
			blockLight = world.getBlockLight(blockPos);
			// estimate what the lighting should be
			if (hasSkyLight || !hasCeiling)
			{
				if (topBlock)
					skyLight = DEFAULT_MAX_LIGHT;
				else
				{
					
					if (hasSkyLight)
						skyLight = world.getSkyLight(blockPos);
					//else
					//	skyLight = 0;
					
					if (!chunk.isLightCorrect() && (skyLight == 0 || skyLight == 15))
					{
						// we don't know what the light here is,
						// lets just take a guess
						if (blockPos.getY() >= mc.getClientLevel().getSeaLevel() - 5)
						{
							skyLight = 12;
							isDefault = 1;
						}
						else
							skyLight = 0;
					}
				}
				if (hasSkyLight)
					skyLight = 0;
			}
		}
		
		blockLight = LodUtil.clamp(0, Math.max(blockLight, blockBrightness), DEFAULT_MAX_LIGHT);
		
		return blockLight + (skyLight << 4) + (isDefault << 8);
	}
	
	/** Returns a color int for the given block. */
	private int getColorForBlock(ChunkWrapper chunk, BlockPosWrapper blockPos)
	{
		
		
		int colorOfBlock;
		int colorInt;
		
		int xRel = blockPos.getX() - chunk.getPos().getMinBlockX();
		int zRel = blockPos.getZ() - chunk.getPos().getMinBlockZ();
		//int x = blockPos.getX();
		int y = blockPos.getY();
		//int z = blockPos.getZ();
		
		BlockColorWrapper blockColorWrapper;
		BlockShapeWrapper blockShapeWrapper = chunk.getBlockShapeWrapper(blockPos);
		
		if (chunk.isWaterLogged(blockPos))
			blockColorWrapper = BlockColorWrapper.getWaterColor();
		else
			blockColorWrapper = chunk.getBlockColorWrapper(blockPos);
		
		if (blockShapeWrapper.isToAvoid())
			return 0;
		
		colorOfBlock = blockColorWrapper.getColor();
		
		
		if (blockColorWrapper.hasTint())
		{
			BiomeWrapper biome = chunk.getBiome(xRel, y, zRel);
			int tintValue;
			if (blockColorWrapper.hasGrassTint())
				// grass and green plants
				tintValue = biome.getGrassTint(0,0);
			else if (blockColorWrapper.hasFolliageTint())
				tintValue = biome.getFolliageTint();
			else
				//we can reintroduce this with the wrappers
				tintValue = biome.getWaterTint();
			
			colorInt = ColorUtil.multiplyRGBcolors(tintValue | 0xFF000000, colorOfBlock);
		}
		else
			colorInt = colorOfBlock;
		return colorInt;
	}
	
	
	/** Is the block at the given blockPos a valid LOD point? */
	private boolean isLayerValidLodPoint(ChunkWrapper chunk, BlockPosWrapper blockPos)
	{
		
		
		if (chunk.isWaterLogged(blockPos))
			return true;
		
		boolean nonFullAvoidance = LodConfig.CLIENT.worldGenerator.blockToAvoid.get().nonFull;
		boolean noCollisionAvoidance = LodConfig.CLIENT.worldGenerator.blockToAvoid.get().noCollision;
		
		BlockShapeWrapper block = chunk.getBlockShapeWrapper(blockPos);
		return !block.isToAvoid()
					   && !(nonFullAvoidance && block.isNonFull())
					   && !(noCollisionAvoidance && block.hasNoCollision());
		
	}
}
