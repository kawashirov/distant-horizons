package com.backsun.lod.proxy;

import org.lwjgl.opengl.GL11;

import com.backsun.lod.builders.LodBuilder;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.LodRegion;
import com.backsun.lod.objects.LodWorld;
import com.backsun.lod.renderer.LodRenderer;
import com.backsun.lod.renderer.RenderGlobalHook;
import com.backsun.lod.util.LodConfig;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
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
 * @version 03-27-2021
 */
public class ClientProxy
{
	private LodRenderer renderer;
	private LodWorld lodWorld;
	private LodBuilder lodBuilder;
	Minecraft mc = Minecraft.getInstance();
	
	
	public ClientProxy()
	{
		lodBuilder = new LodBuilder();
		renderer = new LodRenderer(lodBuilder);
	}
	
	
	
	
	//==============//
	// render event //
	//==============//
	
	@SubscribeEvent
	public void renderWorldLast(RenderWorldLastEvent event)
	{
		RenderGlobalHook.endRenderingStencil();
		GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0xFF);
		
		if (LodConfig.CLIENT.drawLODs.get())
			renderLods(event.getPartialTicks());
		
		GL11.glDisable(GL11.GL_STENCIL_TEST);
	}
	
	/**
	 * Do any setup that is required to draw LODs
	 * and then tell the LodRenderer to draw.
	 */
	public void renderLods(float partialTicks)
	{
		// update the 
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
		lodWorld = lodBuilder.generateLodChunkAsync(event.getChunk(), event.getWorld().getDimensionType());
	}
	
	
	@SubscribeEvent
	public void worldUnloadEvent(WorldEvent.Unload event)
	{
		// clear the old LodWorld to make sure
		// the correct world is used when changing worlds
		lodWorld = null;
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
			lodWorld = lodBuilder.generateLodChunkAsync(event.getWorld().getChunk(event.getPos()), event.getWorld().getDimensionType());
		}
	}
	
}
