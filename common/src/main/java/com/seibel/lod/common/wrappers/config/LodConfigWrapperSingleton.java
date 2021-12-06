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
				advancedGraphics = new AdvancedGraphics();
				fogQuality = new FogQuality();
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
					Config.Client.Graphics.Quality.drawResolution = newHorizontalResolution;
				}


				@Override
				public int getLodChunkRenderDistance()
				{
					return Config.Client.Graphics.Quality.lodChunkRenderDistance;
				}
				@Override
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance)
				{
					Config.Client.Graphics.Quality.lodChunkRenderDistance = newLodChunkRenderDistance;
				}


				@Override
				public VerticalQuality getVerticalQuality()
				{
					return Config.Client.Graphics.Quality.verticalQuality;
				}
				@Override
				public void setVerticalQuality(VerticalQuality newVerticalQuality)
				{
					Config.Client.Graphics.Quality.verticalQuality = newVerticalQuality;
				}


				@Override
				public HorizontalScale getHorizontalScale()
				{
					return Config.Client.Graphics.Quality.horizontalScale;
				}
				@Override
				public void setHorizontalScale(HorizontalScale newHorizontalScale)
				{
					Config.Client.Graphics.Quality.horizontalScale = newHorizontalScale;
				}


				@Override
				public HorizontalQuality getHorizontalQuality()
				{
					return Config.Client.Graphics.Quality.horizontalQuality;
				}
				@Override
				public void setHorizontalQuality(HorizontalQuality newHorizontalQuality)
				{
					Config.Client.Graphics.Quality.horizontalQuality = newHorizontalQuality;
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
					Config.Client.Graphics.FogQuality.fogDistance = newFogDistance;
				}


				@Override
				public FogDrawMode getFogDrawMode()
				{
					return Config.Client.Graphics.FogQuality.fogDrawMode;
				}

				@Override
				public void setFogDrawMode(FogDrawMode setFogDrawMode)
				{
					Config.Client.Graphics.FogQuality.fogDrawMode = setFogDrawMode;
				}


				@Override
				public FogColorMode getFogColorMode()
				{
					return Config.Client.Graphics.FogQuality.fogColorMode;
				}

				@Override
				public void setFogColorMode(FogColorMode newFogColorMode)
				{
					Config.Client.Graphics.FogQuality.fogColorMode = newFogColorMode;
				}


				@Override
				public boolean getDisableVanillaFog()
				{
					return Config.Client.Graphics.FogQuality.disableVanillaFog;
				}
				@Override
				public void setDisableVanillaFog(boolean newDisableVanillaFog)
				{
					Config.Client.Graphics.FogQuality.disableVanillaFog = newDisableVanillaFog;
				}
			}


			public static class AdvancedGraphics implements IAdvancedGraphics
			{
				@Override
				public LodTemplate getLodTemplate()
				{
					return Config.Client.Graphics.AdvancedGraphics.lodTemplate;
				}
				@Override
				public void setLodTemplate(LodTemplate newLodTemplate)
				{
					Config.Client.Graphics.AdvancedGraphics.lodTemplate = newLodTemplate;
				}


				@Override
				public boolean getDisableDirectionalCulling()
				{
					return Config.Client.Graphics.AdvancedGraphics.disableDirectionalCulling;
				}
				@Override
				public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling)
				{
					Config.Client.Graphics.AdvancedGraphics.disableDirectionalCulling = newDisableDirectionalCulling;
				}


				@Override
				public boolean getAlwaysDrawAtMaxQuality()
				{
					return Config.Client.Graphics.AdvancedGraphics.alwaysDrawAtMaxQuality;
				}
				@Override
				public void setAlwaysDrawAtMaxQuality(boolean newAlwaysDrawAtMaxQuality)
				{
					Config.Client.Graphics.AdvancedGraphics.alwaysDrawAtMaxQuality = newAlwaysDrawAtMaxQuality;
				}


				@Override
				public VanillaOverdraw getVanillaOverdraw()
				{
					return Config.Client.Graphics.AdvancedGraphics.vanillaOverdraw;
				}
				@Override
				public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw)
				{
					Config.Client.Graphics.AdvancedGraphics.vanillaOverdraw = newVanillaOverdraw;
				}


				@Override
				public boolean getUseExtendedNearClipPlane()
				{
					return Config.Client.Graphics.AdvancedGraphics.useExtendedNearClipPlane;
				}
				@Override
				public void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane)
				{
					Config.Client.Graphics.AdvancedGraphics.useExtendedNearClipPlane = newUseExtendedNearClipPlane;
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
				Config.Client.WorldGenerator.generationPriority = newGenerationPriority;
			}


			@Override
			public DistanceGenerationMode getDistanceGenerationMode()
			{
				return Config.Client.WorldGenerator.distanceGenerationMode;
			}
			@Override
			public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode)
			{
				Config.Client.WorldGenerator.distanceGenerationMode = newDistanceGenerationMode;
			}


			@Override
			public boolean getAllowUnstableFeatureGeneration()
			{
				return Config.Client.WorldGenerator.allowUnstableFeatureGeneration;
			}
			@Override
			public void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration)
			{
				Config.Client.WorldGenerator.allowUnstableFeatureGeneration = newAllowUnstableFeatureGeneration;
			}


			@Override
			public BlocksToAvoid getBlocksToAvoid()
			{
				return Config.Client.WorldGenerator.blocksToAvoid;
			}
			@Override
			public void setBlockToAvoid(BlocksToAvoid newBlockToAvoid)
			{
				Config.Client.WorldGenerator.blocksToAvoid = newBlockToAvoid;
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
					Config.Client.Advanced.Threading.numberOfWorldGenerationThreads = newNumberOfWorldGenerationThreads;
				}


				@Override
				public int getNumberOfBufferBuilderThreads()
				{
					return Config.Client.Advanced.Threading.numberOfBufferBuilderThreads;
				}
				@Override
				public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads)
				{
					Config.Client.Advanced.Threading.numberOfBufferBuilderThreads = newNumberOfWorldBuilderThreads;
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
					return Config.Client.Advanced.Debugging.drawLods;
				}
				@Override
				public void setDrawLods(boolean newDrawLods)
				{
					Config.Client.Advanced.Debugging.drawLods = newDrawLods;
				}


				@Override
				public DebugMode getDebugMode()
				{
					return Config.Client.Advanced.Debugging.debugMode;
				}
				@Override
				public void setDebugMode(DebugMode newDebugMode)
				{
					Config.Client.Advanced.Debugging.debugMode = newDebugMode;
				}


				@Override
				public boolean getDebugKeybindingsEnabled()
				{
					return Config.Client.Advanced.Debugging.enableDebugKeybindings;
				}
				@Override
				public void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings)
				{
					Config.Client.Advanced.Debugging.enableDebugKeybindings = newEnableDebugKeybindings;
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
					Config.Client.Advanced.Buffers.gpuUploadMethod = newDisableVanillaFog;
				}


				@Override
				public int getGpuUploadTimeoutInMilliseconds()
				{
					return Config.Client.Advanced.Buffers.gpuUploadTimeoutInMilleseconds;
				}
				@Override
				public void setGpuUploadTimeoutInMilliseconds(int newTimeoutInMilliseconds) {
					Config.Client.Advanced.Buffers.gpuUploadTimeoutInMilleseconds = newTimeoutInMilliseconds;
				}


				@Override
				public BufferRebuildTimes getRebuildTimes()
				{
					return Config.Client.Advanced.Buffers.rebuildTimes;
				}
				@Override
				public void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes)
				{
					Config.Client.Advanced.Buffers.rebuildTimes = newBufferRebuildTimes;
				}
			}
		}
	}
}
