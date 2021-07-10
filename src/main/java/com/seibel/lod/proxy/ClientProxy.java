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
package com.seibel.lod.proxy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.seibel.lod.builders.LodBufferBuilder;
import com.seibel.lod.builders.LodChunkBuilder;
import com.seibel.lod.builders.worldGeneration.LodChunkGenWorker;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodRegion;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.profiler.IProfiler;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of this program.
 * 
 * @author James_Seibel
 * @version 7-9-2021
 */
public class ClientProxy
{
	public static final Logger LOGGER = LogManager.getLogger("LOD");
	
	private static LodWorld lodWorld = new LodWorld();
	private static LodChunkBuilder lodChunkBuilder = new LodChunkBuilder();
	private static LodBufferBuilder lodBufferBuilder = new LodBufferBuilder(lodChunkBuilder);
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
		int newWidth = Math.max(4, 
				// TODO is this logic good?
				(mc.options.renderDistance * LodChunk.WIDTH * 2 * LodConfig.CLIENT.lodChunkRadiusMultiplier.get()) / LodRegion.SIZE
				);
		if (lodChunkBuilder.regionWidth != newWidth)
		{
			lodWorld.resizeDimensionRegionWidth(newWidth);
			lodChunkBuilder.regionWidth = newWidth;
			
			// skip this frame, hopefully the lodWorld
			// should have everything set up by then
			return;
		}
		
		LodDimension lodDim = lodWorld.getLodDimension(mc.player.level.dimensionType());
		if (lodDim == null)
			return;
		
		
		// offset the regions
		double playerX = mc.player.getX();
		double playerZ = mc.player.getZ();
		
		int xOffset = ((int)playerX / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterX();
		int zOffset = ((int)playerZ / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterZ();
		
		if (xOffset != 0 || zOffset != 0)
		{
			lodDim.move(xOffset, zOffset);
		}
		
		
		// for testing
//		LodConfig.CLIENT.drawLODs.set(true);
//		LodConfig.CLIENT.debugMode.set(false);
		
//		LodConfig.CLIENT.lodDetail.set(LodDetail.DOUBLE);
//		LodConfig.CLIENT.lodChunkRadiusMultiplier.set(12);
//		LodConfig.CLIENT.fogDistance.set(FogDistance.FAR);
//		LodConfig.CLIENT.fogDrawOverride.set(FogDrawOverride.ALWAYS_DRAW_FOG_FANCY);
		
//		LodConfig.CLIENT.distanceGenerationMode.set(DistanceGenerationMode.FEATURES);
//		LodConfig.CLIENT.allowUnstableFeatureGeneration.set(false);
//		LOGGER.info(lodBufferBuilder.numberOfChunksWaitingToGenerate.get());
		
		
		// Note to self:
		// if "unspecified" shows up in the pie chart, it is
		// possibly because the amount of time between sections
		// is too small for the profile to measure
		IProfiler profiler = mc.getProfiler();
		profiler.pop(); // get out of "terrain"
		profiler.push("LOD");
		
		renderer.drawLODs(lodDim, partialTicks, mc.getProfiler());
		
		profiler.pop(); // end LOD
		profiler.push("terrain"); // restart terrain
	}	
	
	
	
	
	//==============//
	// forge events //
	//==============//
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent.Load event)
	{
		lodChunkBuilder.generateLodChunkAsync(event.getChunk(), lodWorld, event.getWorld());
	}
	
	
	@SubscribeEvent
	public void worldLoadEvent(WorldEvent.Load event)
	{
		// the player just loaded a new world/dimension
		lodWorld.selectWorld(LodUtil.getWorldID(event.getWorld()));
		// make sure the correct LODs are being rendered
		// (if this isn't done the previous world's LODs may be drawn)
		renderer.regenerateLODsNextFrame();
	}
	
	@SubscribeEvent
	public void worldUnloadEvent(WorldEvent.Unload event)
	{
		// the player just unloaded a world/dimension
		
		if(mc.getConnection().getLevel() == null)
		{
			// if this isn't done unfinished tasks may be left in the queue
			// preventing new LodChunks form being generated
			LodChunkGenWorker.restartExecuterService();
			
			lodBufferBuilder.numberOfChunksWaitingToGenerate.set(0);
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
			lodChunkBuilder.generateLodChunkAsync(event.getWorld().getChunk(event.getPos()), lodWorld, event.getWorld());
		}
	}
	
	
	
	
	//================//
	// public getters //
	//================//
	
	public static LodWorld getLodWorld()
	{
		return lodWorld;
	}
	
	public static LodChunkBuilder getLodBuilder()
	{
		return lodChunkBuilder;
	}
	
	public static LodRenderer getRenderer()
	{
		return renderer;
	}
}
