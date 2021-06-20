package com.seibel.lod.builders;

import java.awt.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.enums.ColorDirection;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.handlers.LodConfigHandler;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDataPoint;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.util.LodUtil;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;

/**
 * This object is in charge of creating Lod
 * related objects. 
 * (specifically: Lod World, Dimension, Region, and Chunk objects)
 * 
 * @author James Seibel
 * @version 6-19-2021
 */
public class LodBuilder
{
	private static final Color INVISIBLE = new Color(0,0,0,0);

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
		if (lodWorld == null || !lodWorld.getIsWorldLoaded())
			return;
			
		// is this chunk from the same world as the lodWorld?
		if (!lodWorld.getWorldName().equals(LodUtil.getWorldID(world)))
			// we are not in the same world anymore
			// don't add this LOD
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
				DimensionType dim = world.getDimensionType();
				
				LodChunk lod = generateLodFromChunk(chunk);
				
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
		if(chunk == null)
			throw new IllegalArgumentException("generateLodFromChunk given a null chunk");
		
		
		LodDetail detail = LodConfigHandler.CLIENT.lodDetail.get();
		LodDataPoint[][] dataPoints = new LodDataPoint[detail.lengthCount][detail.lengthCount];
		
		for(int i = 0; i < detail.lengthCount * detail.lengthCount; i++)
		{
			int startX = detail.startX[i];
			int startZ = detail.startZ[i];
			int endX = detail.endX[i];
			int endZ = detail.endZ[i];
			
			
			Color color = generateLodColorForAreaInDirection(chunk, ColorDirection.TOP, startX, startZ, endX, endZ);
			
			short height = determineHeightPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
			short depth = determineBottomPointForArea(chunk.getSections(), startX, startZ, endX, endZ);
			
			int x = i / detail.lengthCount;
			int z = i % detail.lengthCount;
			
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
	@SuppressWarnings("unused")
	private short determineHeightPoint(Heightmap heightmap, int endZ)
	{
		short highest = 0;
		for(int x = 0; x < LodChunk.WIDTH; x++)
		{
			for(int z = 0; z < LodChunk.WIDTH; z++)
			{
				short newHeight = (short) heightmap.getHeight(x, z);
				if (newHeight > highest)
					highest = newHeight;
			}
		}
		
		return highest;
	}
	
	/**
	 * Generate the color for the given chunk in the given ColorDirection.
	 * NOTE: only vertical is currently implemented for area,
	 * the horizontal colors will always be the same regardless of the area.
	 */
	private Color  generateLodColorForAreaInDirection(IChunk chunk, ColorDirection colorDir, int startX, int startZ, int endX, int endZ)
	{
		Minecraft mc =  Minecraft.getInstance();
		BlockColors bc = mc.getBlockColors();
		
		switch (colorDir)
		{
		case TOP:
			return generateLodColorVerticalOverArea(chunk, colorDir, bc, startX, startZ, endX, endZ);
		case BOTTOM:
			return generateLodColorVerticalOverArea(chunk, colorDir, bc, startX, startZ, endX, endZ);
			
		case NORTH:
			return generateLodColorHorizontal(chunk, colorDir, bc);
		case SOUTH:
			return generateLodColorHorizontal(chunk, colorDir, bc);
			
		case EAST:
			return generateLodColorHorizontal(chunk, colorDir, bc);
		case WEST:
			return generateLodColorHorizontal(chunk, colorDir, bc);
		}
		
		return INVISIBLE;
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
	
	/**
	 * Generates the color of the top or bottom of the given chunk.
	 * 
	 * @throws IllegalArgumentException if given a ColorDirection other than TOP or BOTTOM
	 */
	private Color generateLodColorVerticalOverArea(
			IChunk chunk, ColorDirection colorDir, BlockColors bc,
			int startX, int startZ, int endX, int endZ)
	{
		if(colorDir != ColorDirection.TOP && colorDir != ColorDirection.BOTTOM)
		{
			throw new IllegalArgumentException("generateLodColorVertical only accepts the ColorDirection TOP or BOTTOM");
		}
		
		
		
		ChunkSection[] chunkSections = chunk.getSections();
		
		int numbOfBlocks = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		
		boolean goTopDown = (colorDir == ColorDirection.TOP);
		
		
		// either go top down or bottom up
		int dataStart = goTopDown? chunkSections.length - 1 : 0;
		int dataMax = chunkSections.length; 
		int dataMin = 0;
		int dataIncrement = goTopDown? -1 : 1;
		
		int topStart = goTopDown? CHUNK_SECTION_HEIGHT - 1 : 0;
		int topMax = CHUNK_SECTION_HEIGHT;
		int topMin = 0;
		int topIncrement =  goTopDown? -1 : 1;
		
		for(int x = startX; x < endX; x++)
		{
			for(int z = startZ; z < endZ; z++)
			{
				boolean foundBlock = false;
				
				for(int i = dataStart; !foundBlock && i >= dataMin && i < dataMax; i += dataIncrement)
				{
					if(!foundBlock && chunkSections[i] != null)
					{
						for(int y = topStart; !foundBlock && y >= topMin && y < topMax; y += topIncrement)
						{
							int ci;
							ci = chunkSections[i].getBlockState(x, y, z).materialColor.colorValue;
							
							if(ci == 0)
							{
								// skip air or invisible blocks
								continue;
							}
							
							Color c = LodUtil.intToColor(ci);
							
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
		
		/*
		 * unused variation that can be used with only the heightmap,
		 * although it just returns the foliage color, so it shouldn't
		 * be used normally.
			
			Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
			
			int numbOfBlocks = CHUNK_DATA_WIDTH * CHUNK_DATA_WIDTH;
			int red = 0;
			int green = 0;
			int blue = 0;
			
			for(int x = 0; x < CHUNK_DATA_WIDTH; x++)
			{
				Biome biome = chunk.getBiomes().getNoiseBiome(x,z, heightmap.getHeight(x, z));
				Color c = intToColor(biome.getFoliageColor());
				
				red += c.getRed();
				green += c.getGreen();
				blue += c.getBlue();
			}
		}
		
		red /= numbOfBlocks;
		green /= numbOfBlocks;
		blue /= numbOfBlocks;
		
		return new Color(red, green, blue);
	 */
	
	
	/**
	 * Generates the color for the sides of the given chunk.
	 */
	private Color generateLodColorHorizontal(
			IChunk chunk, ColorDirection colorDir, BlockColors bc)
	{
		if(colorDir != ColorDirection.NORTH && colorDir != ColorDirection.SOUTH && colorDir != ColorDirection.EAST && colorDir != ColorDirection.WEST)
		{
			throw new IllegalArgumentException("generateLodColorHorizontal only accepts the ColorDirection N (North), S (South), E (East), or W (West)");
		}
		
		ChunkSection[] chunkSections = chunk.getSections();
		
		int numbOfBlocks = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		
		
		// these don't change since the over direction doesn't matter
		int overStart = 0;
		int overIncrement = 1;
		
		// determine which direction is "in"
		int inStart = 0;
		int inIncrement = 1;
		switch (colorDir)
		{
		case NORTH:
			inStart = 0;
			inIncrement = 1;
			break;
		case SOUTH:
			inStart = CHUNK_DATA_WIDTH - 1;
			inIncrement = -1;
			break;
		case EAST:
			inStart = 0;
			inIncrement = 1;
			break;
		case WEST:
			inStart = CHUNK_DATA_WIDTH - 1;
			inIncrement = -1;
			break;
		default:
			// we were given an invalid position, return invisible.
			// this shouldn't happen and is mostly here to make the
			// compiler happy
			return INVISIBLE;
		}
		
		
		for (int section = 0; section < chunkSections.length; section++)
		{
			if (chunkSections[section] == null)
				continue;
			
			for (int y = 0; y < CHUNK_SECTION_HEIGHT; y++)
			{
				boolean foundBlock = false;
				
				// over moves "over" the side of the chunk
				// in moves "into" the chunk until it finds a block
				for (int over = overStart; !foundBlock && over >= 0 && over < CHUNK_DATA_WIDTH; over += overIncrement)
				{
					for (int in = inStart; !foundBlock && in >= 0 && in < CHUNK_DATA_WIDTH; in += inIncrement)
					{
						int x = -1;
						int z = -1;
						
						// determine which should be X and Z							
						switch(colorDir)
						{
						case NORTH:
							x = over;
							z = in;
							break;
						case SOUTH:
							x = over;
							z = in;
							break;
						case EAST:
							x = in;
							z = over;
							break;
						case WEST:
							x = in;
							z = over;
							break;
						default:
							// here to make the compiler happy
							break;
						}
						
						// if this block is buried, under other blocks
						// don't add it
						if(!isBlockPosVisible(chunkSections[section], x,y,z))
						{
							// go to the next "over" block location,
							// don't look at the next "in" location,
							// since the next "in" location will more than
							// likely still be covered
							in = CHUNK_DATA_WIDTH + 2;
							continue;
						}
						
						
						int ci;
						ci = chunkSections[section].getBlockState(x, y, z).getMaterial().getColor().colorValue;
						
						if (ci == 0) {
							// skip air or invisible blocks
							continue;
						}
						
						Color c = LodUtil.intToColor(ci);
						
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
		
		// if we didn't find any blocks return invisible
		if(numbOfBlocks == 0)
			return INVISIBLE;
		
		red /= numbOfBlocks;
		green /= numbOfBlocks;
		blue /= numbOfBlocks;
		
		return new Color(red, green, blue);
	}
	
	
	
	private static BlockState airState = Blocks.AIR.getDefaultState();
	
	/** 
	 * returns true if the block at the given coordinates is open to
	 * air on at least one side.
	 */
	private boolean isBlockPosVisible(ChunkSection chunkSection, int x, int y, int z)
	{
		/*
		// above
		if (y+1 < CHUNK_SECTION_HEIGHT) // don't go over the top
			if (chunkSection.getBlockState(x, y+1, z).getBlock() == (Blocks.AIR))
				return true;
		// below
		if (y-1 >= 0) // don't go below the bottom
			if (chunkSection.getBlockState(x, y-1, z).getBlock() == (Blocks.AIR))
				return true;
		*/
		
		// north
		if (z-1 > 0)
			if (chunkSection.getBlockState(x, y, z-1) == airState)
				return true;
		// south
		if (z+1 < LodChunk.WIDTH)
			if (chunkSection.getBlockState(x, y, z+1) == airState)
				return true;
		
		// east
		if (x+1 <= LodChunk.WIDTH)
			if (chunkSection.getBlockState(x+1, y, z) == airState)
				return true;
		// west
		if (x-1 >= 0)
			if (chunkSection.getBlockState(x-1, y, z) == airState)
				return true;
		
			
		return false;
	}



	
	
	
	
	
}
