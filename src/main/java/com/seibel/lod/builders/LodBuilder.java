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
package com.seibel.lod.builders;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.enums.LodQualityMode;
import com.seibel.lod.util.*;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.block.*;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelDataMap;

import javax.swing.*;

/**
 * This object is in charge of creating Lod related objects. (specifically: Lod
 * World, Dimension, and Region objects)
 *
 * @author Leonardo Amato
 * @author James Seibel
 * @version 9-7-2021
 */
public class LodBuilder
{
	private ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));

	public static final int CHUNK_DATA_WIDTH = LodUtil.CHUNK_WIDTH;
	public static final int CHUNK_SECTION_HEIGHT = CHUNK_DATA_WIDTH;
	public static final Heightmap.Type DEFAULT_HEIGHTMAP = Heightmap.Type.WORLD_SURFACE_WG;
	public static final ConcurrentMap<Block, Integer> colorMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<Block, VoxelShape> shapeMap = new ConcurrentHashMap<>();

	public static final ModelDataMap dataMap = new ModelDataMap.Builder().build() ;

	/**
	 * If no blocks are found in the area in determineBottomPointForArea return this
	 */
	public static final short DEFAULT_DEPTH = 0;
	/**
	 * If no blocks are found in the area in determineHeightPointForArea return this
	 */
	public static final short DEFAULT_HEIGHT = 0;

	/**
	 * How wide LodDimensions should be in regions
	 */
	public int defaultDimensionWidthInRegions = 5;

	public LodBuilder()
	{

	}

	public void generateLodNodeAsync(IChunk chunk, LodWorld lodWorld, IWorld world)
	{
		generateLodNodeAsync(chunk, lodWorld, world, DistanceGenerationMode.SERVER);
	}


	public void generateLodNodeAsync(IChunk chunk, LodWorld lodWorld, IWorld world, DistanceGenerationMode generationMode)
	{
		if (lodWorld == null || !lodWorld.getIsWorldLoaded())
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
				DimensionType dim = world.dimensionType();

				LodDimension lodDim;

				int playerPosX;
				int playerPosZ;
				if (MinecraftWrapper.INSTANCE.getPlayer() == null)
				{
					playerPosX = chunk.getPos().getMinBlockX();
					playerPosZ = chunk.getPos().getMinBlockZ();
				} else
				{
					playerPosX = (int) MinecraftWrapper.INSTANCE.getPlayer().getX();
					playerPosZ = (int) MinecraftWrapper.INSTANCE.getPlayer().getZ();
				}
				if (lodWorld.getLodDimension(dim) == null)
				{
					lodDim = new LodDimension(dim, lodWorld, defaultDimensionWidthInRegions);
					lodWorld.addLodDimension(lodDim);
					lodDim.treeGenerator(playerPosX, playerPosZ);
				} else
				{
					lodDim = lodWorld.getLodDimension(dim);
				}
				generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig(generationMode));
			} catch (IllegalArgumentException | NullPointerException e)
			{
				e.printStackTrace();
				// if the world changes while LODs are being generated
				// they will throw errors as they try to access things that no longer
				// exist.
			}
		});
		lodGenThreadPool.execute(thread);

		return;
	}

	/**
	 * Creates a LodChunk for a chunk in the given world.
	 *
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 */
	public void generateLodNodeFromChunk(LodDimension lodDim, IChunk chunk) throws IllegalArgumentException
	{
		generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig());
	}

	/**
	 * Creates a LodChunk for a chunk in the given world.
	 *
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
		int color;
		byte light;
		short height;
		short depth;
		try
		{
			LodDetail detail;
			LodRegion region = lodDim.getRegion(chunk.getPos().getRegionX(), chunk.getPos().getRegionZ());
			if (region == null)
				return;
			byte minDetailLevel = region.getMinDetailLevel();
			detail = DetailDistanceUtil.getLodGenDetail(minDetailLevel);

			LodQualityMode lodQualityMode = LodConfig.CLIENT.worldGenerator.lodQualityMode.get();
			byte detailLevel = detail.detailLevel;
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
				long singleData = 0;
				long[] data = null;
				boolean isServer = config.distanceGenerationMode == DistanceGenerationMode.SERVER;

				switch (lodQualityMode)
				{
					default:
					case HEIGHTMAP:
						long[] dataToMergeSingle;
						dataToMergeSingle = createSingleDataToMerge(detail, chunk, config, startX, startZ, endX, endZ);
						singleData = DataPointUtil.mergeSingleData(dataToMergeSingle);
						lodDim.addSingleData(detailLevel,
								posX,
								posZ,
								singleData,
								false,
								isServer);
						break;
					case MULTI_LOD:
						long[][] dataToMergeVertical;
						dataToMergeVertical = createVerticalDataToMerge(detail, chunk, config, startX, startZ, endX, endZ);
						data = DataPointUtil.mergeVerticalData(dataToMergeVertical);
						if (data.length == 0 || data == null)
							data = new long[]{DataPointUtil.EMPTY_DATA};
						lodDim.addData(detailLevel,
								posX,
								posZ,
								data,
								false,
								isServer);
						break;
				}


			}
			lodDim.updateData(LodUtil.CHUNK_DETAIL_LEVEL, chunk.getPos().x, chunk.getPos().z);
		} catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	private long[][] createVerticalDataToMerge(LodDetail detail, IChunk chunk, LodBuilderConfig config, int startX, int startZ, int endX, int endZ)
	{
		long[][] dataToMerge = ThreadMapUtil.getBuilderVerticalArray()[detail.detailLevel];
		ChunkPos chunkPos = chunk.getPos();

		int size = 1 << detail.detailLevel;
		int height = 0;
		int depth = 0;
		int color = 0;
		int light = 0;
		int generation = config.distanceGenerationMode.complexity;

		int xRel;
		int zRel;
		int xAbs;
		int yAbs;
		int zAbs;

		BlockPos.Mutable blockPos = new BlockPos.Mutable(0, 0, 0);
		int index = 0;
		if (dataToMerge == null)
		{
			dataToMerge = new long[size * size][DataPointUtil.WORLD_HEIGHT];
		}
		//dataToMerge = new long[size * size][1024];

		for (index = 0; index < size * size; index++)
		{
			for (int i = 0; i < dataToMerge[index].length; i++)
			{
				dataToMerge[index][i] = 0;
			}
			xRel = Math.floorMod(index, size) + startX;
			zRel = Math.floorDiv(index, size) + startZ;
			xAbs = chunkPos.getMinBlockX() + xRel;
			zAbs = chunkPos.getMinBlockZ() + zRel;

			//Calculate the height of the lod
			yAbs = 255;
			int count = 0;
			while (yAbs > 0)
			{
				height = determineHeightPointFrom(chunk, config, xRel, zRel, yAbs, blockPos);

				//If the lod is at default, then we set this as void data
				if (height == DEFAULT_HEIGHT)
				{
					dataToMerge[index][0] = DataPointUtil.createVoidDataPoint(generation);
					break;
				}

				yAbs = height - 1;
				// We search light on above air block
				color = generateLodColor(chunk, config, xRel, yAbs, zRel, blockPos);
				depth = determineBottomPointFrom(chunk, config, xRel, zRel, yAbs, blockPos);
				blockPos.set(xAbs, yAbs + 1, zAbs);
				light = getLightValue(chunk, blockPos);

				//System.out.println(dataToMerge.length + " " + index +" " + count + " " + yAbs);
				//System.out.println(dataToMerge.length + " " + dataToMerge[index].length);
				dataToMerge[index][count] = DataPointUtil.createDataPoint(height, depth, color, (light >> 4) & 0b1111, light & 0b1111, generation);
				yAbs = depth - 1;
				count++;
			}
		}
		return dataToMerge;
	}

	/**
	 * Find the lowest valid point from the bottom.
	 */
	private short determineBottomPointFrom(IChunk chunk, LodBuilderConfig config, int xRel, int zRel, int yAbs, BlockPos.Mutable blockPos)
	{
		short depth = DEFAULT_DEPTH;
		if (config.useHeightmap)
		{
			depth = 0;
		} else
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
					if (!isLayerValidLodPoint(chunk, blockPos))
					{
						depth = (short) (sectionIndex * CHUNK_DATA_WIDTH + yRel + 1);
						voidData = false;
						break;
					}
				}
				if (!voidData)
				{
					break;
				}
			}
		}
		return depth;
	}

	/**
	 * Find the highest valid point from the Top
	 */
	private short determineHeightPointFrom(IChunk chunk, LodBuilderConfig config, int xRel, int zRel, int yAbs, BlockPos.Mutable blockPos)
	{
		short height = DEFAULT_HEIGHT;
		if (config.useHeightmap)
		{
			height = (short) chunk.getOrCreateHeightmapUnprimed(LodUtil.DEFAULT_HEIGHTMAP).getFirstAvailable(xRel, zRel);
		} else
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
				{
					break;
				}
			}
		}
		return height;
	}

	private long[] createSingleDataToMerge(LodDetail detail, IChunk chunk, LodBuilderConfig config, int startX, int startZ, int endX, int endZ)
	{
		long[] dataToMerge = ThreadMapUtil.getBuilderArray()[detail.detailLevel];
		ChunkPos chunkPos = chunk.getPos();

		int size = 1 << detail.detailLevel;
		int height = 0;
		int depth = 0;
		int color = 0;
		int light = 0;
		int generation = config.distanceGenerationMode.complexity;

		int xRel;
		int zRel;
		int xAbs;
		int yAbs;
		int zAbs;
		int lightBlock;
		int lightSky;


		BlockPos.Mutable blockPos = new BlockPos.Mutable(0, 0, 0);
		int index = 0;
		if (dataToMerge == null)
		{
			dataToMerge = new long[size * size];
		}
		for (index = 0; index < size * size; index++)
		{
			xRel = Math.floorMod(index, size) + startX;
			zRel = Math.floorDiv(index, size) + startZ;
			xAbs = chunkPos.getMinBlockX() + xRel;
			zAbs = chunkPos.getMinBlockZ() + zRel;

			//Calculate the height of the lod
			height = determineHeightPoint(chunk, config, xRel, zRel, blockPos);

			//If the lod is at default, then we set this as void data
			if (height == DEFAULT_HEIGHT)
			{
				dataToMerge[index] = DataPointUtil.createVoidDataPoint(generation);
				continue;
			}

			yAbs = height - 1;
			// We search light on above air block

			color = generateLodColor(chunk, config, xRel, yAbs, zRel, blockPos);
			depth = determineBottomPoint(chunk, config, xRel, zRel, blockPos);

			blockPos.set(xAbs, yAbs + 1, zAbs);
			light = getLightValue(chunk, blockPos);
			lightBlock = light & 0b1111;
			lightSky = (light >> 4) & 0b1111;
			dataToMerge[index] = DataPointUtil.createDataPoint(height, depth, color, lightSky, lightBlock, generation);
		}
		return dataToMerge;
	}
	// =====================//
	// constructor helpers //
	// =====================//

	/**
	 * Find the lowest valid point from the bottom.
	 */
	private short determineBottomPoint(IChunk chunk, LodBuilderConfig config, int xRel, int zRel, BlockPos.Mutable blockPos)
	{
		ChunkSection[] chunkSections = chunk.getSections();
		short depth = DEFAULT_DEPTH;
		if (config.useHeightmap)
		{
			depth = 0;
		} else
		{
			boolean found = false;
			for (int sectionIndex = 0; sectionIndex < chunkSections.length; sectionIndex++)
			{
				for (int yRel = 0; yRel < CHUNK_DATA_WIDTH; yRel++)
				{
					blockPos.set(chunk.getPos().getMinBlockX() + xRel, sectionIndex * CHUNK_DATA_WIDTH + yRel, chunk.getPos().getMinBlockZ() + zRel);
					if (isLayerValidLodPoint(chunk, blockPos))
					{
						depth = (short) (sectionIndex * CHUNK_DATA_WIDTH + yRel);
						found = true;
						break;
					}
				}
				if (found)
				{
					break;
				}
			}
		}
		return depth;
	}


	/**
	 * Find the highest valid point from the Top
	 */
	private short determineHeightPoint(IChunk chunk, LodBuilderConfig config, int xRel, int zRel, BlockPos.Mutable blockPos)
	{
		short height = DEFAULT_HEIGHT;
		if (config.useHeightmap)
		{
			height = (short) chunk.getOrCreateHeightmapUnprimed(LodUtil.DEFAULT_HEIGHTMAP).getFirstAvailable(xRel, zRel);
		} else
		{
			boolean voidData = true;
			ChunkSection[] chunkSections = chunk.getSections();
			for (int sectionIndex = chunkSections.length - 1; sectionIndex >= 0; sectionIndex--)
			{
				for (int yRel = CHUNK_DATA_WIDTH - 1; yRel >= 0; yRel--)
				{
					blockPos.set(chunk.getPos().getMinBlockX() + xRel, sectionIndex * CHUNK_DATA_WIDTH + yRel, chunk.getPos().getMinBlockZ() + zRel);
					if (isLayerValidLodPoint(chunk, blockPos))
					{
						height = (short) (sectionIndex * CHUNK_DATA_WIDTH + yRel + 1);
						voidData = false;
						break;
					}
				}
				if (!voidData)
				{
					break;
				}
			}
		}
		return height;
	}

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
		} else
		{
			int sectionIndex = Math.floorDiv(yAbs, CHUNK_SECTION_HEIGHT);
			int yRel = Math.floorMod(yAbs, CHUNK_SECTION_HEIGHT);
			if (chunkSections[sectionIndex] != null)
			{
				// the bit shift is equivalent to dividing by 4

				blockPos.set(chunk.getPos().getMinBlockX() + xRel, sectionIndex * CHUNK_DATA_WIDTH + yRel, chunk.getPos().getMinBlockZ() + zRel);
				//colorInt = getColorTextureForBlock(blockState, blockPos);
				colorInt = getColorForBlock(chunk, blockPos);
			}
			if (colorInt == 0 && yAbs > 0)
			{
				//invisible case
				colorInt = generateLodColor(chunk, config, xRel, yAbs - 1, zRel, blockPos);
			}
		}
		return colorInt;
	}

	private int getLightValue(IChunk chunk, BlockPos.Mutable blockPos)
	{
		int skyLight;
		int blockLight;
		if (MinecraftWrapper.INSTANCE.getPlayer() == null)
			return 0;
		if (MinecraftWrapper.INSTANCE.getPlayer().level == null)
			return 0;

		IWorld world = MinecraftWrapper.INSTANCE.getPlayer().level;

		blockLight = world.getBrightness(LightType.BLOCK, blockPos);
		skyLight = world.getBrightness(LightType.SKY, blockPos);

		blockPos.set(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ());
		BlockState blockState = chunk.getBlockState(blockPos);

		blockLight = LodUtil.clamp(0, blockLight + blockState.getLightValue(chunk, blockPos), 15);

		return blockLight + (skyLight << 4);
	}

	private int getColorTextureForBlock(BlockState blockState, BlockPos blockPos, boolean topTextureRequired)
	{
		if (colorMap.containsKey(blockState.getBlock()))
			return colorMap.get(blockState.getBlock());


		World world = MinecraftWrapper.INSTANCE.getWorld();
		TextureAtlasSprite texture;
		if(topTextureRequired)
		{
			List<BakedQuad> quad = ((IForgeBakedModel) MinecraftWrapper.INSTANCE.getModelManager().getBlockModelShaper().getBlockModel(blockState)).getQuads(blockState, Direction.UP, new Random(0), dataMap);
			if (!quad.isEmpty())
			{
				texture = quad.get(0).getSprite();
			}
			else
			{
				texture = MinecraftWrapper.INSTANCE.getModelManager().getBlockModelShaper().getTexture(blockState, world, blockPos);
			}
		}
		else
		{
			texture = MinecraftWrapper.INSTANCE.getModelManager().getBlockModelShaper().getTexture(blockState, world, blockPos);
		}


		int count = 0;
		int alpha = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		int color = 0;
		for (int k = 0; k < texture.getFrameCount(); k++)
		{
			for (int i = 0; i < texture.getHeight(); i++)
			{
				for (int j = 0; j < texture.getWidth(); j++)
				{
					if (texture.isTransparent(k, i, j))
					{
						if (blockState.getBlock() instanceof LeavesBlock)
							color = 0;
						else
							continue;
					} else
					{
						color = texture.getPixelRGBA(k, i, j);
					}
					count++;
					alpha += ColorUtil.getAlpha(color);
					red += ColorUtil.getBlue(color);
					green += ColorUtil.getGreen(color);
					blue += ColorUtil.getRed(color);
				}
			}
		}
		if (count == 0)
		{
			color = 0;
		} else
		{
			alpha /= count;
			red /= count;
			green /= count;
			blue /= count;
			color = ColorUtil.rgbToInt(alpha, red, green, blue);
		}
		colorMap.put(blockState.getBlock(), color);
		return color;
	}

	/**
	 * Returns a color int for the given block.
	 */
	private int getColorForBlock(IChunk chunk, BlockPos blockPos)
	{

		int xRel = blockPos.getX() - chunk.getPos().getMinBlockX();
		int zRel = blockPos.getZ() - chunk.getPos().getMinBlockZ();
		int x = blockPos.getX();
		int y = blockPos.getY();
		int z = blockPos.getZ();
		Biome biome = chunk.getBiomes().getNoiseBiome(xRel >> 2, y >> 2, zRel >> 2);
		int brightness;
		int red = 0;
		int green = 0;
		int blue = 0;

		BlockState blockState = chunk.getBlockState(blockPos);
		int colorInt = 0;


		// block special cases
		if (blockState == Blocks.AIR.defaultBlockState()
				    || blockState == Blocks.CAVE_AIR.defaultBlockState()
				    || blockState == Blocks.BARRIER.defaultBlockState())
		{
			Color tmp = LodUtil.intToColor(biome.getGrassColor(x, z));
			tmp = tmp.darker();
			colorInt = LodUtil.colorToInt(tmp);
		} else if (blockState == Blocks.NETHERRACK.defaultBlockState())
		{
			colorInt = LodUtil.NETHERRACK_COLOR_INT;
		} else if (blockState == Blocks.WARPED_NYLIUM.defaultBlockState())
		{
			colorInt = LodUtil.WARPED_NYLIUM_COLOR_INT;
		} else if (blockState == Blocks.CRIMSON_NYLIUM.defaultBlockState())
		{
			colorInt = LodUtil.CRIMSON_NYLIUM_COLOR_INT;
		} else if (blockState == Blocks.WEEPING_VINES.defaultBlockState()
				           || blockState == Blocks.WEEPING_VINES_PLANT.defaultBlockState()
				           || blockState == Blocks.CRIMSON_FUNGUS.defaultBlockState()
				           || blockState == Blocks.CRIMSON_ROOTS.defaultBlockState())
		{
			colorInt = Blocks.NETHER_WART_BLOCK.defaultMaterialColor().col;
		} else if (blockState.getBlock().equals(Blocks.TWISTING_VINES)
				           || blockState.equals(Blocks.TWISTING_VINES_PLANT)
				           || blockState == Blocks.WARPED_ROOTS.defaultBlockState()
				           || blockState == Blocks.WARPED_FUNGUS.defaultBlockState())
		{
			colorInt = Blocks.WARPED_NYLIUM.defaultMaterialColor().col;
		}


		// plant life
		else if (blockState.getBlock() instanceof LeavesBlock || blockState.getBlock() == Blocks.VINE)
		{
			brightness = getColorTextureForBlock(blockState, blockPos, false);
			colorInt = ColorUtil.changeBrightnessValue(biome.getFoliageColor(), brightness);
		} else if ((blockState.getBlock() instanceof GrassBlock || blockState.getBlock() instanceof AbstractPlantBlock
				            || blockState.getBlock() instanceof BushBlock || blockState.getBlock() instanceof IGrowable)
				           && !(blockState.getBlock() == Blocks.BROWN_MUSHROOM || blockState.getBlock() == Blocks.RED_MUSHROOM))
		{
			/*brightness = getColorTextureForBlock(blockState, blockPos, true);
			colorInt = ColorUtil.changeBrightnessValue(biome.getGrassColor(x, z), brightness);*/
			colorInt = ColorUtil.applySaturationAndBrightnessMultipliers(biome.getGrassColor(x, z), 1f, 0.65f);
		}
		// water
		else if (blockState.getBlock() == Blocks.WATER)
		{
			/*brightness = getColorTextureForBlock(blockState, blockPos, true);
			colorInt = ColorUtil.changeBrightnessValue(biome.getWaterColor(), brightness);*/
			colorInt = ColorUtil.applySaturationAndBrightnessMultipliers(biome.getWaterColor(), 1f, 0.75f);
		}

		// everything else
		else
		{
			colorInt = getColorTextureForBlock(blockState, blockPos, false);
		}

		return colorInt;
	}

	/**
	 * Returns a color int for the given biome.
	 */
	private int getColorForBiome(int x, int z, Biome biome)
	{
		int colorInt = 0;

		switch (biome.getBiomeCategory())
		{

			case NETHER:
				colorInt = LodUtil.NETHERRACK_COLOR_INT;
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

	/**
	 * Is the layer between the given X, Z, and dataIndex values a valid LOD point?
	 */
	private boolean isLayerValidLodPoint(IChunk chunk, BlockPos.Mutable blockPos)
	{

		BlockState blockState = chunk.getBlockState(blockPos);
		boolean onlyUseFullBlock = false;
		boolean avoidSmallBlock = false;
		if (blockState != null)
		{
			//blockState.isCollisionShapeFullBlock(chunk, blockPos);

			if (avoidSmallBlock || onlyUseFullBlock)
			{
				if (!blockState.getFluidState().isEmpty())
				{
					return true;
				}

				VoxelShape voxelShape;
				if (shapeMap.containsKey(blockState.getBlock()))
				{
					voxelShape = shapeMap.get(blockState.getBlock());

				} else
				{
					voxelShape = blockState.getShape(chunk, blockPos);
					shapeMap.put(blockState.getBlock(), voxelShape);
				}
				if (!voxelShape.isEmpty())
				{
					AxisAlignedBB bbox = voxelShape.bounds();
					int xWidth = (int) (bbox.maxX - bbox.minX);
					int yWidth = (int) (bbox.maxY - bbox.minY);
					int zWidth = (int) (bbox.maxZ - bbox.minZ);
					if (xWidth < 1 && zWidth < 1 && yWidth < 1 && onlyUseFullBlock)
					{
						return false;
					}
					if (xWidth < 0.7 && zWidth < 0.7 && yWidth < 1 && avoidSmallBlock)
					{
						return false;
					}
				} else
				{
					return false;
				}
			}

			if (blockState.getBlock() != Blocks.AIR
					    && blockState.getBlock() != Blocks.CAVE_AIR
					    && blockState.getBlock() != Blocks.BARRIER)
			{
				return true;
			}
		}

		return false;
	}
}
