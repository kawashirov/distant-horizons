package com.backsun.lod.builders;

import java.awt.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.backsun.lod.enums.ColorDirection;
import com.backsun.lod.enums.LodLocation;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.LodWorld;
import com.backsun.lod.util.LodUtils;

import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

/**
 * This object is in charge of creating Lod
 * related objects. 
 * (specifically: Lod World, Dimension, Region, and Chunk objects)
 * 
 * @author James Seibel
 * @version 4-01-2021
 */
public class LodBuilder
{
	private ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor();
	
	/** Default size of any LOD regions we use */
	public int regionWidth = 5;
	
	
	public static final int CHUNK_DATA_WIDTH = LodChunk.WIDTH;
	public static final int CHUNK_DATA_HEIGHT = LodChunk.WIDTH;
	
	/**
	 * This is how many blocks are
	 * required at a specific y-value
	 * to constitute a LOD point
	 */
	private final int LOD_BLOCK_REQ = 16;
	// the max number of blocks per layer = 64 (8*8)
	// since each layer is 1/4 the chunk
	
	
	public LodBuilder()
	{
		
	}
	
	
	
	public void generateLodChunkAsync(IChunk chunk, LodWorld lodWorld, DimensionType dim)
	{
		if (lodWorld == null || !lodWorld.getIsWorldLoaded())
			return;
			
		// is this chunk from the same world as the lodWorld?
		if (!lodWorld.getWorldName().equals(LodUtils.getCurrentWorldID()))
			// we are not in the same world anymore
			// don't add this LOD
			return;
				
		
		// don't try to create an LOD object
		// if for some reason we aren't
		// given a valid chunk object
		if (chunk == null || !LodUtils.chunkHasBlockData(chunk))
			return;
		
		ServerWorld world = LodUtils.getServerWorldFromDimension(dim);
		if (world == null)
			return;
			
		Thread thread = new Thread(() ->
		{
			try
			{
				LodChunk lod = generateLodFromChunk(chunk, world);
				
				LodDimension lodDim;
				
				if (lodWorld.getLodDimension(dim) == null)
				{
					lodDim = new LodDimension(dim, regionWidth);
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
	 * Creates a LodChunk for a chunk in the given world. <br>
	 * Note: The world is required to determine each block's color
	 * 
	 * @throws IllegalArgumentException 
	 * thrown if either the chunk or world is null.
	 */
	public LodChunk generateLodFromChunk(IChunk chunk, World world) throws IllegalArgumentException
	{
		if(chunk == null)
		{
			throw new IllegalArgumentException("generateLodFromChunk given a null chunk");
		}
		if(world == null)
		{
			throw new IllegalArgumentException("generateLodFromChunk given a null world");
		}
		
		short[] top = new short[4];
		short[] bottom = new short[4];
		Color[] colors = new Color[6];
		
		// generate the top and bottom points of this LOD
		for(LodLocation loc : LodLocation.values())
		{
			top[loc.value] = generateLodCorner(chunk, SectionGenerationMode.GENERATE_TOP, loc);
			bottom[loc.value] = generateLodCorner(chunk, SectionGenerationMode.GENERATE_BOTTOM, loc);
		}
		
		// determine the average color for each direction
		for(ColorDirection dir : ColorDirection.values())
		{
			colors[dir.value] = generateLodColorForDirection(chunk, world, dir);
		}
		
		return new LodChunk(chunk.getPos(), top, bottom, colors);
	}
	
	
	
	
	
	
	//=====================//
	// constructor helpers //
	//=====================//
	
	/** GENERATE_TOP, GENERATE_BOTTOM */
	private enum SectionGenerationMode
	{
		GENERATE_TOP,
		GENERATE_BOTTOM;
	}
	
	/**
	 * Generate the height for the given LodLocation, either the top or bottom.
	 * <br><br>
	 * If invalid/null/empty chunks are given 
	 * crashes may occur.
	 */
	public short generateLodCorner(IChunk chunk, SectionGenerationMode sectionGenMode, LodLocation lodLoc)
	{
		int startX = 0;
		int endX = 0;
		
		int startZ = 0;
		int endZ = 0;
		
		// determine where we should look in this
		// chunk
		switch(lodLoc)
		{
		case NE:
			// -N
			startZ = 0;
			endZ = (CHUNK_DATA_WIDTH / 2) - 1;
			// +E
			startX = CHUNK_DATA_WIDTH / 2;
			endX = CHUNK_DATA_WIDTH - 1;
			break;
			
		case SE:
			// +S
			startZ = CHUNK_DATA_WIDTH / 2;
			endZ = CHUNK_DATA_WIDTH;
			// +E
			startX = CHUNK_DATA_WIDTH / 2;
			endX = CHUNK_DATA_WIDTH;
			break;
			
		case SW:
			// +S
			startZ = CHUNK_DATA_WIDTH / 2;
			endZ = CHUNK_DATA_WIDTH;
			// -W
			startX = 0;
			endX = (CHUNK_DATA_WIDTH / 2) - 1;
			break;
			
		case NW:
			// -N
			startZ = 0;
			endZ = CHUNK_DATA_WIDTH / 2;
			// -W
			startX = 0;
			endX = CHUNK_DATA_WIDTH / 2;
			break;
		}
		
		
		// should have a length of 16
		// (each storage is 16x16x16 and the
		// world height is 256)
		ChunkSection[] chunkSections = chunk.getSections();
		
		
		if(sectionGenMode == SectionGenerationMode.GENERATE_TOP)
			return determineTopPoint(chunkSections, startX, endX, startZ, endZ);
		else
			return determineBottomPoint(chunkSections, startX, endX, startZ, endZ);
	}
	
	
	/**
	 * Find the lowest valid point from the bottom.
	 */
	private short determineBottomPoint(ChunkSection[] chunkSections, int startX, int endX, int startZ, int endZ)
	{
		// search from the bottom up
		for(int i = 0; i < CHUNK_DATA_WIDTH; i++)
		{
			for(int y = 0; y < CHUNK_DATA_HEIGHT; y++)
			{
				
				if(isLayerValidLodPoint(chunkSections, startX, endX, startZ, endZ, i, y))
				{
					// we found
					// enough blocks in this
					// layer to count as an
					// LOD point
					return (short) (y + (i * CHUNK_DATA_HEIGHT));
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
	private short determineBottomPoint(Heightmap heightmap, int startX, int endX, int startZ, int endZ)
	{
		// the heightmap only shows how high the blocks go, it
		// doesn't have any info about how low they go
		return 0;
	}
	
	
	
	/**
	 * Find the highest valid point from the Top
	 */
	private short determineTopPoint(ChunkSection[] chunkSections, int startX, int endX, int startZ, int endZ)
	{
		// search from the top down
		for(int i = chunkSections.length - 1; i >= 0; i--)
		{
			for(int y = CHUNK_DATA_WIDTH - 1; y >= 0; y--)
			{
				if(isLayerValidLodPoint(chunkSections, startX, endX, startZ, endZ, i, y))
				{
					// we found
					// enough blocks in this
					// layer to count as an
					// LOD point
					return (short) (y + (i * CHUNK_DATA_HEIGHT));
				}
			}
		}
		
		// we never found a valid LOD point
		return -1;
	}
	
	/**
	 * Find the highest valid point from the Top
	 */
	@SuppressWarnings("unused")
	private short determineTopPoint(Heightmap heightmap, int startX, int endX, int startZ, int endZ)
	{
		short highest = 0;
		for(int x = startX; x < endX; x++)
		{
			for(int z = startZ; z < endZ; z++)
			{
				short newHeight = (short) heightmap.getHeight(x, z);
				if (newHeight > highest)
					highest = newHeight;
			}
		}
		
		return highest;
	}
	
	
	
	
	
	
	
	
	/**
	 * Is the layer between the given X, Z, and dataIndex
	 * values a valid LOD point?
	 */
	private boolean isLayerValidLodPoint(
			ChunkSection[] chunkSections, 
			int startX, int endX, 
			int startZ, int endZ, 
			int sectionIndex, int y)
	{
		// search through this layer
		int layerBlocks = 0;
		
		for(int x = startX; x < endX; x++)
		{
			for(int z = startZ; z < endZ; z++)
			{
				if(chunkSections[sectionIndex] == null)
				{
					// this section doesn't have any blocks,
					// it is not a valid section
					return false;
				}
				else
				{
					if(chunkSections[sectionIndex].getBlockState(x, y, z) != null && chunkSections[sectionIndex].getBlockState(x, y, z).getBlock() != Blocks.AIR)
					{
						// we found a valid block in
						// in this layer
						layerBlocks++;
						
						if(layerBlocks >= LOD_BLOCK_REQ)
						{
							return true;
						}
					}
				}
				
			} // z
		} // x
		
		return false;
	}
	
	/**
	 * Generate the color of the given ColorDirection at the given chunk
	 * in the given world.
	 */
	private Color  generateLodColorForDirection(IChunk chunk, World world, ColorDirection colorDir)
	{
		Minecraft mc =  Minecraft.getInstance();
		BlockColors bc = mc.getBlockColors();
		
		switch (colorDir)
		{
		case TOP:
			return generateLodColorVertical(chunk, colorDir, world, bc);
		case BOTTOM:
			return generateLodColorVertical(chunk, colorDir, world, bc);
			
		case N:
			return generateLodColorHorizontal(chunk, colorDir, world, bc);
		case S:
			return generateLodColorHorizontal(chunk, colorDir, world, bc);
			
		case E:
			return generateLodColorHorizontal(chunk, colorDir, world, bc);
		case W:
			return generateLodColorHorizontal(chunk, colorDir, world, bc);
		}
		
		return new Color(0, 0, 0, 0);
	}
	
	/**
	 * Generates the color of the top or bottom of a given chunk in the given world.
	 * 
	 * @throws IllegalArgumentException if given a ColorDirection other than TOP or BOTTOM
	 */
	private Color generateLodColorVertical(IChunk chunk, ColorDirection colorDir, World world, BlockColors bc)
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
		
		int topStart = goTopDown? CHUNK_DATA_HEIGHT - 1 : 0;
		int topMax = CHUNK_DATA_HEIGHT;
		int topMin = 0;
		int topIncrement =  goTopDown? -1 : 1;
		
		for(int x = 0; x < CHUNK_DATA_WIDTH; x++)
		{
			for(int z = 0; z < CHUNK_DATA_WIDTH; z++)
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
							
							Color c = intToColor(ci);
							
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
				for(int z = 0; z < CHUNK_DATA_WIDTH; z++)
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
	}
	
	/**
	 * Generates the color of the side of a given chunk in the given world for the given ColorDirection.
	 * 
	 * @throws IllegalArgumentException if given a ColorDirection other than N, S, W, E (North, South, East, West)
	 */
	private Color generateLodColorHorizontal(IChunk chunk, ColorDirection colorDir, World world, BlockColors bc)
	{
		if(colorDir != ColorDirection.N && colorDir != ColorDirection.S && colorDir != ColorDirection.E && colorDir != ColorDirection.W)
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
		case N:
			inStart = 0;
			inIncrement = 1;
			break;
		case S:
			inStart = CHUNK_DATA_WIDTH - 1;
			inIncrement = -1;
			break;
		case E:
			inStart = 0;
			inIncrement = 1;
			break;
		case W:
			inStart = CHUNK_DATA_WIDTH - 1;
			inIncrement = -1;
			break;
		default:
			// we were given an invalid position, return invisible.
			// this shouldn't happen and is mostly here to make the
			// compiler happy
			return new Color(0,0,0,0);
		}
		
		
		for (int i = 0; i < chunkSections.length; i++)
		{
			if (chunkSections[i] != null)
			{
				for (int y = 0; y < CHUNK_DATA_HEIGHT; y++)
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
							case N:
								x = over;
								z = in;
								break;
							case S:
								x = over;
								z = in;
								break;
							case E:
								x = in;
								z = over;
								break;
							case W:
								x = in;
								z = over;
								break;
							default:
								// this will never happen, it would have
								// been caught by the switch before the loops
								break;
							}
							
							int ci;
							ci = chunkSections[i].getBlockState(x, y, z).getMaterial().getColor().colorValue;
							
							if (ci == 0) {
								// skip air or invisible blocks
								continue;
							}
							
							Color c = intToColor(ci);
							
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
	 * Convert a BlockColors int into a Color object.
	 */
	private Color intToColor(int num)
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
	@SuppressWarnings("unused")
	private int colorToInt(Color color)
	{
		return color.getRGB();
	}
	
}
