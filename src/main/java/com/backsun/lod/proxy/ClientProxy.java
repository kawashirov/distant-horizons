package com.backsun.lod.proxy;

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

//TODO Find a way to replace getIntegratedServer so this mod could be used on non-local worlds.
// Minecraft.getMinecraft().getIntegratedServer()

/**
 * This handles all events sent to the client,
 * and is the starting point for most of this program.
 * 
 * @author James_Seibel
 * @version 03-31-2021
 */
public class ClientProxy
{
	private static LodWorld lodWorld = new LodWorld();
	private static LodBuilder lodBuilder = new LodBuilder();
	private static LodRenderer renderer = new LodRenderer(lodBuilder);
	
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
		// update each regions' width to match the new render distance
		int newWidth = Math.max(4, (mc.gameSettings.renderDistanceChunks * LodChunk.WIDTH * 2) / LodRegion.SIZE);
		if (lodWorld != null && lodBuilder.regionWidth != newWidth)
		{
			lodWorld.resizeDimensionRegionWidth(newWidth);
			lodBuilder.regionWidth = newWidth;
			
			// skip this frame, hopefully the lodWorld
			// should have everything set up by then
			return;
		}
		
		
		if (mc == null || mc.player == null || lodWorld == null)
			return;
		
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
		lodBuilder.generateLodChunkAsync(event.getChunk(), lodWorld, event.getWorld().getDimensionType());
	}
	
	
	@SubscribeEvent
	public void worldLoadEvent(WorldEvent.Load event)
	{
		// update the LodWorld to use the new world the player
		// is loaded
		lodWorld.selectWorld(LodUtils.getCurrentWorldID());
	}
	
	@SubscribeEvent
	public void worldUnloadEvent(WorldEvent.Unload event)
	{
		lodWorld.deselectWorld();
	}
	
	
	@SubscribeEvent
	public void worldChangeEvent(BlockEvent event)
	{
		if (event.getClass() == BlockEvent.BreakEvent.class ||
			event.getClass() == BlockEvent.EntityPlaceEvent.class ||
			event.getClass() == BlockEvent.EntityMultiPlaceEvent.class ||
			event.getClass() == BlockEvent.FluidPlaceBlockEvent.class ||
			event.getClass() == BlockEvent.PortalSpawnEvent.class)
		{
			// recreate the LOD where the blocks were changed
			lodBuilder.generateLodChunkAsync(event.getWorld().getChunk(event.getPos()), lodWorld, event.getWorld().getDimensionType());
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
