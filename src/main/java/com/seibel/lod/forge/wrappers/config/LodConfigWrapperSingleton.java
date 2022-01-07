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

package com.seibel.lod.forge.wrappers.config;

import com.seibel.lod.core.enums.config.BlocksToAvoid;
import com.seibel.lod.core.enums.config.BufferRebuildTimes;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.GenerationPriority;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.config.HorizontalQuality;
import com.seibel.lod.core.enums.config.HorizontalResolution;
import com.seibel.lod.core.enums.config.VanillaOverdraw;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.FogColorMode;
import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawMode;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.forge.ForgeConfig;

/**
 * @author James Seibel
 * @version 11-29-2021
 */
public class LodConfigWrapperSingleton implements ILodConfigWrapperSingleton
{
	public static final LodConfigWrapperSingleton INSTANCE = new LodConfigWrapperSingleton();
	
	
	private static final Client client = new Client();
	@Override
	public IClient client()
	{
		return client;
	}
	
	public static class Client implements IClient
	{
		public final IGraphics graphics;
		public final IWorldGenerator worldGenerator;
		public final IAdvanced advanced;

		@Override
		public boolean getOptionsButton() {
			return ForgeConfig.CLIENT.optionsButton.get();
		}

		@Override
		public void setOptionsButton(boolean newOptionsButton) {
			ForgeConfig.CLIENT.optionsButton.set(newOptionsButton);
		}	

		@Override
		public IGraphics graphics()
		{
			return graphics;
		}
		
		@Override
		public IWorldGenerator worldGenerator()
		{
			return worldGenerator;
		}
		
		@Override
		public IAdvanced advanced()
		{
			return advanced;
		}
		
		
		
		//================//
		// Client Configs //
		//================//
		public Client()
		{
			graphics = new Graphics();
			worldGenerator = new WorldGenerator();
			advanced = new Advanced();
		}
		
		
		//==================//
		// Graphics Configs //
		//==================//
		public static class Graphics implements IGraphics
		{
			public final IQuality quality;
			public final IFogQuality fogQuality;
			public final IAdvancedGraphics advancedGraphics;
			public final ICloudQuality cloudQuality;
			
			

			@Override
			public IQuality quality()
			{
				return quality;
			}

			@Override
			public IFogQuality fogQuality()
			{
				return fogQuality;
			}

			@Override
			public IAdvancedGraphics advancedGraphics()
			{
				return advancedGraphics;
			}

			@Override
			public ICloudQuality cloudQuality()
			{
				return cloudQuality;
			}
			
			Graphics()
			{
				quality = new Quality();
				advancedGraphics = new AdvancedGraphics();
				fogQuality = new FogQuality();
				cloudQuality = new CloudQuality();
			}
			
			
			public static class Quality implements IQuality
			{
				@Override
				public HorizontalResolution getDrawResolution()
				{
					return ForgeConfig.CLIENT.graphics.quality.drawResolution.get();
				}
				@Override
				public void setDrawResolution(HorizontalResolution newHorizontalResolution)
				{
					ForgeConfig.CLIENT.graphics.quality.drawResolution.set(newHorizontalResolution);
				}
				
