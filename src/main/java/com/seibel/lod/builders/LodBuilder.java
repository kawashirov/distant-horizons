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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.util.*;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.block.*;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;

import javax.xml.crypto.Data;

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
					playerPosX = (int) world.players().get(0).getX();
					playerPosZ = (int) world.players().get(0).getZ();
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
				//e.printStackTrace();
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
		long data;
		try
		{
			LodDetail detail;
			LodRegion region = lodDim.getRegion(chunk.getPos().getRegionX(), chunk.getPos().getRegionZ());
			if (region == null)
				return;
			byte minDetailLevel = region.getMinDetailLevel();
			detail = DetailDistanceUtil.getLodGenDetail(minDetailLevel);

			byte detailLevel = detail.detailLevel;
			int posX;
			int posZ;
			for (int i = 0; i < detail.dataPointLengthCount * detail.dataPointLengthCount; i++)
			{
				startX = detail.startX[i];
				startZ = detail.startZ[i];
				endX = detail.endX[i];
				endZ = detail.endZ[i];

				/*
				color = generateLodColorForArea(chunk, config, startX, startZ, endX, endZ);
				light = generateLodLightForArea(chunk, config, startX, startZ, endX, endZ);

				if (!config.useHeightmap)
				{
					height = determineHeightPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
					depth = determineBottomPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
				} else
				{
					height = determineHeightPoint(chunk.getOrCreateHeightmapUnprimed(LodUtil.DEFAULT_HEIGHTMAP), startX,
							startZ, endX, endZ);
					depth = 0;
				}*/
				posX = LevelPosUtil.convert((byte) 0, chunk.getPos().x * 16 + startX, detail.detailLevel);
				posZ = LevelPosUtil.convert((byte) 0, chunk.getPos().z * 16 + startZ, detail.detailLevel);
				long[] dataToMerge = createSingleDataToMerge(detail, chunk, config, startX, startZ, endX, endZ);
				boolean isServer = config.distanceGenerationMode == DistanceGenerationMode.SERVER;
				data = DataPointUtil.mergeSingleData(dataToMerge);
				lodDim.addData(detailLevel,
						posX,
						posZ,
						data,
						false,
						isServer);
			}
			lodDim.updateData(LodUtil.CHUNK_DETAIL_LEVEL, chunk.getPos().x, chunk.getPos().z);
		} catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}

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
			height = determineHeightPoint(chunk, config, xRel, zRel);

			//If the lod is at default, then we set this as void data
			if (height == DEFAULT_HEIGHT)
			{
				dataToMerge[index] = DataPointUtil.createVoidDataPoint(generation);
				continue;
			}

			yAbs = height - 1;
			// We search light on above air block
			blockPos.set(xAbs, yAbs + 1, zAbs);

			color = generateLodColor(chunk, config, xRel, yAbs, zRel);
			light = getLightBlockValue(chunk, blockPos);
			depth = determineBottomPoint(chunk, config, xRel, zRel);

			dataToMerge[index] = DataPointUtil.createDataPoint(height, depth, color, light, generation);
		}
		return dataToMerge;
	}
	// =====================//
	// constructor helpers //
	// =====================//

	/**
	 * Find the lowest valid point from the bottom.
	 */
	private short determineBottomPoint(IChunk chunk, LodBuilderConfig config, int xRel, int zRel)
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
					if (isLayerValidLodPoint(chunkSections, sectionIndex, yRel, xRel, zRel))
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
	private short determineHeightPoint(IChunk chunk, LodBuilderConfig config, int xRel, int zRel)
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
					if (isLayerValidLodPoint(chunkSections, sectionIndex, yRel, xRel, zRel))
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
	private int generateLodColor(IChunk chunk, LodBuilderConfig config, int xRel, int yAbs, int zRel)
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
				BlockState blockState = chunkSections[sectionIndex].getBlockState(xRel, yRel, zRel);

				// the bit shift is equivalent to dividing by 4
				Biome biome = chunk.getBiomes().getNoiseBiome(xRel >> 2, yAbs >> 2, zRel >> 2);

				colorInt = getColorForBlock(xRel, zRel, blockState, biome);
			}
			if (colorInt == 0 && yAbs > 0)
			{
				//invisible case
				colorInt = generateLodColor(chunk, config, xRel, yAbs - 1, zRel);
			}
		}
		return colorInt;
	}

	private int getLightBlockValue(IChunk chunk, BlockPos.Mutable blockPos)
	{
		int lightBlock;

		//*TODO choose the best one between those options*/
		//lightBlock = MinecraftWrapper.INSTANCE.getPlayer().level.getLightEngine().getLayerListener(LightType.BLOCK).getLightValue(blockPos);
		//lightBlock = (byte) MinecraftWrapper.INSTANCE.getPlayer().level.getLightEngine().blockEngine.getLightValue(blockPos);
		lightBlock = (byte) MinecraftWrapper.INSTANCE.getPlayer().level.getBrightness(LightType.BLOCK, blockPos);
		//BlockState blockState = chunk.getBlockState(blockPos);
		//lightBlock = (byte) blockState.getLightBlock(chunk, blockPos);
		return lightBlock;
	}

	/**
	 * Returns a color int for the given block.
	 */
	private int getColorForBlock(int x, int z, BlockState blockState, Biome biome)
	{
		int colorInt = 0;

		// block special cases
		if (blockState == Blocks.AIR.defaultBlockState()
				    || blockState == Blocks.CAVE_AIR.defaultBlockState()
				    || blockState == Blocks.BARRIER.defaultBlockState())
		{
			Color tmp = LodUtil.intToColor(biome.getGrassColor(x, z));
			tmp = tmp.darker();
			colorInt = LodUtil.colorToInt(tmp);
		} else if (blockState == Blocks.STONE.defaultBlockState())
		{
			colorInt = LodUtil.STONE_COLOR_INT;
		} else if (blockState == Blocks.MYCELIUM.defaultBlockState())
		{
			colorInt = LodUtil.MYCELIUM_COLOR_INT;
		} else if (blockState == Blocks.SOUL_TORCH.defaultBlockState()
				           || blockState == Blocks.SOUL_WALL_TORCH.defaultBlockState())
		{
			colorInt = Blocks.WARPED_PLANKS.defaultMaterialColor().col;
		} else if (blockState == Blocks.TORCH.defaultBlockState()
				           || blockState == Blocks.WALL_TORCH.defaultBlockState())
		{
			colorInt = Blocks.OAK_PLANKS.defaultMaterialColor().col;
		} else if (blockState == Blocks.REDSTONE_TORCH.defaultBlockState()
				           || blockState == Blocks.REDSTONE_WALL_TORCH.defaultBlockState())
		{
			colorInt = Blocks.CRIMSON_PLANKS.defaultMaterialColor().col;
		}


		// plant life
		else if (blockState.getBlock() instanceof LeavesBlock || blockState.getBlock() == Blocks.VINE)
		{
			Color leafColor = LodUtil.intToColor(biome.getFoliageColor()).darker();
			leafColor = leafColor.darker();
			colorInt = LodUtil.colorToInt(leafColor);
		} else if ((blockState.getBlock() instanceof GrassBlock || blockState.getBlock() instanceof AbstractPlantBlock
				            || blockState.getBlock() instanceof BushBlock || blockState.getBlock() instanceof IGrowable)
				           && !(blockState.getBlock() == Blocks.BROWN_MUSHROOM || blockState.getBlock() == Blocks.RED_MUSHROOM))
		{
			Color plantColor = LodUtil.intToColor(biome.getGrassColor(x, z));
			plantColor = plantColor.darker();
			colorInt = LodUtil.colorToInt(plantColor);
		}

		// water
		else if (blockState.getBlock() == Blocks.WATER)
		{
			colorInt = biome.getWaterColor();
		}

		// everything else
		else
		{
			colorInt = blockState.getBlock().defaultMaterialColor().col;
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
				colorInt = Blocks.BEDROCK.defaultBlockState().materialColor.col;
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
	private boolean isLayerValidLodPoint(ChunkSection[] chunkSections, int sectionIndex, int y, int x, int z)
	{
		if (chunkSections[sectionIndex] == null)
		{
			// this section doesn't have any blocks,
			// it is not a valid section
			return false;
		} else
		{
			if (chunkSections[sectionIndex].getBlockState(x, y, z) != null
					    && chunkSections[sectionIndex].getBlockState(x, y, z).getBlock() != Blocks.AIR
					    && chunkSections[sectionIndex].getBlockState(x, y, z).getBlock() != Blocks.CAVE_AIR
					    && chunkSections[sectionIndex].getBlockState(x, y, z).getBlock() != Blocks.BARRIER)
			{
				return true;
			}
		}

		return false;
	}
}
