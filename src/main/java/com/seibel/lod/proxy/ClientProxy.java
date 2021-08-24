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
import com.seibel.lod.builders.LodBuilder;
import com.seibel.lod.builders.worldGeneration.LodNodeGenWorker;
import com.seibel.lod.enums.DistanceCalculatorType;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogDrawOverride;
import com.seibel.lod.enums.LodDetail;
import com.seibel.lod.enums.ShadingMode;
import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.LodUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of this program.
 *
 * @author James_Seibel
 * @version 8-17-2021
 */
public class ClientProxy
{
	public static final Logger LOGGER = LogManager.getLogger("LOD");
	
	private static LodWorld lodWorld = new LodWorld();
	private static LodBuilder lodBuilder = new LodBuilder();
	private static LodBufferBuilder lodBufferBuilder = new LodBufferBuilder(lodBuilder);
	private static LodRenderer renderer = new LodRenderer(lodBufferBuilder);
	
	private boolean configOverrideReminderPrinted = false;
	
	Minecraft mc = Minecraft.getInstance();
	
	/** This is used to determine if the LODs should be regenerated */
	public static int previousChunkRenderDistance = 0;
	/** This is used to determine if the LODs should be regenerated */
	public static int previousLodRenderDistance = 0;
	
	/** can be set if we want to recalculate variables related
	 * to the LOD view distance */
	private boolean recalculateWidths = false;

	
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


		applyConfigOverrides();
		viewDistanceChangedEvent();
		
		LodDimension lodDim = lodWorld.getLodDimension(mc.player.level.dimensionType());
		if (lodDim == null)
			return;


		playerMoveEvent(lodDim);
		//System.out.println("memory needed " + lodDim.getMinMemoryNeeded() + " byte");
		//System.out.println(lodDim);

		lodDim.treeCutter((int) mc.player.getX(),(int) mc.player.getZ());
		lodDim.treeGenerator((int) mc.player.getX(),(int) mc.player.getZ());
		// comment out when creating a release
		
		
		// Note to self:
		// if "unspecified" shows up in the pie chart, it is
		// possibly because the amount of time between sections
		// is too small for the profiler to measure
		IProfiler profiler = mc.getProfiler();
		profiler.pop(); // get out of "terrain"
		profiler.push("LOD");
		renderer.drawLODs(lodDim, partialTicks, mc.getProfiler());

