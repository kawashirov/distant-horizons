package com.backsun.lod.proxy;

import com.backsun.lod.builders.LodBufferBuilder;
import com.backsun.lod.builders.LodBuilder;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.LodRegion;
import com.backsun.lod.objects.LodWorld;
import com.backsun.lod.renderer.LodRenderer;
import com.backsun.lod.util.LodUtils;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of this program.
 * 
 * @author James_Seibel
 * @version 05-29-2021
 */
public class ClientProxy
{
	private static LodWorld lodWorld = new LodWorld();
	private static LodBuilder lodBuilder = new LodBuilder();
	private static LodBufferBuilder lodBufferBuilder = new LodBufferBuilder(lodBuilder);
	private static LodRenderer renderer = new LodRenderer(lodBufferBuilder);
	
	Minecraft mc = Minecraft.getInstance();
	
	
	public ClientProxy()
	{
		
	}
	
	
	
	
	//==============//
	// render event //
	//==============//
	
	/**
	 * Do any setup that is required to draw LODs
	 * and then tell the LodRenderer to draw.
	 */
	public void renderLods(float partialTicks)
	{
		if (mc == null || mc.player == null || !lodWorld.getIsWorldLoaded())
			return;
		
		// update each regions' width to match the new render distance
		int newWidth = Math.max(4, (mc.gameSettings.renderDistanceChunks * LodChunk.WIDTH * 2) / LodRegion.SIZE);
		if (lodBuilder.regionWidth != newWidth)
		{
			lodWorld.resizeDimensionRegionWidth(newWidth);
			lodBuilder.regionWidth = newWidth;
			
			// skip this frame, hopefully the lodWorld
			// should have everything set up by then
			return;
		}
		
		LodDimension lodDim = lodWorld.getLodDimension(mc.player.world.getDimensionType());
		if (lodDim == null)
			return;
		
		
		// offset the regions
		double playerX = mc.player.getPosX();
		double playerZ = mc.player.getPosZ();
		
		int xOffset = ((int)playerX / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterX();
		int zOffset = ((int)playerZ / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterZ();
		
		if (xOffset != 0 || zOffset != 0)
		{
			lodDim.move(xOffset, zOffset);
		}
		
		
		
		renderer.drawLODs(lodDim, partialTicks, mc.getProfiler());
	}	
	
	
	
	
	//==============//
	// forge events //
	//==============//
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent.Load event)
	{
		lodBuilder.generateLodChunkAsync(event.getChunk(), lodWorld, event.getWorld());
	}
	
	
	@SubscribeEvent
	public void worldLoadEvent(WorldEvent.Load event)
	{
		// the player just loaded a new world/dimension
		lodWorld.selectWorld(LodUtils.getWorldID(event.getWorld()));
		// make sure the correct LODs are being rendered
		// (if this isn't done the previous world's LODs may be drawn)
		renderer.regenerateLODsNextFrame();
	}
	
	@SubscribeEvent
	public void worldUnloadEvent(WorldEvent.Unload event)
	{
		// the player just unloaded a world/dimension
		
		if(mc.getConnection().getWorld() == null)
		{
			lodBufferBuilder.numberOfChunksWaitingToGenerate = 0;
			// the player has disconnected from a server
			lodWorld.deselectWorld();
		}
	}
	
	
	@SubscribeEvent
	public void blockChangeEvent(BlockEvent event)
	{
		if (event.getClass() == BlockEvent.BreakEvent.class ||
			event.getClass() == BlockEvent.EntityPlaceEvent.class ||
			event.getClass() == BlockEvent.EntityMultiPlaceEvent.class ||
			event.getClass() == BlockEvent.FluidPlaceBlockEvent.class ||
			event.getClass() == BlockEvent.PortalSpawnEvent.class)
		{
			// recreate the LOD where the blocks were changed
			lodBuilder.generateLodChunkAsync(event.getWorld().getChunk(event.getPos()), lodWorld, event.getWorld());
		}
	}
	
	
	
	
	//================//
	// public getters //
	//================//
	
	public static LodWorld getLodWorld()
	{
		return lodWorld;
	}
	
	public static LodBuilder getLodBuilder()
	{
		return lodBuilder;
	}
	
	public static LodRenderer getRenderer()
	{
		return renderer;
	}
}
