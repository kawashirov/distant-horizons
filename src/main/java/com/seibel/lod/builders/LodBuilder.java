package com.seibel.lod.builders;

import java.awt.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.util.LodUtil;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;

/**
 * This object is in charge of creating Lod
 * related objects. 
 * (specifically: Lod World, Dimension, Region, and Chunk objects)
 * 
 * @author James Seibel
 * @version 6-27-2021
 */
public class LodBuilder
{
	private ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor();
	
	/** Default size of any LOD regions we use */
	public int regionWidth = 5;
	
	
	public static final int CHUNK_DATA_WIDTH = LodChunk.WIDTH;
	public static final int CHUNK_SECTION_HEIGHT = LodChunk.WIDTH;
	
	
	
	public LodBuilder()
	{
		
	}
	
	
	public void generateLodChunkAsync(IChunk chunk, LodWorld lodWorld, IWorld world)
	{
		generateLodChunkAsync(chunk, new LodBuilderConfig(), lodWorld, world);
	}
	
	public void generateLodChunkAsync(IChunk chunk, LodBuilderConfig config, LodWorld lodWorld, IWorld world)
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
				
				LodChunk lod = generateLodFromChunk(chunk, config);
				
				LodDimension lodDim;
				
				if (lodWorld.getLodDimension(dim) == null)
				{
					lodDim = new LodDimension(dim, lodWorld, regionWidth);
					lodWorld.addLodDimension(lodDim);
				}
				else
				{
					lodDim = lodWorld.getLodDimension(dim);
				}
				