		profiler.pop(); // end LOD
		profiler.push("terrain"); // restart "terrain"
		
		
		// these can't be set until after the buffers are built (in renderer.drawLODs)
		// otherwise the buffers may be set to the wrong size, or not changed at all
		previousChunkRenderDistance = mc.options.renderDistance;
		previousLodRenderDistance = LodConfig.CLIENT.lodChunkRenderDistance.get();
	}
	
	
	private void applyConfigOverrides()
	{
		// remind the developer(s). that config override is active
		if (!configOverrideReminderPrinted)
		{
			mc.player.sendMessage(new StringTextComponent("Debug settings enabled!"), mc.player.getUUID());
			configOverrideReminderPrinted = true;
		}
		
		//		LodConfig.CLIENT.drawLODs.set(true);
		LodConfig.CLIENT.debugMode.set(false);
		
		LodConfig.CLIENT.maxDrawDetail.set(LodDetail.FULL);
		LodConfig.CLIENT.maxGenerationDetail.set(LodDetail.FULL);

		LodConfig.CLIENT.fogDistance.set(FogDistance.FAR);
		LodConfig.CLIENT.fogDrawOverride.set(FogDrawOverride.ALWAYS_DRAW_FOG_FANCY);
		LodConfig.CLIENT.shadingMode.set(ShadingMode.DARKEN_SIDES);
		//		LodConfig.CLIENT.brightnessMultiplier.set(1.0);
		//		LodConfig.CLIENT.saturationMultiplier.set(1.0);
		
		LodConfig.CLIENT.distanceGenerationMode.set(DistanceGenerationMode.SURFACE);
		LodConfig.CLIENT.allowUnstableFeatureGeneration.set(false);
		LodConfig.CLIENT.lodChunkRenderDistance.set(128);
		LodConfig.CLIENT.lodDistanceCalculatorType.set(DistanceCalculatorType.LINEAR);
		LodConfig.CLIENT.lodQuality.set(1);
		LodConfig.CLIENT.allowUnstableFeatureGeneration.set(false);
		LodConfig.CLIENT.numberOfWorldGenerationThreads.set(Runtime.getRuntime().availableProcessors());
		
		// has to be set in the config file
		//      LodConfig.CLIENT.numberOfWorldGenerationThreads.set(16);
	}
	
	
	//==============//
	// forge events //
	//==============//
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent.Load event)
	{
		lodBuilder.generateLodNodeAsync(event.getChunk(), lodWorld, event.getWorld(), DistanceGenerationMode.SERVER);
	}
	
	@SubscribeEvent
	public void worldSaveEvent(WorldEvent.Save event)
	{
		if (lodWorld != null)
			lodWorld.saveAllDimensions();
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
		
		if (mc.getConnection().getLevel() == null)
		{
			// if this isn't done unfinished tasks may be left in the queue
			// preventing new LodChunks form being generated
			LodNodeGenWorker.restartExecuterService();
			
			lodBufferBuilder.numberOfChunksWaitingToGenerate.set(0);
			// the player has disconnected from a server
			lodWorld.deselectWorld();
			
			
			// hopefully this should reduce issues related to the buffer builder
			// breaking when changing worlds.
			renderer.destroyBuffers();
			recalculateWidths = true;
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
			lodBuilder.generateLodNodeAsync(event.getWorld().getChunk(event.getPos()), lodWorld, event.getWorld());
		}
	}
	
	
	//==================//
	// frame LOD events //
	//==================//
	
	/**
	 * Re-centers the given LodDimension if it needs to be.
	 */
	private void playerMoveEvent(LodDimension lodDim)
	{
		// make sure the dimension is centered
		RegionPos playerRegionPos = new RegionPos(mc.player.blockPosition());
		RegionPos worldRegionOffset = new RegionPos(playerRegionPos.x - lodDim.getCenterX(), playerRegionPos.z - lodDim.getCenterZ());
		if (worldRegionOffset.x != 0 || worldRegionOffset.z != 0)
		{
			lodWorld.saveAllDimensions();
			lodDim.move(worldRegionOffset);
			LOGGER.info("offset: " + worldRegionOffset.x + "," + worldRegionOffset.z + "\t center: " + lodDim.getCenterX() + "," + lodDim.getCenterZ());
		}
	}
	
	
	/**
	 * Re-sizes all LodDimensions if they needs to be.
	 */
	private void viewDistanceChangedEvent()
	{
		// calculate how wide the dimension(s) should be in regions
		int chunksWide = LodConfig.CLIENT.lodChunkRenderDistance.get() * 2 + 1;
		int newWidth = (int) Math.ceil(chunksWide / (float) LodUtil.REGION_WIDTH_IN_CHUNKS);
		newWidth = (newWidth % 2 == 0) ? (newWidth += 1) : (newWidth += 2); // make sure we have a odd number of regions
		
		// do the dimensions need to change in size?
		if (lodBuilder.defaultDimensionWidthInRegions != newWidth || recalculateWidths)
		{
			// TODO make this async
			
			// update the dimensions to fit the new width
			lodWorld.resizeDimensionRegionWidth(newWidth);
			lodBuilder.defaultDimensionWidthInRegions = newWidth;
			renderer.setupBuffers(newWidth);
			
			recalculateWidths = false;
			//LOGGER.info("new dimension width in regions: " + newWidth + "\t potential: " + newWidth );
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