				@Override
				public int getLodChunkRenderDistance()
				{
					return ForgeConfig.CLIENT.graphics.quality.lodChunkRenderDistance.get();
				}
				@Override
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance)
				{
					ForgeConfig.CLIENT.graphics.quality.lodChunkRenderDistance.set(newLodChunkRenderDistance);
				}
				
				
				@Override
				public VerticalQuality getVerticalQuality()
				{
					return ForgeConfig.CLIENT.graphics.quality.verticalQuality.get();
				}
				@Override
				public void setVerticalQuality(VerticalQuality newVerticalQuality)
				{
					ForgeConfig.CLIENT.graphics.quality.verticalQuality.set(newVerticalQuality);
				}
				
				
				@Override
				public int getHorizontalScale()
				{
					return ForgeConfig.CLIENT.graphics.quality.horizontalScale.get();
				}
				@Override
				public void setHorizontalScale(int newHorizontalScale)
				{
					ForgeConfig.CLIENT.graphics.quality.horizontalScale.set(newHorizontalScale);
				}
				
				
				@Override
				public HorizontalQuality getHorizontalQuality()
				{
					return ForgeConfig.CLIENT.graphics.quality.horizontalQuality.get();
				}
				@Override
				public void setHorizontalQuality(HorizontalQuality newHorizontalQuality)
				{
					ForgeConfig.CLIENT.graphics.quality.horizontalQuality.set(newHorizontalQuality);
				}
			}
			
			
			public static class FogQuality implements IFogQuality
			{
				@Override
				public FogDistance getFogDistance()
				{
					return ForgeConfig.CLIENT.graphics.fogQuality.fogDistance.get();
				}
				@Override
				public void setFogDistance(FogDistance newFogDistance)
				{
					ForgeConfig.CLIENT.graphics.fogQuality.fogDistance.set(newFogDistance);
				}
				
				
				@Override
				public FogDrawMode getFogDrawMode()
				{
					return ForgeConfig.CLIENT.graphics.fogQuality.fogDrawMode.get();
				}
				@Override
				public void setFogDrawMode(FogDrawMode newFogDrawMode)
				{
					ForgeConfig.CLIENT.graphics.fogQuality.fogDrawMode.set(newFogDrawMode);
				}
				
				
				@Override
				public FogColorMode getFogColorMode()
				{
					return ForgeConfig.CLIENT.graphics.fogQuality.fogColorMode.get();
				}
				@Override
				public void setFogColorMode(FogColorMode newFogColorMode)
				{
					ForgeConfig.CLIENT.graphics.fogQuality.fogColorMode.set(newFogColorMode);
				}
				
				
				@Override
				public boolean getDisableVanillaFog()
				{
					return ForgeConfig.CLIENT.graphics.fogQuality.disableVanillaFog.get();
				}
				@Override
				public void setDisableVanillaFog(boolean newDisableVanillaFog)
				{
					ForgeConfig.CLIENT.graphics.fogQuality.disableVanillaFog.set(newDisableVanillaFog);
				}
			}
			
			
			public static class AdvancedGraphics implements IAdvancedGraphics
			{
				@Override
				public boolean getDisableDirectionalCulling()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphics.disableDirectionalCulling.get();
				}
				@Override
				public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphics.disableDirectionalCulling.set(newDisableDirectionalCulling);
				}
				
				
				@Override
				public boolean getAlwaysDrawAtMaxQuality()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphics.alwaysDrawAtMaxQuality.get();
				}
				@Override
				public void setAlwaysDrawAtMaxQuality(boolean newAlwaysDrawAtMaxQuality)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphics.alwaysDrawAtMaxQuality.set(newAlwaysDrawAtMaxQuality);
				}
				
				
				@Override
				public VanillaOverdraw getVanillaOverdraw()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphics.vanillaOverdraw.get();
				}
				@Override
				public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphics.vanillaOverdraw.set(newVanillaOverdraw);
				}
				
				
				@Override
				public boolean getUseExtendedNearClipPlane()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphics.useExtendedNearClipPlane.get();
				}
				@Override
				public void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphics.useExtendedNearClipPlane.set(newUseExtendedNearClipPlane);
				}
				@Override
				public int getBacksideCullingRange() {
					return ForgeConfig.CLIENT.graphics.advancedGraphics.backsideCullingRange.get();
				}
				@Override
				public void setBacksideCullingRange(int backsideCullingRange) {
					ForgeConfig.CLIENT.graphics.advancedGraphics.backsideCullingRange.set(backsideCullingRange);
				}
			}
		
			
			public static class CloudQuality implements ICloudQuality {

				@Override
				public boolean getCustomClouds() {
					return ForgeConfig.CLIENT.graphics.cloudQuality.customClouds.get();
				}

				@Override
				public void setCustomClouds(boolean newCustomClouds) {
					ForgeConfig.CLIENT.graphics.cloudQuality.customClouds.set(newCustomClouds);
				}

				@Override
				public boolean getFabulousClouds() {
					return ForgeConfig.CLIENT.graphics.cloudQuality.fabulousClouds.get();
				}

				@Override
				public void setFabulousClouds(boolean newFabulousClouds) {
					ForgeConfig.CLIENT.graphics.cloudQuality.fabulousClouds.set(newFabulousClouds);
				}

				@Override
				public boolean getExtendClouds() {
					return ForgeConfig.CLIENT.graphics.cloudQuality.extendClouds.get();
				}

				@Override
				public void setExtendClouds(boolean newExtendClouds) {
					ForgeConfig.CLIENT.graphics.cloudQuality.extendClouds.set(newExtendClouds);
				}

				@Override
				public double getCloudHeight() {
					return ForgeConfig.CLIENT.graphics.cloudQuality.cloudHeight.get();
				}

				@Override
				public void setCloudHeight(double newCloudHeight) {
					ForgeConfig.CLIENT.graphics.cloudQuality.cloudHeight.set(newCloudHeight);
				}
				
			}
		}
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		public static class WorldGenerator implements IWorldGenerator
		{
			@Override
			public GenerationPriority getGenerationPriority()
			{
				return ForgeConfig.CLIENT.worldGenerator.generationPriority.get();
			}
			@Override
			public void setGenerationPriority(GenerationPriority newGenerationPriority)
			{
				ForgeConfig.CLIENT.worldGenerator.generationPriority.set(newGenerationPriority);
			}
			
			
			@Override
			public DistanceGenerationMode getDistanceGenerationMode()
			{
				return ForgeConfig.CLIENT.worldGenerator.distanceGenerationMode.get();
			}
			@Override
			public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode)
			{
				ForgeConfig.CLIENT.worldGenerator.distanceGenerationMode.set(newDistanceGenerationMode);
			}
			
			
			@Override
			public boolean getAllowUnstableFeatureGeneration()
			{
				return ForgeConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.get();
			}
			@Override
			public void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration)
			{
				ForgeConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.set(newAllowUnstableFeatureGeneration);
			}
			
			
			@Override
			public BlocksToAvoid getBlocksToAvoid()
			{
				return ForgeConfig.CLIENT.worldGenerator.blocksToAvoid.get();
			}
			@Override
			public void setBlockToAvoid(BlocksToAvoid newBlockToAvoid)
			{
				ForgeConfig.CLIENT.worldGenerator.blocksToAvoid.set(newBlockToAvoid);
			}
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		public static class Advanced implements IAdvanced
		{
			public final IThreading threading;
			public final IDebugging debugging;
			public final IBuffers buffers;
			
			
			@Override
			public IThreading threading()
			{
				return threading;
			}


			@Override
			public IDebugging debugging()
			{
				return debugging;
			}


			@Override
			public IBuffers buffers()
			{
				return buffers;
			}
			
			
			public Advanced()
			{
				threading = new Threading();
				debugging = new Debugging();
				buffers = new Buffers();
			}
			
			public static class Threading implements IThreading
			{
				@Override
				public int getNumberOfWorldGenerationThreads()
				{
					return ForgeConfig.CLIENT.advanced.threading.numberOfWorldGenerationThreads.get();
				}
				@Override
				public void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads)
				{
					ForgeConfig.CLIENT.advanced.threading.numberOfWorldGenerationThreads.set(newNumberOfWorldGenerationThreads);
				}
				
				
				@Override
				public int getNumberOfBufferBuilderThreads()
				{
					return ForgeConfig.CLIENT.advanced.threading.numberOfBufferBuilderThreads.get();
				}
				@Override
				public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads)
				{
					ForgeConfig.CLIENT.advanced.threading.numberOfBufferBuilderThreads.set(newNumberOfWorldBuilderThreads);
				}
			}
			
			
			
			
			//===============//
			// Debug Options //
			//===============//
			public static class Debugging implements IDebugging
			{
				@Override
				public boolean getDrawLods()
				{
					return ForgeConfig.CLIENT.advanced.debugging.drawLods.get();
				}
				@Override
				public void setDrawLods(boolean newDrawLods)
				{
					ForgeConfig.CLIENT.advanced.debugging.drawLods.set(newDrawLods);
				}
				
				
				@Override
				public DebugMode getDebugMode()
				{
					return ForgeConfig.CLIENT.advanced.debugging.debugMode.get();
				}
				@Override
				public void setDebugMode(DebugMode newDebugMode)
				{
					ForgeConfig.CLIENT.advanced.debugging.debugMode.set(newDebugMode);
				}
				
				
				@Override
				public boolean getDebugKeybindingsEnabled()
				{
					return ForgeConfig.CLIENT.advanced.debugging.enableDebugKeybindings.get();
				}
				@Override
				public void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings)
				{
					ForgeConfig.CLIENT.advanced.debugging.enableDebugKeybindings.set(newEnableDebugKeybindings);
				}
			}
			
			
			public static class Buffers implements IBuffers
			{
				@Override
				public GpuUploadMethod getGpuUploadMethod()
				{
					return ForgeConfig.CLIENT.advanced.buffers.gpuUploadMethod.get();
				}
				@Override
				public void setGpuUploadMethod(GpuUploadMethod newDisableVanillaFog)
				{
					ForgeConfig.CLIENT.advanced.buffers.gpuUploadMethod.set(newDisableVanillaFog);
				}
				
				
				@Override
				public int getGpuUploadPerMegabyteInMilliseconds()
				{
					return ForgeConfig.CLIENT.advanced.buffers.gpuUploadPerMegabyteInMilliseconds.get();
				}
				@Override
				public void setGpuUploadPerMegabyteInMilliseconds(int newTimeoutInMilliseconds)
				{
					ForgeConfig.CLIENT.advanced.buffers.gpuUploadPerMegabyteInMilliseconds.set(newTimeoutInMilliseconds);
				}
				
				
				@Override
				public BufferRebuildTimes getRebuildTimes()
				{
					return ForgeConfig.CLIENT.advanced.buffers.rebuildTimes.get();
				}
				@Override
				public void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes)
				{
					ForgeConfig.CLIENT.advanced.buffers.rebuildTimes.set(newBufferRebuildTimes);
				}
			}
		}
	}	
}
