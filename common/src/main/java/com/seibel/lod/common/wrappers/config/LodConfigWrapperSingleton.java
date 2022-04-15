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
		public final IMultiplayer multiplayer;
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
		public IMultiplayer multiplayer() {
			return multiplayer;
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
			multiplayer = new Multiplayer();
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

				@Override
				public int getLodBiomeBlending() {
					return Config.Client.Graphics.Quality.lodBiomeBlending;
				}

				@Override
				public void setLodBiomeBlending(int newLodBiomeBlending) {
					ConfigGui.editSingleOption.getEntry("client.graphics.quality.lodBiomeBlending").value = newLodBiomeBlending;
					ConfigGui.editSingleOption.saveOption("client.graphics.quality.lodBiomeBlending");
				}
			}
			
			
			public static class FogQuality implements IFogQuality
			{
				public final IAdvancedFog advancedFog;

				FogQuality()
				{
					advancedFog = new AdvancedFog();
				}

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

				@Override
				public IAdvancedFog advancedFog() {
					return advancedFog;
				}

				public static class AdvancedFog implements IAdvancedFog {
					public final IHeightFog heightFog;

					public AdvancedFog() {
						heightFog = new HeightFog();
					}

					@Override
					public double getFarFogStart() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogStart;
					}
					@Override
					public double getFarFogEnd() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogEnd;
					}
					@Override
					public double getFarFogMin() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogMin;
					}
					@Override
					public double getFarFogMax() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogMax;
					}
					@Override
					public FogSetting.FogType getFarFogType() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogType;
					}
					@Override
					public double getFarFogDensity() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogDensity;
					}

					@Override
					public void setFarFogStart(double newFarFogStart) {
						ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.farFogStart").value = newFarFogStart;
						ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.farFogStart");
					}
					@Override
					public void setFarFogEnd(double newFarFogEnd) {
						ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.farFogEnd").value = newFarFogEnd;
						ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.farFogEnd");
					}
					@Override
					public void setFarFogMin(double newFarFogMin) {
						ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.farFogMin").value = newFarFogMin;
						ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.farFogMin");
					}
					@Override
					public void setFarFogMax(double newFarFogMax) {
						ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.farFogMax").value = newFarFogMax;
						ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.farFogMax");
					}
					@Override
					public void setFarFogType(FogSetting.FogType newFarFogType) {
						ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.farFogType").value = newFarFogType;
						ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.farFogType");
					}
					@Override
					public void setFarFogDensity(double newFarFogDensity) {
						ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.farFogDensity").value = newFarFogDensity;
						ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.farFogDensity");
					}

					@Override
					public IHeightFog heightFog() {
						return heightFog;
					}
					
					public static class HeightFog implements IHeightFog {

						@Override
						public HeightFogMixMode getHeightFogMixMode() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.heightFog.heightFogMixMode;
						}
						@Override
						public HeightFogMode getHeightFogMode() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.heightFog.heightFogMode;
						}
						@Override
						public double getHeightFogHeight() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.heightFog.heightFogHeight;
						}
						@Override
						public double getHeightFogStart() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.heightFog.heightFogStart;
						}
						@Override
						public double getHeightFogEnd() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.heightFog.heightFogEnd;
						}
						@Override
						public double getHeightFogMin() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.heightFog.heightFogMin;
						}
						@Override
						public double getHeightFogMax() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.heightFog.heightFogMax;
						}
						@Override
						public FogSetting.FogType getHeightFogType() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.heightFog.heightFogType;
						}
						@Override
						public double getHeightFogDensity() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.heightFog.heightFogDensity;
						}

						@Override
						public void setHeightFogMixMode(HeightFogMixMode newHeightFogMixMode) {
							ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.heightFog.heightFogMixMode").value = newHeightFogMixMode;
							ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.heightFog.heightFogMixMode");
						}
						@Override
						public void setHeightFogMode(HeightFogMode newHeightFogMode) {
							ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.heightFog.heightFogMode").value = newHeightFogMode;
							ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.heightFog.heightFogMode");
						}
						@Override
						public void setHeightFogHeight(double newHeightFogHeight) {
							ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.heightFog.heightFogHeight").value = newHeightFogHeight;
							ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.heightFog.heightFogHeight");
						}
						@Override
						public void setHeightFogStart(double newHeightFogStart) {
							ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.heightFog.heightFogStart").value = newHeightFogStart;
							ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.heightFog.heightFogStart");
						}
						@Override
						public void setHeightFogEnd(double newHeightFogEnd) {
							ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.heightFog.heightFogEnd").value = newHeightFogEnd;
							ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.heightFog.heightFogEnd");
						}
						@Override
						public void setHeightFogMin(double newHeightFogMin) {
							ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.heightFog.heightFogMin").value = newHeightFogMin;
							ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.heightFog.heightFogMin");
						}
						@Override
						public void setHeightFogMax(double newHeightFogMax) {
							ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.heightFog.heightFogMax").value = newHeightFogMax;
							ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.heightFog.heightFogMax");
						}
						@Override
						public void setHeightFogType(FogSetting.FogType newHeightFogType) {
							ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.heightFog.heightFogType").value = newHeightFogType;
							ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.heightFog.heightFogType");
						}
						@Override
						public void setHeightFogDensity(double newHeightFogDensity) {
							ConfigGui.editSingleOption.getEntry("client.graphics.fogQuality.advancedFog.heightFog.heightFogDensity").value = newHeightFogDensity;
							ConfigGui.editSingleOption.saveOption("client.graphics.fogQuality.advancedFog.heightFog.heightFogDensity");
						}
					}
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

				@Override
				public int getOverdrawOffset() {
					return Config.Client.Graphics.AdvancedGraphics.overdrawOffset;
				}

				@Override
				public void setOverdrawOffset(int newOverdrawOffset) {
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.overdrawOffset").value = newOverdrawOffset;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.overdrawOffset");
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
				
				@Override
				public double getBrightnessMultiplier()
				{
					return Config.Client.Graphics.AdvancedGraphics.brightnessMultiplier;
				}
				@Override
				public void setBrightnessMultiplier(double newBrightnessMultiplier)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.brightnessMultiplier").value = newBrightnessMultiplier;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.brightnessMultiplier");
				}
				
				@Override
				public double getSaturationMultiplier()
				{
					return Config.Client.Graphics.AdvancedGraphics.saturationMultiplier;
				}
				@Override
				public void setSaturationMultiplier(double newSaturationMultiplier)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.saturationMultiplier").value = newSaturationMultiplier;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.saturationMultiplier");
				}

				@Override
				public boolean getEnableCaveCulling() {
					return Config.Client.Graphics.AdvancedGraphics.enableCaveCulling;
				}

				@Override
				public void setEnableCaveCulling(boolean newEnableCaveCulling) {
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.enableCaveCulling").value = newEnableCaveCulling;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.enableCaveCulling");

				}

				@Override
				public int getCaveCullingHeight() {
					return Config.Client.Graphics.AdvancedGraphics.caveCullingHeight;
				}

				@Override
				public void setCaveCullingHeight(int newCaveCullingHeight) {
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.caveCullingHeight").value = newCaveCullingHeight;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.caveCullingHeight");

				}

				@Override
				public int getEarthCurveRatio()
				{
					return Config.Client.Graphics.AdvancedGraphics.earthCurveRatio;
				}
				@Override
				public void setEarthCurveRatio(int newEarthCurveRatio)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.earthCurveRatio").value = newEarthCurveRatio;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.earthCurveRatio");
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
				return (boolean) ConfigGui.editSingleOption.getEntry("client.worldGenerator.enableDistantGeneration").value;
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
		
		
		
		//=====================//
		// Multiplayer Configs //
		//=====================//
		public static class Multiplayer implements IMultiplayer
		{
			@Override
			public ServerFolderNameMode getServerFolderNameMode()
			{
				return Config.Client.Multiplayer.serverFolderNameMode;
			}
			@Override
			public void setServerFolderNameMode(ServerFolderNameMode newServerFolderNameMode)
			{
				ConfigGui.editSingleOption.getEntry("client.multiplayer.serverFolderNameMode").value = newServerFolderNameMode;
				ConfigGui.editSingleOption.saveOption("client.multiplayer.serverFolderNameMode");
			}
			
			@Override
			public double getMultiDimensionRequiredSimilarity()
			{
				return Config.Client.Multiplayer.multiDimensionRequiredSimilarity;
			}
			
			@Override
			public void setMultiDimensionRequiredSimilarity(double newMultiDimensionMinimumSimilarityPercent)
			{
				ConfigGui.editSingleOption.getEntry("client.multiplayer.multiDimensionMinimumSimilarityPercent").value = newMultiDimensionMinimumSimilarityPercent;
				ConfigGui.editSingleOption.saveOption("client.multiplayer.multiDimensionMinimumSimilarityPercent");
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
				public final IDebugSwitch debugSwitch;

				@Override
				public IDebugSwitch debugSwitch()
				{
					return debugSwitch;
				}

				/* RendererType:
				 * DEFAULT
				 * DEBUG
				 * DISABLED
				 * */
				@Override
				public RendererType getRendererType() {
					return (RendererType) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.rendererType").value;
				}
				@Override
				public void setRendererType(RendererType newRenderType) {
					ConfigGui.editSingleOption.getEntry("client.advanced.debugging.rendererType").value = newRenderType;
					ConfigGui.editSingleOption.saveOption("client.advanced.debugging.rendererType");
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

				public Debugging()
				{
					debugSwitch = new DebugSwitch();
				}

				public static class DebugSwitch implements IDebugSwitch {

					/* The logging switches available:
					 * WorldGenEvent
					 * WorldGenPerformance
					 * WorldGenLoadEvent
					 * LodBuilderEvent
					 * RendererBufferEvent
					 * RendererGLEvent
					 * FileReadWriteEvent
					 * FileSubDimEvent
					 * NetworkEvent //NOT IMPL YET
					 */

					@Override
					public LoggerMode getLogWorldGenEvent() {
						return (LoggerMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logWorldGenEvent").value;
					}
					@Override
					public void setLogWorldGenEvent(LoggerMode newLogWorldGenEvent) {
						ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logWorldGenEvent").value = newLogWorldGenEvent;
						ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugSwitch.logWorldGenEvent");
					}

					@Override
					public LoggerMode getLogWorldGenPerformance() {
						return (LoggerMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logWorldGenPerformance").value;
					}
					@Override
					public void setLogWorldGenPerformance(LoggerMode newLogWorldGenPerformance) {
						ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logWorldGenPerformance").value = newLogWorldGenPerformance;
						ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugSwitch.logWorldGenPerformance");
					}

					@Override
					public LoggerMode getLogWorldGenLoadEvent() {
						return (LoggerMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logWorldGenLoadEvent").value;
					}
					@Override
					public void setLogWorldGenLoadEvent(LoggerMode newLogWorldGenLoadEvent) {
						ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logWorldGenLoadEvent").value = newLogWorldGenLoadEvent;
						ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugSwitch.logWorldGenLoadEvent");
					}

					@Override
					public LoggerMode getLogLodBuilderEvent() {
						return (LoggerMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logLodBuilderEvent").value;
					}
					@Override
					public void setLogLodBuilderEvent(LoggerMode newLogLodBuilderEvent) {
						ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logLodBuilderEvent").value = newLogLodBuilderEvent;
						ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugSwitch.logLodBuilderEvent");
					}

					@Override
					public LoggerMode getLogRendererBufferEvent() {
						return (LoggerMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logRendererBufferEvent").value;
					}
					@Override
					public void setLogRendererBufferEvent(LoggerMode newLogRendererBufferEvent) {
						ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logRendererBufferEvent").value = newLogRendererBufferEvent;
						ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugSwitch.logRendererBufferEvent");
					}

					@Override
					public LoggerMode getLogRendererGLEvent() {
						return (LoggerMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logRendererGLEvent").value;
					}
					@Override
					public void setLogRendererGLEvent(LoggerMode newLogRendererGLEvent) {
						ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logRendererGLEvent").value = newLogRendererGLEvent;
						ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugSwitch.logRendererGLEvent");
					}

					@Override
					public LoggerMode getLogFileReadWriteEvent() {
						return (LoggerMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logFileReadWriteEvent").value;
					}
					@Override
					public void setLogFileReadWriteEvent(LoggerMode newLogFileReadWriteEvent) {
						ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logFileReadWriteEvent").value = newLogFileReadWriteEvent;
						ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugSwitch.logFileReadWriteEvent");
					}

					@Override
					public LoggerMode getLogFileSubDimEvent() {
						return (LoggerMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logFileSubDimEvent").value;
					}
					@Override
					public void setLogFileSubDimEvent(LoggerMode newLogFileSubDimEvent) {
						ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logFileSubDimEvent").value = newLogFileSubDimEvent;
						ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugSwitch.logFileSubDimEvent");
					}

					@Override
					public LoggerMode getLogNetworkEvent() {
						return (LoggerMode) ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logNetworkEvent").value;
					}
					@Override
					public void setLogNetworkEvent(LoggerMode newLogNetworkEvent) {
						ConfigGui.editSingleOption.getEntry("client.advanced.debugging.debugSwitch.logNetworkEvent").value = newLogNetworkEvent;
						ConfigGui.editSingleOption.saveOption("client.advanced.debugging.debugSwitch.logNetworkEvent");
					}
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

			@Override
			public boolean getLodOnlyMode() {
				return Config.Client.Advanced.lodOnlyMode;
			}

			@Override
			public void setLodOnlyMode(boolean newLodOnlyMode) {
				ConfigGui.editSingleOption.getEntry("client.advanced.buffers.lodOnlyMode").value = newLodOnlyMode;
				ConfigGui.editSingleOption.saveOption("client.advanced.buffers.lodOnlyMode");

			}
		}
	}
}
