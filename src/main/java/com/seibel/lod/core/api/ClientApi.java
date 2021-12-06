/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
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

package com.seibel.lod.core.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;

/**
 * This holds the methods that should be called
 * by the host mod loader (Fabric, Forge, etc.).
 * Specifically for the client.
 * 
 * @author James Seibel
 * @version 11-12-2021
 */
public class ClientApi
{
	public static final ClientApi INSTANCE = new ClientApi();
	public static final Logger LOGGER = LogManager.getLogger(ModInfo.NAME);
	
	public static LodRenderer renderer = new LodRenderer(ApiShared.lodBufferBuilderFactory);
	
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final EventApi EVENT_API = EventApi.INSTANCE;
	
	/**
	 * there is some setup that should only happen once,
	 * once this is true that setup has completed
	 */
	private boolean firstTimeSetupComplete = false;
	private boolean configOverrideReminderPrinted = false;
	
	
	
	private ClientApi()
	{
		
	}
	
	
	
	
	public void renderLods(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks)
	{
		// comment out when creating a release
		applyConfigOverrides();
		
		// clear any out of date objects
		MC.clearFrameObjectCache();
		
		try
		{
			// only run the first time setup once
			if (!firstTimeSetupComplete)
				firstFrameSetup();
			
			
			if (!MC.playerExists() || ApiShared.lodWorld.getIsWorldNotLoaded())
				return;
			
			LodDimension lodDim = ApiShared.lodWorld.getLodDimension(MC.getCurrentDimension());
			if (lodDim == null)
				return;
			
			DetailDistanceUtil.updateSettings();
			EVENT_API.viewDistanceChangedEvent();
			EVENT_API.playerMoveEvent(lodDim);
			
			lodDim.cutRegionNodesAsync(MC.getPlayerBlockPos().getX(), MC.getPlayerBlockPos().getZ());
			lodDim.expandOrLoadRegionsAsync(MC.getPlayerBlockPos().getX(), MC.getPlayerBlockPos().getZ());
			
			
			
			if (CONFIG.client().advanced().debugging().getDrawLods())
			{
				// Note to self:
				// if "unspecified" shows up in the pie chart, it is
				// possibly because the amount of time between sections
				// is too small for the profiler to measure
				IProfilerWrapper profiler = MC.getProfiler();
				profiler.pop(); // get out of "terrain"
				profiler.push("LOD");
				
				
				ClientApi.renderer.drawLODs(lodDim, mcModelViewMatrix, mcProjectionMatrix, partialTicks, MC.getProfiler());
				
				profiler.pop(); // end LOD
				profiler.push("terrain"); // go back into "terrain"
			}
			
			
			
			// these can't be set until after the buffers are built (in renderer.drawLODs)
			// otherwise the buffers may be set to the wrong size, or not changed at all
			ApiShared.previousChunkRenderDistance = MC_RENDER.getRenderDistance();
			ApiShared.previousLodRenderDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance();
		}
		catch (Exception e)
		{
			ClientApi.LOGGER.error("client proxy: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** used in a development environment to change settings on the fly */
	private void applyConfigOverrides()
	{
		// remind the developer(s) that the config override is active
		if (!configOverrideReminderPrinted)
		{
			MC.sendChatMessage(ModInfo.READABLE_NAME + " experimental build " + ModInfo.VERSION);
			MC.sendChatMessage("You are running a unsupported version of the mod!");
			MC.sendChatMessage("Here be dragons!");
			
			configOverrideReminderPrinted = true;
		}
		
//		CONFIG.client().worldGenerator().setDistanceGenerationMode(DistanceGenerationMode.FULL);
		
//		CONFIG.client().worldGenerator().setGenerationPriority(GenerationPriority.AUTO);		
		
//		CONFIG.client().graphics().advancedGraphics().setGpuUploadMethod(GpuUploadMethod.BUFFER_STORAGE);
//		CONFIG.client().graphics().quality().setLodChunkRenderDistance(128);
		
//		CONFIG.client().graphics().fogQuality().setFogDrawMode(FogDrawMode.FOG_ENABLED);
//		CONFIG.client().graphics().fogQuality().setFogDistance(FogDistance.FAR);
//		CONFIG.client().graphics().fogQuality().setDisableVanillaFog(true);
		
//		CONFIG.client().advanced().buffers().setRebuildTimes(BufferRebuildTimes.FREQUENT);
		
		
		CONFIG.client().advanced().debugging().setDebugKeybindingsEnabled(true);
	}
	
	
	
	
	//=================//
	// Lod maintenance //
	//=================//
	
	/** This event is called once during the first frame Minecraft renders in the world. */
	public void firstFrameSetup()
	{
		// make sure the GLProxy is created before the LodBufferBuilder needs it
		GLProxy.getInstance();
		
		firstTimeSetupComplete = true;
	}
	
	
	
	
	

	
	
	
}
