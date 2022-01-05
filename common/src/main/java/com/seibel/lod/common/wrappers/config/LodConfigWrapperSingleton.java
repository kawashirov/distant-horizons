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
			public final ICloudQuality cloudQuality;
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
			public ICloudQuality cloudQuality()
			{
				return cloudQuality;
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
				cloudQuality = new CloudQuality();
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
					Config.Client.Graphics.Quality.drawResolution = newHorizontalResolution;
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
				}


				@Override
				public int getHorizontalScale()
				{
					return Config.Client.Graphics.Quality.horizontalScale;
				}
				@Override
				public void setHorizontalScale(int newHorizontalScale)
				{
					Config.Client.Graphics.Quality.horizontalScale = newHorizontalScale;
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
				}
			}


			public static class CloudQuality implements ICloudQuality
			{
				@Override
				public boolean getCustomClouds()
				{
					return Config.Client.Graphics.CloudQuality.customClouds;
				}
				@Override
				public void setCustomClouds(boolean newCustomClouds)
				{
					Config.Client.Graphics.CloudQuality.customClouds = newCustomClouds;
					ConfigGui.saveToFile();
				}


				@Override
				public boolean getFabulousClouds()
				{
					return Config.Client.Graphics.CloudQuality.fabulousClouds;
				}
				@Override
				public void setFabulousClouds(boolean newFabulousClouds)
				{
					Config.Client.Graphics.CloudQuality.fabulousClouds = newFabulousClouds;
					ConfigGui.saveToFile();
				}


				@Override
				public boolean getExtendClouds()
				{
					return Config.Client.Graphics.CloudQuality.extendClouds;
				}
				@Override
				public void setExtendClouds(boolean newExtendClouds)
				{
					Config.Client.Graphics.CloudQuality.extendClouds = newExtendClouds;
					ConfigGui.saveToFile();
				}


				@Override
				public double getCloudHeight()
				{
					return Config.Client.Graphics.CloudQuality.cloudHeight;
				}
				@Override
				public void setCloudHeight(double newCloudHeight)
				{
					Config.Client.Graphics.CloudQuality.cloudHeight = newCloudHeight;
					ConfigGui.saveToFile();
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
					Config.Client.Graphics.AdvancedGraphics.disableDirectionalCulling = newDisableDirectionalCulling;
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
				}
				
				@Override
				public int getBacksideCullingRange()
				{
					return Config.Client.Graphics.AdvancedGraphics.backsideCullingRange;
				}
				@Override
				public void setBacksideCullingRange(int backsideCullingRange)
				{
					Config.Client.Graphics.AdvancedGraphics.backsideCullingRange = backsideCullingRange;
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
				ConfigGui.saveToFile();
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
				ConfigGui.saveToFile();
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
				ConfigGui.saveToFile();
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
				ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
				}


				@Override
				public int getGpuUploadPerMegabyteInMilliseconds()
				{
					return Config.Client.Advanced.Buffers.gpuUploadPerMegabyteInMilliseconds;
				}
				@Override
				public void setGpuUploadPerMegabyteInMilliseconds(int newMilliseconds) {
					Config.Client.Advanced.Buffers.gpuUploadPerMegabyteInMilliseconds = newMilliseconds;
					ConfigGui.saveToFile();
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
					ConfigGui.saveToFile();
				}
			}
		}
	}
}
