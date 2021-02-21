package com.backsun.lod.proxy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.opengl.GL11;

import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.LodRegion;
import com.backsun.lod.objects.LodWorld;
import com.backsun.lod.renderer.LodRenderer;
import com.backsun.lod.renderer.RenderGlobalHook;
import com.backsun.lod.util.LodConfig;
import com.backsun.lod.util.LodFileHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

//TODO Find a way to replace getIntegratedServer so this mod could be used on non-local worlds.
// Minecraft.getInstance().getIntegratedServer()

/**
 * This is used by the client.
 * 
 * @author James_Seibel
 * @version 01-31-2021
 */
public class ClientProxy
{
	private LodRenderer renderer;
	private LodWorld lodWorld;
	private ExecutorService lodGenThreadPool = Executors.newFixedThreadPool(1);
	Minecraft mc = Minecraft.getInstance();
	
	/** Default size of any LOD regions we use */
	private int regionWidth = 5;
	
	public ClientProxy()
	{
		
	}
	
	
	
	
	//==============//
	// render event //
	//==============//
	
	@SubscribeEvent
	public void renderWorldLast(RenderWorldLastEvent event)
	{
		RenderGlobalHook.endRenderingStencil();
		GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0xFF);
		
		if (LodConfig.COMMON.drawLODs.get())
			renderLods(event.getPartialTicks());
		
		GL11.glDisable(GL11.GL_STENCIL_TEST);
	}
	
	public void renderLods(float partialTicks)
	{
		int newWidth = Math.max(4, (mc.gameSettings.renderDistanceChunks * LodChunk.WIDTH * 2) / LodRegion.SIZE);
		if (lodWorld != null && regionWidth != newWidth)
		{
			lodWorld.resizeDimensionRegionWidth(newWidth);
			regionWidth = newWidth;
			
			// skip this frame, hopefully the lodWorld
			// should have everything set up by then
			return;
		}
		
		if (mc == null || mc.player == null || lodWorld == null)
			return;
		
		LodDimension lodDim = lodWorld.getLodDimension(mc.player.world.getDimensionType());
		if (lodDim == null)
			return;
		
		mc.getProfiler().endSection();
		mc.getProfiler().startSection("LOD");
		
		double playerX = mc.player.getPosX();
		double playerZ = mc.player.getPosZ();
		
		int xOffset = ((int)playerX / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterX();
		int zOffset = ((int)playerZ / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterZ();
		
		if (xOffset != 0 || zOffset != 0)
		{
			lodDim.move(xOffset, zOffset);
		}
		
		
		// we wait to create the renderer until the first frame
		// to make sure that the EntityRenderer has
		// been created, that way we can get the fovModifer
		// method from it through reflection.
		if (renderer == null)
		{
			renderer = new LodRenderer();
		}
		else
		{
			renderer.drawLODs(lodDim, partialTicks, mc.getProfiler());
		}
		
		// end of profiler tracking
		mc.getProfiler().endSection();
	}	
	
	
	
	
	
	
	
	
	//===============//
	// update events //
	//===============//
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent.Load event)
	{
		if (mc != null && event != null)
		{
			World world = mc.world;
			
			if(world != null)
			{
				generateLodChunk((Chunk)event.getChunk());
			}
		}
	}
	
	private void generateLodChunk(Chunk chunk)
	{
		// don't try to create an LOD object
		// if for some reason we aren't
		// given a valid chunk object
		// (Minecraft often gives back empty
		// or null chunks in this method)
		if (chunk == null || chunk.getWorld() == null || !isValidChunk(chunk))
			return;
			
		Thread thread = new Thread(() ->
		{
			try
			{
				DimensionType dim = chunk.getWorldForge().getDimensionType();
				World world = chunk.getWorld();
				LodChunk lod = new LodChunk(chunk, world);
				LodDimension lodDim;
				
				if (lodWorld == null)
				{
					lodWorld = new LodWorld(LodFileHandler.getWorldName());
				}
				else
				{
					// if we have a lodWorld make sure 
					// it is for this minecraft world
					if (!lodWorld.worldName.equals(LodFileHandler.getWorldName()))
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
	}
	
	/**
	 * Return whether the given chunk
	 * has any data in it.
	 */
	private boolean isValidChunk(Chunk chunk)
	{
		ChunkSection[] sections = chunk.getSections();
		
		for(ChunkSection section : sections)
		{
			if(section != null && !section.isEmpty())
			{
				return true;
			}
		}
		
		return false;
	}
	
	
}
