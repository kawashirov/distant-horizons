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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodQuadTreeDimension;
import com.seibel.lod.objects.LodQuadTreeNode;
import com.seibel.lod.objects.LodQuadTreeWorld;
import com.seibel.lod.util.LodThreadFactory;
import com.seibel.lod.util.LodUtil;

import net.minecraft.block.AbstractPlantBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;

/**
 * This object is in charge of creating Lod related objects. (specifically: Lod
 * World, Dimension, Region, and Chunk objects)
 *
 * @author Leonardo Amato
 * @author James Seibel
 * @version 8-14-2021
 */
public class LodNodeBuilder
{
	private ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));
	
	public static final int CHUNK_DATA_WIDTH = LodUtil.CHUNK_WIDTH;
	public static final int CHUNK_SECTION_HEIGHT = CHUNK_DATA_WIDTH;
	public static final Heightmap.Type DEFAULT_HEIGHTMAP = Heightmap.Type.WORLD_SURFACE_WG;
	
	/**
	 * Default size of any LOD regions we use
	 */
	public int regionWidth = 5;
	
	public LodNodeBuilder()
	{
		
	}
	
	public void generateLodNodeAsync(IChunk chunk, LodQuadTreeWorld lodWorld, IWorld world)
	{
		generateLodNodeAsync(chunk, lodWorld, world, DistanceGenerationMode.SERVER);
	}
	
	public void generateLodNodeAsync(IChunk chunk, LodQuadTreeWorld lodWorld, IWorld world, DistanceGenerationMode generationMode)
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
				
				List<LodQuadTreeNode> nodeList = generateLodNodeFromChunk(chunk, new LodBuilderConfig(generationMode));
				
				LodQuadTreeDimension lodDim;
				
				if (lodWorld.getLodDimension(dim) == null)
				{
					lodDim = new LodQuadTreeDimension(dim, lodWorld, regionWidth);
					lodWorld.addLodDimension(lodDim);
				}
				else
				{
					lodDim = lodWorld.getLodDimension(dim);
				}
				
				for (LodQuadTreeNode node : nodeList)
				{
					lodDim.addNode(node);
					
				}
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
		
		return;
	}
	
	/**
	 * Creates a LodChunk for a chunk in the given world.
	 *
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 */
	public List<LodQuadTreeNode> generateLodNodeFromChunk(IChunk chunk) throws IllegalArgumentException
	{
		return generateLodNodeFromChunk(chunk, new LodBuilderConfig());
	}
	
	/**
	 * Creates a LodChunk for a chunk in the given world.
	 *
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 */
	public List<LodQuadTreeNode> generateLodNodeFromChunk(IChunk chunk, LodBuilderConfig config)
			throws IllegalArgumentException
	{
		LodDetail detail = LodConfig.CLIENT.maxGenerationDetail.get();
		List<LodQuadTreeNode> lodNodeList = new ArrayList<>();
		
		if (chunk == null)
			throw new IllegalArgumentException("generateLodFromChunk given a null chunk");
		
		
		for (int i = 0; i < detail.dataPointLengthCount * detail.dataPointLengthCount; i++)
		{
			int startX = detail.startX[i];
			int startZ = detail.startZ[i];
			int endX = detail.endX[i];
			int endZ = detail.endZ[i];
			
			Color color = generateLodColorForArea(chunk, config, startX, startZ, endX, endZ);
			
			short height;
			short depth;
			
			if (!config.useHeightmap)
			{
				height = determineHeightPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
				depth = determineBottomPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
			}
			else
			{
				height = determineHeightPoint(chunk.getOrCreateHeightmapUnprimed(LodUtil.DEFAULT_HEIGHTMAP), startX,
						startZ, endX, endZ);
				depth = 0;
			}
			
			lodNodeList.add(new LodQuadTreeNode((byte) detail.detailLevel,
					LodUtil.convertLevelPos(chunk.getPos().getMinBlockX() + startX, 0, detail.detailLevel),
					LodUtil.convertLevelPos(chunk.getPos().getMinBlockZ() + startZ, 0, detail.detailLevel),
					new LodDataPoint(height, depth, color), config.distanceGenerationMode));
			
		}
		
		return lodNodeList;
	}
	
	// =====================//
	// constructor helpers //
	// =====================//
	
	/**
	 * Find the lowest valid point from the bottom.
	 *
	 * @param chunkSections
	 * @param startX
	 * @param startZ
	 * @param endX
	 * @param endZ
	 */
	private short determineBottomPointForArea(ChunkSection[] chunkSections, int startX, int startZ, int endX, int endZ)
	{
		int numberOfBlocksRequired = ((endX - startX) * (endZ - startZ) / 2);
		
		// search from the bottom up
		for (int section = 0; section < CHUNK_DATA_WIDTH; section++)
		{
			for (int y = 0; y < CHUNK_SECTION_HEIGHT; y++)
			{
				int numberOfBlocksFound = 0;
				
				for (int x = startX; x < endX; x++)
				{
					for (int z = startZ; z < endZ; z++)
					{
						if (isLayerValidLodPoint(chunkSections, section, y, x, z))
						{
							numberOfBlocksFound++;
							
							if (numberOfBlocksFound >= numberOfBlocksRequired)
							{
								// we found
								// enough blocks in this
								// layer to count as an
								// LOD point
								return (short) (y + (section * CHUNK_SECTION_HEIGHT));
							}
						}
					}
				}
			}
		}
		
		// we never found a valid LOD point
		return -1;
	}
	
	/**
	 * Find the lowest valid point from the bottom.
	 */
	@SuppressWarnings("unused")
	private short determineBottomPoint(Heightmap heightmap)
	{
		// the heightmap only shows how high the blocks go, it
		// doesn't have any info about how low they go
		return 0;
	}
	
	/**
	 * Find the highest valid point from the Top
	 *
	 * @param chunkSections
	 * @param startX
	 * @param startZ
	 * @param endX
	 * @param endZ
	 */
	private short determineHeightPointForArea(ChunkSection[] chunkSections, int startX, int startZ, int endX, int endZ)
	{
		int numberOfBlocksRequired = ((endX - startX) * (endZ - startZ) / 2);
		// search from the top down
		for (int section = chunkSections.length - 1; section >= 0; section--)
		{
			for (int y = CHUNK_DATA_WIDTH - 1; y >= 0; y--)
			{
				int numberOfBlocksFound = 0;
				
				for (int x = startX; x < endX; x++)
				{
					for (int z = startZ; z < endZ; z++)
					{
						if (isLayerValidLodPoint(chunkSections, section, y, x, z))
						{
							numberOfBlocksFound++;
							
							if (numberOfBlocksFound >= numberOfBlocksRequired)
							{
								// we found
								// enough blocks in this
								// layer to count as an
								// LOD point
								return (short) (y + 1 + (section * CHUNK_SECTION_HEIGHT));
							}
						}
					}
				}
			}
		}
		
		// we never found a valid LOD point
		return -1;
	}
	
	/**
	 * Find the highest point from the Top
	 */
	private short determineHeightPoint(Heightmap heightmap, int startX, int startZ, int endX, int endZ)
	{
		short highest = 0;
		for (int x = startX; x < endX; x++)
		{
			for (int z = startZ; z < endZ; z++)
			{
				short newHeight = (short) heightmap.getFirstAvailable(x, z);
				if (newHeight > highest)
					highest = newHeight;
			}
		}
		
		return highest;
	}
	
	/**
	 * Generate the color for the given chunk using biome water color, foliage
	 * color, and grass color.
	 *
	 * @param config_useSolidBlocksInColorGen <br>
	 *                                        If true we look down from the top of
	 *                                        the <br>
	 *                                        chunk until we find a non-invisible
	 *                                        block, and then use <br>
	 *                                        its color. If false we generate the
	 *                                        color immediately for <br>
	 *                                        each x and z.
	 * @param config_useBiomeColors           <br>
	 *                                        If true use biome foliage, water, and
	 *                                        grass colors, <br>
	 *                                        otherwise only use the block's
	 *                                        material color
	 */
	private Color generateLodColorForArea(IChunk chunk, LodBuilderConfig config, int startX, int startZ, int endX,
			int endZ)
	{
		ChunkSection[] chunkSections = chunk.getSections();
		
		int numbOfBlocks = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		
		for (int x = startX; x < endX; x++)
		{
			for (int z = startZ; z < endZ; z++)
			{
				boolean foundBlock = false;
				
				// go top down
				for (int i = chunkSections.length - 1; !foundBlock && i >= 0; i--)
				{
					if (!foundBlock && (chunkSections[i] != null || !config.useSolidBlocksInColorGen))
					{
						for (int y = CHUNK_SECTION_HEIGHT - 1; !foundBlock && y >= 0; y--)
						{
							int colorInt = 0;
							BlockState blockState = null;
							
							if (chunkSections[i] != null)
							{
								blockState = chunkSections[i].getBlockState(x, y, z);
								colorInt = blockState.materialColor.col;
							}
							
							if (colorInt == 0 && config.useSolidBlocksInColorGen)
							{
								// skip air or invisible blocks
								continue;
							}
							
							if (config.useBiomeColors)
							{
								// I have no idea why I need to bit shift to the right, but
								// if I don't the biomes don't show up correctly.
								Biome biome = chunk.getBiomes().getNoiseBiome(x >> 2, y + 1 * chunkSections.length >> 2,
										z >> 2);
								colorInt = getColorForBiome(x, z, biome);
							}
							else
							{
								
								// the bit shift is equivalent to dividing by 4
								Biome biome = chunk.getBiomes().getNoiseBiome(x >> 2, y + i * chunkSections.length >> 2,
										z >> 2);
								colorInt = getColorForBlock(x, z, blockState, biome);
							}
							
							Color c = LodUtil.intToColor(colorInt);
							
							red += c.getRed();
							green += c.getGreen();
							blue += c.getBlue();
							
							numbOfBlocks++;
							
							// we found a valid block, skip to the
							// next x and z
							foundBlock = true;
						}
					}
				}
			}
		}
		
		if (numbOfBlocks == 0)
			numbOfBlocks = 1;
		
		red /= numbOfBlocks;
		green /= numbOfBlocks;
		blue /= numbOfBlocks;
		
		return new Color(red, green, blue);
	}
	
	/**
	 * Returns a color int for a given block.
	 */
	private int getColorForBlock(int x, int z, BlockState blockState, Biome biome)
	{
		int colorInt = 0;
		
		// block special cases
		if (blockState == Blocks.AIR.defaultBlockState())
		{
			Color tmp = LodUtil.intToColor(biome.getGrassColor(x, z));
			tmp = tmp.darker();
			colorInt = LodUtil.colorToInt(tmp);
		}
		else if (blockState == Blocks.MYCELIUM.defaultBlockState())
		{
			colorInt = MaterialColor.COLOR_LIGHT_GRAY.col;
		}
		
		// plant life
		else if (blockState.getBlock() instanceof LeavesBlock)
		{
			Color leafColor = LodUtil.intToColor(biome.getFoliageColor()).darker();
			colorInt = LodUtil.colorToInt(leafColor);
		}
		else if (blockState.getBlock() instanceof GrassBlock || blockState.getBlock() instanceof AbstractPlantBlock
				|| blockState.getBlock() instanceof BushBlock || blockState.getBlock() instanceof IGrowable)
		{
			Color tmp = LodUtil.intToColor(biome.getGrassColor(x, z));
			tmp = tmp.darker();
			colorInt = LodUtil.colorToInt(tmp);
		}
		
		// water
		else if (blockState.getBlock() == Blocks.WATER)
		{
			colorInt = biome.getWaterColor();
		}
		
		// everything else
		else
		{
			colorInt = blockState.materialColor.col;
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
		}
		else
		{
			if (chunkSections[sectionIndex].getBlockState(x, y, z) != null
					&& chunkSections[sectionIndex].getBlockState(x, y, z).getBlock() != Blocks.AIR)
			{
				return true;
			}
		}
		
		return false;
	}
	
}
