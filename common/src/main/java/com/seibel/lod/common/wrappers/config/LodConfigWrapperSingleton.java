package com.seibel.lod.common.wrappers.config;

import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.common.Config;

/**
 * This holds the config defaults and setters/getters
 * that should be hooked into the host mod loader (Fabric, Forge, etc.).
 *
 * @author James Seibel
 * @version 11-16-2021
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


		@Override
		public boolean getOptionsButton()
		{
			return Config.optionsButton;
		}
		@Override
		public void setOptionsButton(boolean newOptionsButton)
		{
			ConfigGui.editSingleOption.getEntry("optionsButton").value = newOptionsButton;
			ConfigGui.editSingleOption.saveOption("optionsButton");
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


			Graphics()
			{
				quality = new Quality();
				fogQuality = new FogQuality();
				advancedGraphics = new AdvancedGraphics();
			}


			public static class Quality implements IQuality
			{
				@Override
				public HorizontalResolution getDrawResolution()
				{
					return Config.Client.Graphics.Quality.drawResolution;
				}
				@Override
				public void setDrawResolution(HorizontalResolution newHorizontalResolution)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.quality.drawResolution").value = newHorizontalResolution;
					ConfigGui.editSingleOption.saveOption("client.graphics.quality.drawResolution");
				}


				@Override
				public int getLodChunkRenderDistance()
				{
					return Config.Client.Graphics.Quality.lodChunkRenderDistance;
				}
				@Override
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.quality.lodChunkRenderDistance").value = newLodChunkRenderDistance;
					ConfigGui.editSingleOption.saveOption("client.graphics.quality.lodChunkRenderDistance");
				}


				@Override
				public VerticalQuality getVerticalQuality()
				{
					return Config.Client.Graphics.Quality.verticalQuality;
				}
				@Override
				public void setVerticalQuality(VerticalQuality newVerticalQuality)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.quality.verticalQuality").value = newVerticalQuality;
					ConfigGui.editSingleOption.saveOption("client.graphics.quality.verticalQuality");
				}


				@Override
				public int getHorizontalScale()
				{
					return Config.Client.Graphics.Quality.horizontalScale;
				}
				@Override
				public void setHorizontalScale(int newHorizontalScale)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.quality.horizontalScale").value = newHorizontalScale;
					ConfigGui.editSingleOption.saveOption("client.graphics.quality.horizontalScale");
				}


				@Override
				public HorizontalQuality getHorizontalQuality()
				{
					return Config.Client.Graphics.Quality.horizontalQuality;
				}
				@Override
				public void setHorizontalQuality(HorizontalQuality newHorizontalQuality)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.quality.horizontalQuality").value = newHorizontalQuality;
					ConfigGui.editSingleOption.saveOption("client.graphics.quality.horizontalQuality");
				}
				
				@Override
				public DropoffQuality getDropoffQuality() {
					return Config.Client.Graphics.Quality.dropoffQuality;
				}
				@Override
				public void setDropoffQuality(DropoffQuality newDropoffQuality) {
					ConfigGui.editSingleOption.getEntry("client.graphics.quality.dropoffQuality").value = newDropoffQuality;
					ConfigGui.editSingleOption.saveOption("client.graphics.quality.dropoffQuality");
				}
			}


			public static class FogQuality implements IFogQuality
			{
				@Override
				public FogDistance getFogDistance()
				{
					return Config.Client.Graphics.FogQuality.fogDistance;
				}
				@Override
				public void setFogDistance(FogDistance newFogDistance)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.fogDistance").value = newFogDistance;
					ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.fogDistance");
				}


				@Override
				public FogDrawMode getFogDrawMode()
				{
					return Config.Client.Graphics.FogQuality.fogDrawMode;
				}

				@Override
				public void setFogDrawMode(FogDrawMode setFogDrawMode)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.fogDrawMode").value = setFogDrawMode;
					ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.fogDrawMode");
				}


				@Override
				public FogColorMode getFogColorMode()
				{
					return Config.Client.Graphics.FogQuality.fogColorMode;
				}

				@Override
				public void setFogColorMode(FogColorMode newFogColorMode)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.fogColorMode").value = newFogColorMode;
					ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.fogColorMode");
				}


				@Override
				public boolean getDisableVanillaFog()
				{
					return Config.Client.Graphics.FogQuality.disableVanillaFog;
				}
				@Override
				public void setDisableVanillaFog(boolean newDisableVanillaFog)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.disableVanillaFog").value = newDisableVanillaFog;
					ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.disableVanillaFog");
				}
			}


			public static class AdvancedGraphics implements IAdvancedGraphics
			{
				@Override
				public boolean getDisableDirectionalCulling()
				{
					return Config.Client.Graphics.AdvancedGraphics.disableDirectionalCulling;
				}
				@Override
				public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.disableDirectionalCulling").value = newDisableDirectionalCulling;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.disableDirectionalCulling");
				}

				@Override
				public VanillaOverdraw getVanillaOverdraw()
				{
					return Config.Client.Graphics.AdvancedGraphics.vanillaOverdraw;
				}
				@Override
				public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.vanillaOverdraw").value = newVanillaOverdraw;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.vanillaOverdraw");
				}
				/*
				@Override
				public int getBacksideCullingRange()
				{
					return Config.Client.Graphics.AdvancedGraphics.backsideCullingRange;
				}
				@Override
				public void setBacksideCullingRange(int newBacksideCullingRange)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.backsideCullingRange").value = newBacksideCullingRange;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.backsideCullingRange");
				}*/
				
				@Override
				public boolean getUseExtendedNearClipPlane()
				{
					return Config.Client.Graphics.AdvancedGraphics.useExtendedNearClipPlane;
				}
				@Override
				public void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.useExtendedNearClipPlane").value = newUseExtendedNearClipPlane;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.useExtendedNearClipPlane");
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
				return Config.Client.WorldGenerator.generationPriority;
			}
			@Override
			public void setGenerationPriority(GenerationPriority newGenerationPriority)
			{
				ConfigGui.editSingleOption.getEntry("client.worldGenerator.generationPriority").value = newGenerationPriority;
				ConfigGui.editSingleOption.saveOption("client.worldGenerator.generationPriority");
			}


			@Override
			public DistanceGenerationMode getDistanceGenerationMode()
			{
				return Config.Client.WorldGenerator.distanceGenerationMode;
			}
			@Override
			public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode)
			{
				ConfigGui.editSingleOption.getEntry("client.worldGenerator.distanceGenerationMode").value = newDistanceGenerationMode;
				ConfigGui.editSingleOption.saveOption("client.worldGenerator.distanceGenerationMode");
			}

			/*
			@Override
			public boolean getAllowUnstableFeatureGeneration()
			{
				return Config.Client.WorldGenerator.allowUnstableFeatureGeneration;
			}
			@Override
			public void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration)
			{
				ConfigGui.editSingleOption.getEntry("client.worldGenerator.allowUnstableFeatureGeneration").value = newAllowUnstableFeatureGeneration;
				ConfigGui.editSingleOption.saveOption("client.worldGenerator.allowUnstableFeatureGeneration");
			}*/


			@Override
			public BlocksToAvoid getBlocksToAvoid()
			{
				return Config.Client.WorldGenerator.blocksToAvoid;
			}
			@Override
			public void setBlockToAvoid(BlocksToAvoid newBlockToAvoid)
			{
				ConfigGui.editSingleOption.getEntry("client.worldGenerator.blocksToAvoid").value = newBlockToAvoid;
				ConfigGui.editSingleOption.saveOption("client.worldGenerator.blocksToAvoid");
			}
			@Override
			public boolean getEnableDistantGeneration()
			{
				return Config.Client.WorldGenerator.enableDistantGeneration;
			}
			@Override
			public void setEnableDistantGeneration(boolean newEnableDistantGeneration)
			{
				ConfigGui.editSingleOption.getEntry("client.worldGenerator.enableDistantGeneration").value = newEnableDistantGeneration;
				ConfigGui.editSingleOption.saveOption("client.worldGenerator.enableDistantGeneration");
			}
			@Override
			public LightGenerationMode getLightGenerationMode()
			{
				return Config.Client.WorldGenerator.lightGenerationMode;
			}
			@Override
			public void setLightGenerationMode(LightGenerationMode newLightGenerationMode)
			{
				ConfigGui.editSingleOption.getEntry("client.worldGenerator.lightGenerationMode").value = newLightGenerationMode;
				ConfigGui.editSingleOption.saveOption("client.worldGenerator.lightGenerationMode");
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
					return Config.Client.Advanced.Threading.numberOfWorldGenerationThreads;
				}
				@Override
				public void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads)
				{
					ConfigGui.editSingleOption.getEntry("client.advanced.threading.numberOfWorldGenerationThreads").value = newNumberOfWorldGenerationThreads;
					ConfigGui.editSingleOption.saveOption("client.advanced.threading.numberOfWorldGenerationThreads");
				}


				@Override
				public int getNumberOfBufferBuilderThreads()
				{
					return Config.Client.Advanced.Threading.numberOfBufferBuilderThreads;
				}
				@Override
				public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads)
				{
					ConfigGui.editSingleOption.getEntry("client.advanced.threading.numberOfBufferBuilderThreads").value = newNumberOfWorldBuilderThreads;
					ConfigGui.editSingleOption.saveOption("client.advanced.threading.numberOfBufferBuilderThreads");
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
					return (boolean) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.drawLods").value;
				}
				@Override
				public void setDrawLods(boolean newDrawLods)
				{
					ConfigGui.editSingleOption.getEntry("client.advanced.debugging.drawLods").value = newDrawLods;
					ConfigGui.editSingleOption.saveOption("client.advanced.debugging.drawLods");
				}


				@Override
				public DebugMode getDebugMode()
				{
					return (DebugMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugMode").value;
				}
				@Override
				public void setDebugMode(DebugMode newDebugMode)
				{
					ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugMode").value = newDebugMode;
					ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugMode");
				}


				@Override
				public boolean getDebugKeybindingsEnabled()
				{
					return (boolean) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.enableDebugKeybindings").value;
				}
				@Override
				public void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings)
				{
					ConfigGui.editSingleOption.getEntry("client.advanced.debugging.enableDebugKeybindings").value = newEnableDebugKeybindings;
					ConfigGui.editSingleOption.saveOption("client.advanced.debugging.enableDebugKeybindings");
				}
			}


			public static class Buffers implements IBuffers
			{

				@Override
				public GpuUploadMethod getGpuUploadMethod()
				{
					return Config.Client.Advanced.Buffers.gpuUploadMethod;
				}
				@Override
				public void setGpuUploadMethod(GpuUploadMethod newDisableVanillaFog)
				{
					ConfigGui.editSingleOption.getEntry("client.advanced.buffers.gpuUploadMethod").value = newDisableVanillaFog;
					ConfigGui.editSingleOption.saveOption("client.advanced.buffers.gpuUploadMethod");
				}


				@Override
				public int getGpuUploadPerMegabyteInMilliseconds()
				{
					return Config.Client.Advanced.Buffers.gpuUploadPerMegabyteInMilliseconds;
				}
				@Override
				public void setGpuUploadPerMegabyteInMilliseconds(int newMilliseconds) {
					ConfigGui.editSingleOption.getEntry("client.advanced.buffers.gpuUploadPerMegabyteInMilliseconds").value = newMilliseconds;
					ConfigGui.editSingleOption.saveOption("client.advanced.buffers.gpuUploadPerMegabyteInMilliseconds");
				}


				@Override
				public BufferRebuildTimes getRebuildTimes()
				{
					return Config.Client.Advanced.Buffers.rebuildTimes;
				}
				@Override
				public void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes)
				{
					ConfigGui.editSingleOption.getEntry("client.advanced.buffers.newBufferRebuildTimes").value = newBufferRebuildTimes;
					ConfigGui.editSingleOption.saveOption("client.advanced.buffers.newBufferRebuildTimes");
				}
			}
		}
	}
}