				lodDim.addLod(lod);
			}
			catch(IllegalArgumentException | NullPointerException e)
			{
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
	 * @throws IllegalArgumentException 
	 * thrown if either the chunk or world is null.
	 */
	public LodChunk generateLodFromChunk(IChunk chunk) throws IllegalArgumentException
	{
		return generateLodFromChunk(chunk, new LodBuilderConfig());
	}
	
	/**
	 * Creates a LodChunk for a chunk in the given world.
	 * 
	 * @throws IllegalArgumentException 
	 * thrown if either the chunk or world is null.
	 */
	public LodChunk generateLodFromChunk(IChunk chunk, LodBuilderConfig config) throws IllegalArgumentException
	{
		if(chunk == null)
			throw new IllegalArgumentException("generateLodFromChunk given a null chunk");
		
		
		LodDetail detail = LodConfig.CLIENT.lodDetail.get();
		LodDataPoint[][] dataPoints = new LodDataPoint[detail.dataPointLengthCount][detail.dataPointLengthCount];
		
		for(int i = 0; i < detail.dataPointLengthCount * detail.dataPointLengthCount; i++)
		{
			int startX = detail.startX[i];
			int startZ = detail.startZ[i];
			int endX = detail.endX[i];
			int endZ = detail.endZ[i];
			
			Color color;
			
			color = generateLodColorForArea(chunk, config, startX, startZ, endX, endZ);

			
			short height;
			short depth;
			
			if (!config.useHeightmap)
			{
				height = determineHeightPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
				depth = determineBottomPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
			}
			else
			{
				height = determineHeightPoint(chunk.getOrCreateHeightmapUnprimed(LodChunk.DEFAULT_HEIGHTMAP), startX, startZ, endX, endZ);
				depth = 0;
			}
			
			int x = i / detail.dataPointLengthCount;
			int z = i % detail.dataPointLengthCount;
			
			dataPoints[x][z] = new LodDataPoint(height, depth, color);
		}
		
		return new LodChunk(chunk.getPos(), dataPoints, detail);
	}
	
	
	
	
	
	
	//=====================//
	// constructor helpers //
	//=====================//
	
	
	/**
	 * Find the lowest valid point from the bottom.
	 * @param chunkSections
	 * @param startX
	 * @param startZ
	 * @param endX
	 * @param endZ
	 */
	private short determineBottomPointForArea(ChunkSection[] chunkSections, 
			int startX, int startZ, int endX, int endZ)
	{
		int numberOfBlocksRequired = ((endX-startX) * (endZ-startZ) / 2);
		
		// search from the bottom up
		for(int section = 0; section < CHUNK_DATA_WIDTH; section++)
		{
			for(int y = 0; y < CHUNK_SECTION_HEIGHT; y++)
			{
				int numberOfBlocksFound = 0;
				
				for(int x = startX; x < endX; x++)
				{
					for(int z = startZ; z < endZ; z++)
					{
						if(isLayerValidLodPoint(chunkSections, section, y, x, z))
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
	 * @param chunkSections
	 * @param startX
	 * @param startZ
	 * @param endX
	 * @param endZ
	 */
	private short determineHeightPointForArea(ChunkSection[] chunkSections, 
			int startX, int startZ, int endX, int endZ)
	{
		int numberOfBlocksRequired = ((endX-startX) * (endZ-startZ) / 2);
		
		// search from the top down
		for(int section = chunkSections.length - 1; section >= 0; section--)
		{
			for(int y = CHUNK_DATA_WIDTH - 1; y >= 0; y--)
			{
				int numberOfBlocksFound = 0;
				
				for(int x = startX; x < endX; x++)
				{
					for(int z = startZ; z < endZ; z++)
					{
						if(isLayerValidLodPoint(chunkSections, section, y, x, z))
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
	 * Find the highest point from the Top
	 */
	private short determineHeightPoint(Heightmap heightmap,
			int startX, int startZ, int endX, int endZ)
	{
		short highest = 0;
		for(int x = startX; x < endX; x++)
		{
			for(int z = startZ; z < endZ; z++)
			{
				short newHeight = (short) heightmap.getFirstAvailable(x, z);
				if (newHeight > highest)
					highest = newHeight;
			}
		}
		
		return highest;
	}
	
	/**
	 * Generate the color for the given chunk using biome
	 * water color, foliage color, and grass color.
	 * 
	 * @param config_useSolidBlocksInColorGen <br>
	 * 			If true we look down from the top of the <br>
	 * 			chunk until we find a non-invisible block, and then use <br>
	 * 			its color. If false we generate the color immediately for <br>
	 * 			each x and z.
	 * @param config_useBiomeColors <br>
	 * 			If true use biome foliage, water, and grass colors, <br>
	 * 			otherwise use the  
	 */
	private Color  generateLodColorForArea(IChunk chunk, LodBuilderConfig config, int startX, int startZ, int endX, int endZ)
	{
		ChunkSection[] chunkSections = chunk.getSections();
		
		int numbOfBlocks = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		
		
		for(int x = startX; x < endX; x++)
		{
			for(int z = startZ; z < endZ; z++)
			{
				boolean foundBlock = false;
				
				// go top down
				for(int i = chunkSections.length - 1; !foundBlock && i >= 0; i--)
				{
					if( !foundBlock && (chunkSections[i] != null || !config.useSolidBlocksInColorGen))
					{
						for(int y = CHUNK_SECTION_HEIGHT - 1; !foundBlock && y >= 0; y--)
						{
							int colorInt = 0;
							BlockState blockState = null;
							
							if (chunkSections[i] != null)
							{
								blockState = chunkSections[i].getBlockState(x, y, z);
								colorInt = blockState.materialColor.col;
							}
							
							if(colorInt == 0 && config.useSolidBlocksInColorGen)
							{
								// skip air or invisible blocks
								continue;
							}
							
							if (config.useBiomeColors)
							{
								Biome biome = chunk.getBiomes().getNoiseBiome(x, y + i * chunkSections.length, z);
								
								if (biome.getBiomeCategory() == Biome.Category.OCEAN ||
										biome.getBiomeCategory() == Biome.Category.RIVER)
								{
									colorInt = biome.getWaterColor();
								}
								else if (biome.getBiomeCategory() == Biome.Category.EXTREME_HILLS)
								{
									colorInt = Blocks.STONE.defaultMaterialColor().col;
								}
								else if (biome.getBiomeCategory() == Biome.Category.ICY)
								{
									colorInt = LodUtil.colorToInt(Color.WHITE);
								}
								else if (biome.getBiomeCategory() == Biome.Category.THEEND)
								{
									colorInt = Blocks.END_STONE.defaultBlockState().materialColor.col;
								}
								else if (config.useSolidBlocksInColorGen)
								{
									colorInt = getColorForBlock(x, z, blockState, biome);
								}
								else
								{
									colorInt = biome.getGrassColor(x, z);
								}
							}
							else
							{
								Biome biome = chunk.getBiomes().getNoiseBiome(x, y + i * chunkSections.length, z);
								colorInt = getColorForBlock(x,z, blockState, biome);
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
		
		if(numbOfBlocks == 0)
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
		
		if (blockState == Blocks.AIR.defaultBlockState())
		{
			colorInt = biome.getGrassColor(x, z);
		}
		
		else if (blockState.getBlock() instanceof LeavesBlock)
		{
			Color leafColor = LodUtil.intToColor(biome.getFoliageColor()).darker();
			colorInt = LodUtil.colorToInt(leafColor);
		}
		else if (blockState.getBlock() instanceof GrassBlock)
		{
			colorInt = biome.getGrassColor(x, z);
		}
		else if (blockState.getBlock() instanceof FlowingFluidBlock)
		{
			colorInt = biome.getWaterColor();
		}
		else
		{
			colorInt = blockState.materialColor.col;
		}
		
		return colorInt;
	}
	
	
	
	/**
	 * Is the layer between the given X, Z, and dataIndex
	 * values a valid LOD point?
	 */
	private boolean isLayerValidLodPoint(
			ChunkSection[] chunkSections,
			int sectionIndex, int y, 
			int x, int z)
	{
		if(chunkSections[sectionIndex] == null)
		{
			// this section doesn't have any blocks,
			// it is not a valid section
			return false;
		}
		else
		{
			if(chunkSections[sectionIndex].getBlockState(x, y, z) != null && 
				chunkSections[sectionIndex].getBlockState(x, y, z).getBlock() != Blocks.AIR)
			{
				return true;
			}
		}
		
		return false;
	}
	
	


	
	
	
	
	
}
