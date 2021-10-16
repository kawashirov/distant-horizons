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

package com.seibel.lod.builders.lodBuilding;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.HorizontalResolution;
import com.seibel.lod.enums.VerticalQuality;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.proxy.ClientProxy;
import com.seibel.lod.util.*;
import com.seibel.lod.wrappers.MinecraftWrapper;
import net.minecraft.block.*;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.client.model.data.ModelDataMap;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This object is in charge of creating Lod related objects. (specifically: Lod
 * World, Dimension, and Region objects)
 * @author Leonardo Amato
 * @author James Seibel
 * @version 10-9-2021
 */
public class LodBuilder
{
	private static final MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	private final ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));
	
	public static final Direction[] directions = new Direction[] { Direction.UP, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.DOWN };
	public static final int CHUNK_DATA_WIDTH = LodUtil.CHUNK_WIDTH;
	public static final int CHUNK_SECTION_HEIGHT = CHUNK_DATA_WIDTH;
	public static final ConcurrentMap<Block, Integer> colorMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<Block, Integer> tintColor = new ConcurrentHashMap<>();
	public static final ConcurrentMap<Block, Boolean> toTint = new ConcurrentHashMap<>();
	public static final ConcurrentMap<Block, VoxelShape> shapeMap = new ConcurrentHashMap<>();
	
	public static final ModelDataMap dataMap = new ModelDataMap.Builder().build();
	
	/** If no blocks are found in the area in determineBottomPointForArea return this */
	public static final short DEFAULT_DEPTH = 0;
	/** If no blocks are found in the area in determineHeightPointForArea return this */
	public static final short DEFAULT_HEIGHT = 0;
	/** Minecraft's max light value */
	public static final short DEFAULT_MAX_LIGHT = 15;
	
	//TODO make it config
	public static final boolean USE_EXPERIMENTAL_SKYLIGHT = false;
	
	/**
	 * How wide LodDimensions should be in regions <br>
	 * Is automatically set before the first frame in ClientProxy.
	 */
	public int defaultDimensionWidthInRegions = 0;
	
	//public static final boolean useExperimentalLighting = true;
	
	
	
	
	public LodBuilder()
	{
	
	}
	
	public void generateLodNodeAsync(IChunk chunk, LodWorld lodWorld, IWorld world)
	{
		generateLodNodeAsync(chunk, lodWorld, world, DistanceGenerationMode.SERVER);
	}
	
	public void generateLodNodeAsync(IChunk chunk, LodWorld lodWorld, IWorld world, DistanceGenerationMode generationMode)
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
				if (mc.getClientWorld() == null)
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
	public void generateLodNodeFromChunk(LodDimension lodDim, IChunk chunk) throws IllegalArgumentException
	{
		generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig());
	}
	
	/**
	 * Creates a LodNode for a chunk in the given world.
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 */
	public void generateLodNodeFromChunk(LodDimension lodDim, IChunk chunk, LodBuilderConfig config)
			throws IllegalArgumentException
	{
		if (chunk == null)
			throw new IllegalArgumentException("generateLodFromChunk given a null chunk");
		
		int startX;
		int startZ;
		int endX;
		int endZ;
		
		
		LodRegion region = lodDim.getRegion(chunk.getPos().getRegionX(), chunk.getPos().getRegionZ());
		if (region == null)
			return;
		
		// determine how many LODs to generate horizontally
		HorizontalResolution detail;
		byte minDetailLevel = region.getMinDetailLevel();
		detail = DetailDistanceUtil.getLodGenDetail(minDetailLevel);
		
		
		// determine how many LODs to generate vertically
		VerticalQuality verticalQuality = LodConfig.CLIENT.worldGenerator.verticalQuality.get();
		byte detailLevel = detail.detailLevel;
		
		
		// generate the LODs
		int posX;
		int posZ;
		for (int i = 0; i < detail.dataPointLengthCount * detail.dataPointLengthCount; i++)
		{
			startX = detail.startX[i];
			startZ = detail.startZ[i];
			endX = detail.endX[i];
			endZ = detail.endZ[i];
			
			posX = LevelPosUtil.convert((byte) 0, chunk.getPos().x * 16 + startX, detail.detailLevel);
			posZ = LevelPosUtil.convert((byte) 0, chunk.getPos().z * 16 + startZ, detail.detailLevel);
			
			
			long[] data;
			long[] dataToMergeVertical = createVerticalDataToMerge(detail, chunk, config, startX, startZ, endX, endZ);
			data = DataPointUtil.mergeMultiData(dataToMergeVertical, DataPointUtil.worldHeight, DetailDistanceUtil.getMaxVerticalData(detailLevel));
			
			
			//lodDim.clear(detailLevel, posX, posZ);
			if (data != null && data.length != 0)
				lodDim.addVerticalData(detailLevel,	posX, posZ,	data,false);
		}
		lodDim.updateData(LodUtil.CHUNK_DETAIL_LEVEL, chunk.getPos().x, chunk.getPos().z);
	}
	
	/** creates a vertical DataPoint */
	private long[] createVerticalDataToMerge(HorizontalResolution detail, IChunk chunk, LodBuilderConfig config, int startX, int startZ, int endX, int endZ)
	{
		// equivalent to 2^detailLevel
		int size = 1 << detail.detailLevel;
		
		long[] dataToMerge = ThreadMapUtil.getBuilderVerticalArray(detail.detailLevel);
		
		int verticalData = DataPointUtil.worldHeight;
		
		ChunkPos chunkPos = chunk.getPos();
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
		boolean hasCeiling = mc.getClientWorld().dimensionType().hasCeiling();
		boolean hasSkyLight = mc.getClientWorld().dimensionType().hasSkyLight();
		boolean isDefault;
		BlockPos.Mutable blockPos = new BlockPos.Mutable(0, 0, 0);
		int index;
		
		for (index = 0; index < size * size; index++)
		{
			xRel = Math.floorMod(index, size) + startX;
			zRel = Math.floorDiv(index, size) + startZ;
			xAbs = chunkPos.getMinBlockX() + xRel;
			zAbs = chunkPos.getMinBlockZ() + zRel;
			
			//Calculate the height of the lod
			yAbs = DataPointUtil.worldHeight + 2;
			int count = 0;
			boolean topBlock = true;
			while (yAbs > 0)
			{
				height = determineHeightPointFrom(chunk, config, xRel, zRel, yAbs, blockPos);
				
				// If the lod is at the default height, it must be void data
				if (height == DEFAULT_HEIGHT)
				{
					if (topBlock)
						dataToMerge[index * verticalData] = DataPointUtil.createVoidDataPoint(generation);
					break;
				}
				
				yAbs = height - 1;
				// We search light on above air block
				depth = determineBottomPointFrom(chunk, config, xRel, zRel, yAbs, blockPos);
				if (hasCeiling && topBlock)
				{
					yAbs = depth;
					blockPos.set(xAbs, yAbs, zAbs);
					light = getLightValue(chunk, blockPos, hasCeiling, hasSkyLight, topBlock);
					color = generateLodColor(chunk, config, xRel, yAbs, zRel, blockPos);
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
	private short determineBottomPointFrom(IChunk chunk, LodBuilderConfig config, int xRel, int zRel, int yAbs, BlockPos.Mutable blockPos)
	{
		short depth = DEFAULT_DEPTH;
		/*if (config.useHeightmap)
		{
			// when using the generated heightmap there is no data about the lowest point
			depth = 0; //DEFAULT_DEPTH == 0
		}
		else
		{*/
		boolean voidData = true;
		ChunkSection[] chunkSections = chunk.getSections();
		for (int sectionIndex = chunkSections.length - 1; sectionIndex >= 0; sectionIndex--)
		{
			for (int yRel = CHUNK_DATA_WIDTH - 1; yRel >= 0; yRel--)
			{
				if (sectionIndex * CHUNK_DATA_WIDTH + yRel > yAbs)
					continue;
				
				blockPos.set(chunk.getPos().getMinBlockX() + xRel, sectionIndex * CHUNK_DATA_WIDTH + yRel, chunk.getPos().getMinBlockZ() + zRel);
				if (!isLayerValidLodPoint(chunk, blockPos))
				{
					depth = (short) (sectionIndex * CHUNK_DATA_WIDTH + yRel + 1);
					voidData = false;
					break;
				}
			}
			
			if (!voidData)
				break;
		}
		//}
		return depth;
	}
	
	/** Find the highest valid point from the Top */
	private short determineHeightPointFrom(IChunk chunk, LodBuilderConfig config, int xRel, int zRel, int yAbs, BlockPos.Mutable blockPos)
	{
		short height = DEFAULT_HEIGHT;
		if (config.useHeightmap)
			height = (short) chunk.getOrCreateHeightmapUnprimed(LodUtil.DEFAULT_HEIGHTMAP).getFirstAvailable(xRel, zRel);
		else
		{
			boolean voidData = true;
			ChunkSection[] chunkSections = chunk.getSections();
			for (int sectionIndex = chunkSections.length - 1; sectionIndex >= 0; sectionIndex--)
			{
				for (int yRel = CHUNK_DATA_WIDTH - 1; yRel >= 0; yRel--)
				{
					if (sectionIndex * CHUNK_DATA_WIDTH + yRel > yAbs)
						continue;
					blockPos.set(chunk.getPos().getMinBlockX() + xRel, sectionIndex * CHUNK_DATA_WIDTH + yRel, chunk.getPos().getMinBlockZ() + zRel);
					if (isLayerValidLodPoint(chunk, blockPos))
					{
						height = (short) (sectionIndex * CHUNK_DATA_WIDTH + yRel + 1);
						voidData = false;
						break;
					}
				}
				if (!voidData)
					break;
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
	private int generateLodColor(IChunk chunk, LodBuilderConfig config, int xRel, int yAbs, int zRel, BlockPos.Mutable blockPos)
	{
		ChunkSection[] chunkSections = chunk.getSections();
		int colorInt = 0;
		if (config.useBiomeColors)
		{
			// I have no idea why I need to bit shift to the right, but
			// if I don't the biomes don't show up correctly.
			Biome biome = chunk.getBiomes().getNoiseBiome(xRel >> 2, yAbs >> 2, zRel >> 2);
			colorInt = getColorForBiome(xRel, zRel, biome);
		}
		else
		{
			int sectionIndex = Math.floorDiv(yAbs, CHUNK_SECTION_HEIGHT);
			int yRel = Math.floorMod(yAbs, CHUNK_SECTION_HEIGHT);
			if (chunkSections[sectionIndex] != null)
			{
				blockPos.set(chunk.getPos().getMinBlockX() + xRel, sectionIndex * CHUNK_DATA_WIDTH + yRel, chunk.getPos().getMinBlockZ() + zRel);
				colorInt = getColorForBlock(chunk, blockPos);
			}
			
			if (colorInt == 0 && yAbs > 0)
				// if this block is invisible, check the block below it
				colorInt = generateLodColor(chunk, config, xRel, yAbs - 1, zRel, blockPos);
		}
		
		return colorInt;
	}
	
	/** Gets the light value for the given block position */
	private int getLightValue(IChunk chunk, BlockPos.Mutable blockPos, boolean hasCeiling, boolean hasSkyLight, boolean topBlock)
	{
		int skyLight;
		int blockLight;
		int isDefault = 0;
		
		if (mc.getClientWorld() == null)
			return 0;
		
		ClientWorld world = mc.getClientWorld();
		
		
		int blockBrightness = chunk.getLightEmission(blockPos);
		
		if (hasCeiling && topBlock)
			blockPos.set(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ());
		else
			blockPos.set(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ());
		
		
		if (!hasSkyLight && hasCeiling)
			skyLight = 0;
		else if (topBlock)
			skyLight = DEFAULT_MAX_LIGHT;
		else
		{
			if (USE_EXPERIMENTAL_SKYLIGHT)
			{
				skyLight = world.getBrightness(LightType.SKY, blockPos);
				if (!chunk.isLightCorrect() && (skyLight == 0 || skyLight == 15))
				{
					// we don't know what the light here is,
					// lets just take a guess
					if (blockPos.getY() >= mc.getClientWorld().getSeaLevel() - 5)
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
				if (blockPos.getY() >= mc.getClientWorld().getSeaLevel() - 5)
				{
					skyLight = 12;
					isDefault = 1;
				}
				else
					skyLight = 0;
			}
			//if (skyLight == 15)
			//	ClientProxy.LOGGER.warn("skylight 15 while not top block");
		}
		
		blockLight = world.getBrightness(LightType.BLOCK, blockPos);
		blockLight = LodUtil.clamp(0, blockLight + blockBrightness, DEFAULT_MAX_LIGHT);
		
		return blockLight + (skyLight << 4) + (isDefault << 8);
	}
	
	
	/**
	 * Generate the color of the given block from its texture
	 * and store it for later use.
	 */
	private int getColorTextureForBlock(BlockState blockState, BlockPos blockPos, boolean useTopTexture)
	{
		// use the pre-generated color if we can
		Block block = blockState.getBlock();
		if (colorMap.containsKey(block) && toTint.containsKey(block))
			return colorMap.get(block);
		
		
		
		World world = mc.getClientWorld();
		TextureAtlasSprite texture;
		List<BakedQuad> quads = null;
		int tintIndex = Integer.MIN_VALUE;
		boolean isTinted = false;
		int listSize = 0;
		// get the first quad we can for this block
		for (Direction direction : directions)
		{
			quads = mc.getModelManager().getBlockModelShaper().getBlockModel(blockState).getQuads(blockState, direction, new Random(0), dataMap);
			listSize = Math.max(listSize, quads.size());
			for (BakedQuad bakedQuad : quads)
			{
				isTinted |= bakedQuad.isTinted();
				tintIndex = Math.max(tintIndex, bakedQuad.getTintIndex());
			}
		}
		toTint.put(block, isTinted);
		tintColor.put(block, tintIndex);
		for (Direction direction : directions)
		{
			quads = mc.getModelManager().getBlockModelShaper().getBlockModel(blockState).getQuads(blockState, direction, new Random(0), dataMap);
			if (!quads.isEmpty())
				break;
		}
		
		if (useTopTexture && !quads.isEmpty())
			texture = quads.get(0).getSprite();
		else
			texture = mc.getModelManager().getBlockModelShaper().getTexture(blockState, world, blockPos);
		
		
		int count = 0;
		int alpha = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		int numberOfGreyPixel = 0;
		int color;
		int colorMultiplier;
		
		// generate the block's color
		for (int frameIndex = 0; frameIndex < texture.getFrameCount(); frameIndex++)
		{
			// textures normally use u and v instead of x and y
			for (int u = 0; u < texture.getHeight(); u++)
			{
				for (int v = 0; v < texture.getWidth(); v++)
				{
					if (texture.isTransparent(frameIndex, u, v))
						continue;
					
					color = texture.getPixelRGBA(frameIndex, u, v);
					
					// determine if this pixel is gray
					int colorMax = Math.max(Math.max(ColorUtil.getBlue(color), ColorUtil.getGreen(color)), ColorUtil.getRed(color));
					int colorMin = 4 + Math.min(Math.min(ColorUtil.getBlue(color), ColorUtil.getGreen(color)), ColorUtil.getRed(color));
					boolean isGray = colorMax < colorMin;
					if (isGray)
						numberOfGreyPixel++;
					
					
					// for flowers, weight their non-green color higher
					if (block instanceof FlowerBlock && (!(ColorUtil.getGreen(color) > (ColorUtil.getBlue(color) + 30)) || !(ColorUtil.getGreen(color) > (ColorUtil.getRed(color) + 30))))
						colorMultiplier = 5;
					else
						colorMultiplier = 1;
					
					
					// add to the running averages
					count += colorMultiplier;
					alpha += ColorUtil.getAlpha(color) * colorMultiplier;
					red += ColorUtil.getBlue(color) * colorMultiplier;
					green += ColorUtil.getGreen(color) * colorMultiplier;
					blue += ColorUtil.getRed(color) * colorMultiplier;
				}
			}
		}
		
		
		if (count == 0)
			// this block is entirely transparent
			color = 0;
		else
		{
			// determine the average color
			alpha /= count;
			red /= count;
			green /= count;
			blue /= count;
			color = ColorUtil.rgbToInt(alpha, red, green, blue);
		}
		
		// determine if this block should use the biome color tint
		if ((useGrassTint(block) || useLeafTint(block) || useWaterTint(block)) && (float) numberOfGreyPixel / count > 0.75f)
			toTint.replace(block, true);
		
		// add the newly generated block color to the map for later use
		colorMap.put(block, color);
		return color;
	}
	
	/** determine if the given block should use the biome's grass color */
	private boolean useGrassTint(Block block)
	{
		return block instanceof GrassBlock
				|| block instanceof BushBlock
				|| block instanceof IGrowable
				|| block instanceof AbstractPlantBlock
				|| block instanceof AbstractTopPlantBlock
				|| block instanceof TallGrassBlock;
	}
	
	/** determine if the given block should use the biome's foliage color */
	private boolean useLeafTint(Block block)
	{
		return block instanceof LeavesBlock
				|| block == Blocks.VINE
				|| block == Blocks.SUGAR_CANE;
	}
	
	/** determine if the given block should use the biome's water color */
	private boolean useWaterTint(Block block)
	{
		return block == Blocks.WATER;
	}
	
	/** Returns a color int for the given block. */
	private int getColorForBlock(IChunk chunk, BlockPos blockPos)
	{
		int blockColor;
		int colorInt;
		
		int xRel = blockPos.getX() - chunk.getPos().getMinBlockX();
		int zRel = blockPos.getZ() - chunk.getPos().getMinBlockZ();
		int x = blockPos.getX();
		int y = blockPos.getY();
		int z = blockPos.getZ();
		
		Biome biome = chunk.getBiomes().getNoiseBiome(xRel >> 2, y >> 2, zRel >> 2);
		BlockState blockState = chunk.getBlockState(blockPos);
		
		
		
		// block special cases
		// TODO: this needs to be replaced by a config file of some sort
		if (blockState == Blocks.AIR.defaultBlockState()
				|| blockState == Blocks.CAVE_AIR.defaultBlockState()
				|| blockState == Blocks.BARRIER.defaultBlockState())
		{
			return 0;
		}
		
		blockColor = getColorTextureForBlock(blockState, blockPos, true);
		
		//if the blockColor is 0 we reset it and don't use the faceColor
		if (blockColor == 0)
		{
			tintColor.remove(blockState.getBlock());
			toTint.remove(blockState.getBlock());
			colorMap.remove(blockState.getBlock());
			blockColor = getColorTextureForBlock(blockState, blockPos, false);
		}
		
		//if the blockColor is still 0 we use the default material color
		if (blockColor == 0)
		{
			tintColor.replace(blockState.getBlock(), 0);
			toTint.replace(blockState.getBlock(), false);
			colorMap.replace(blockState.getBlock(), blockState.getBlock().defaultMaterialColor().col);
		}
		
		if (toTint.get(blockState.getBlock()))
		{
			int tintValue = 0;
			if (useGrassTint(blockState.getBlock()))
				// grass and green plants
				tintValue = biome.getGrassColor(x, z);
			else if (useWaterTint(blockState.getBlock()))
				// water
				tintValue = biome.getWaterColor();
			else
				// leaves
				tintValue = biome.getFoliageColor();
			colorInt = ColorUtil.multiplyRGBcolors(tintValue | 0xFF000000, blockColor);
		}
		else
			colorInt = blockColor;
		return colorInt;
	}
	
	/** Returns a color int for the given biome. */
	private int getColorForBiome(int x, int z, Biome biome)
	{
		int colorInt;
		
		switch (biome.getBiomeCategory())
		{
		
		case NETHER:
			colorInt = Blocks.NETHERRACK.defaultBlockState().materialColor.col;
			break;
		
		case THEEND:
			colorInt = Blocks.END_STONE.defaultBlockState().materialColor.col;
			break;
		
		case BEACH:
		case DESERT:
			colorInt = Blocks.SAND.defaultBlockState().materialColor.col;
			break;
		
		case EXTREME_HILLS:
			colorInt = Blocks.STONE.defaultMaterialColor().col;
			break;
		
		case MUSHROOM:
			colorInt = MaterialColor.COLOR_LIGHT_GRAY.col;
			break;
		
		case ICY:
			colorInt = Blocks.SNOW.defaultMaterialColor().col;
			break;
		
		case MESA:
			colorInt = Blocks.RED_SAND.defaultMaterialColor().col;
			break;
		
		case OCEAN:
		case RIVER:
			colorInt = biome.getWaterColor();
			break;
		
		case NONE:
		case FOREST:
		case TAIGA:
		case JUNGLE:
		case PLAINS:
		case SAVANNA:
		case SWAMP:
		default:
			Color tmp = LodUtil.intToColor(biome.getGrassColor(x, z));
			tmp = tmp.darker();
			colorInt = LodUtil.colorToInt(tmp);
			break;
			
		}
		
		return colorInt;
	}
	
	public static final ConcurrentMap<Block, Boolean> notFullBlock = new ConcurrentHashMap<>();
	public static final ConcurrentMap<Block, Boolean> smallBlock = new ConcurrentHashMap<>();
	
	/** Is the block at the given blockPos a valid LOD point? */
	private boolean isLayerValidLodPoint(IChunk chunk, BlockPos.Mutable blockPos)
	{
		BlockState blockState = chunk.getBlockState(blockPos);
		boolean avoidNonFullBlock = LodConfig.CLIENT.worldGenerator.avoidNonFullBlock.get();
		boolean	avoidBlockWithNoCollision = LodConfig.CLIENT.worldGenerator.avoidBlockWithNoCollision.get();
		if (blockState != null)
		{
			// TODO this code is dead since avoidSmallBlock and onlyUseFullBlock
			// are set to false and are never changed.
			// should this code be changed?
			
			if (avoidNonFullBlock)
			{
				if (!notFullBlock.containsKey(blockState.getBlock())
							|| notFullBlock.get(blockState.getBlock()) == null
				)
				{
					VoxelShape voxelShape = blockState.getShape(chunk, blockPos);
					if (!blockState.getFluidState().isEmpty())
					{
						notFullBlock.put(blockState.getBlock(), false);
					}
					
					if (!voxelShape.isEmpty())
					{
						AxisAlignedBB bbox = voxelShape.bounds();
						int xWidth = (int) (bbox.maxX - bbox.minX);
						int yWidth = (int) (bbox.maxY - bbox.minY);
						int zWidth = (int) (bbox.maxZ - bbox.minZ);
						if (xWidth < 1 && zWidth < 1 && yWidth < 1)
							notFullBlock.put(blockState.getBlock(), true);
						else
							notFullBlock.put(blockState.getBlock(), false);
					}
					else
					{
						notFullBlock.put(blockState.getBlock(), false);
					}
				}
				
				if (notFullBlock.get(blockState.getBlock()))
					return false;
			}
			
			if (avoidBlockWithNoCollision)
			{
				if (!smallBlock.containsKey(blockState.getBlock())
							|| smallBlock.get(blockState.getBlock()) == null
				)
				{
					if(!blockState.getFluidState().isEmpty())
						smallBlock.put(blockState.getBlock(), false);
						
					VoxelShape voxelShape = blockState.getCollisionShape(chunk, blockPos);
					if (!blockState.getFluidState().isEmpty())
					{
						smallBlock.put(blockState.getBlock(), false);
					}
					else
					{
						
						if (voxelShape.isEmpty())
						{
							smallBlock.put(blockState.getBlock(), true);
						/*AxisAlignedBB bbox = voxelShape.bounds();
						int xWidth = (int) (bbox.maxX - bbox.minX);
						int yWidth = (int) (bbox.maxY - bbox.minY);
						int zWidth = (int) (bbox.maxZ - bbox.minZ);
						
						if (xWidth < 0.7 && zWidth < 0.7 && yWidth < 0.7)
							smallBlock.put(blockState.getBlock(), true);
						else
							smallBlock.put(blockState.getBlock(), false);*/
						}
						else
						{
							smallBlock.put(blockState.getBlock(), false);
						}
					}
				}
				
				if (smallBlock.get(blockState.getBlock()))
					return false;
			}
			
			
			return blockState.getBlock() != Blocks.AIR
						   && blockState.getBlock() != Blocks.CAVE_AIR
						   && blockState.getBlock() != Blocks.BARRIER;
		}
		
		return false;
	}
}
