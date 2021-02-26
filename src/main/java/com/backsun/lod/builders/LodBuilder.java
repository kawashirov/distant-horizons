package com.backsun.lod.builders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.backsun.lod.handlers.LodDimensionFileHandler;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.LodWorld;
import com.backsun.lod.util.LodUtils;

import net.minecraft.world.DimensionType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.server.ServerWorld;

/**
 * This object is in charge of creating Lod
 * related objects. 
 * (specifically: Lod World, Dimension, Region, and Chunk objects)
 * 
 * @author James Seibel
 * @version 2-22-2021
 */
public class LodBuilder
{
	private ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor();
	public volatile LodWorld lodWorld;
	
	/** Default size of any LOD regions we use */
	public int regionWidth = 5;
	
	public LodBuilder()
	{
		
	}
	
	
	
	/**
	 * Returns LodWorld so that it can be passed
	 * to the LodRenderer.
	 */
	public LodWorld generateLodChunkAsync(Chunk chunk)
	{
		// don't try to create an LOD object
		// if for some reason we aren't
		// given a valid chunk object
		// (Minecraft often gives back empty
		// or null chunks in this method)
		if (chunk == null || !isValidChunk(chunk))
			return lodWorld;
		
		
		DimensionType dim = chunk.getWorld().getDimensionType();
		ServerWorld world = LodUtils.getServerWorldFromDimension(dim);
		
		
		if (world == null)
			return lodWorld;
			
		Thread thread = new Thread(() ->
		{
			try
			{
				LodChunk lod = new LodChunk(chunk, world);
				LodDimension lodDim;
				
				if (lodWorld == null)
				{
					lodWorld = new LodWorld(LodDimensionFileHandler.getWorldName());
				}
				else
				{
					// if we have a lodWorld make sure 
					// it is for this minecraft world
					if (!lodWorld.worldName.equals(LodDimensionFileHandler.getWorldName()))
					{
						// this lodWorld isn't for this minecraft world
						// delete it so we can get a new one
						lodWorld = null;
						
						// skip this frame
						// we'll get this set up next time
						return;
					}
				}
				
				
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
		
		return lodWorld;
	}
	
	/**
	 * Return whether the given chunk
	 * has any data in it.
	 */
	public boolean isValidChunk(Chunk chunk)
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
}
