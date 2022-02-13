package com.seibel.lod.common.wrappers.config;

import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.Config;

/**
 * This is temporary for testing the new config
 *
 * @author coolGi2007
 * @version 02-13-2022
 */
public class NewLodConfigWrapperSingleton implements ILodConfigWrapperSingleton
{
    public static final NewLodConfigWrapperSingleton INSTANCE = new NewLodConfigWrapperSingleton();


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
            return Config.client.optionsButton.get();
        }
        @Override
        public void setOptionsButton(boolean newOptionsButton)
        {
            Config.client.optionsButton.set(newOptionsButton);
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
                    return Config.Client.Graphics.Quality.drawResolution.get();
                }
                @Override
                public void setDrawResolution(HorizontalResolution newHorizontalResolution)
                {
                    Config.Client.Graphics.Quality.drawResolution.set(newHorizontalResolution);
                }


                @Override
                public int getLodChunkRenderDistance()
                {
                    return Config.Client.Graphics.Quality.lodChunkRenderDistance.get();
                }
                @Override
                public void setLodChunkRenderDistance(int newLodChunkRenderDistance)
                {
                    Config.Client.Graphics.Quality.lodChunkRenderDistance.set(newLodChunkRenderDistance);
                }


                @Override
                public VerticalQuality getVerticalQuality()
                {
                    return Config.Client.Graphics.Quality.verticalQuality.get();
                }
                @Override
                public void setVerticalQuality(VerticalQuality newVerticalQuality)
                {
                    Config.Client.Graphics.Quality.verticalQuality.set(newVerticalQuality);
                }


                @Override
                public int getHorizontalScale()
                {
                    return Config.Client.Graphics.Quality.horizontalScale.get();
                }
                @Override
                public void setHorizontalScale(int newHorizontalScale)
                {
                    Config.Client.Graphics.Quality.horizontalScale.set(newHorizontalScale);
                }


                @Override
                public HorizontalQuality getHorizontalQuality()
                {
                    return Config.Client.Graphics.Quality.horizontalQuality.get();
                }
                @Override
                public void setHorizontalQuality(HorizontalQuality newHorizontalQuality)
                {
                    Config.Client.Graphics.Quality.horizontalQuality.set(newHorizontalQuality);
                }

                @Override
                public DropoffQuality getDropoffQuality() {
                    return Config.Client.Graphics.Quality.dropoffQuality.get();
                }
                @Override
                public void setDropoffQuality(DropoffQuality newDropoffQuality) {
                    Config.Client.Graphics.Quality.dropoffQuality.set(newDropoffQuality);
                }
            }


            public static class FogQuality implements IFogQuality
            {
                @Override
                public FogDistance getFogDistance()
                {
                    return Config.Client.Graphics.FogQuality.fogDistance.get();
                }
                @Override
                public void setFogDistance(FogDistance newFogDistance)
                {
                    Config.Client.Graphics.FogQuality.fogDistance.set(newFogDistance);
                }


                @Override
                public FogDrawMode getFogDrawMode()
                {
                    return Config.Client.Graphics.FogQuality.fogDrawMode.get();
                }

                @Override
                public void setFogDrawMode(FogDrawMode setFogDrawMode)
                {
                    Config.Client.Graphics.FogQuality.fogDrawMode.set(setFogDrawMode);
                }


                @Override
                public FogColorMode getFogColorMode()
                {
                    return Config.Client.Graphics.FogQuality.fogColorMode.get();
                }

                @Override
                public void setFogColorMode(FogColorMode newFogColorMode)
                {
                    Config.Client.Graphics.FogQuality.fogColorMode.set(newFogColorMode);
                }


                @Override
                public boolean getDisableVanillaFog()
                {
                    return Config.Client.Graphics.FogQuality.disableVanillaFog.get();
                }
                @Override
                public void setDisableVanillaFog(boolean newDisableVanillaFog)
                {
                    Config.Client.Graphics.FogQuality.disableVanillaFog.set(newDisableVanillaFog);
                }
            }


            public static class AdvancedGraphics implements IAdvancedGraphics
            {
                @Override
                public boolean getDisableDirectionalCulling()
                {
                    return Config.Client.Graphics.AdvancedGraphics.disableDirectionalCulling.get();
                }
                @Override
                public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling)
                {
                    Config.Client.Graphics.AdvancedGraphics.disableDirectionalCulling.set(newDisableDirectionalCulling);
                }


                @Override
                public VanillaOverdraw getVanillaOverdraw()
                {
                    return Config.Client.Graphics.AdvancedGraphics.vanillaOverdraw.get();
                }
                @Override
                public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw)
                {
                    Config.Client.Graphics.AdvancedGraphics.vanillaOverdraw.set(newVanillaOverdraw);
                }


                @Override
                public boolean getUseExtendedNearClipPlane()
                {
                    return Config.Client.Graphics.AdvancedGraphics.useExtendedNearClipPlane.get();
                }
                @Override
                public void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane)
                {
                    Config.Client.Graphics.AdvancedGraphics.useExtendedNearClipPlane.set(newUseExtendedNearClipPlane);
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
                return Config.Client.WorldGenerator.generationPriority.get();
            }
            @Override
            public void setGenerationPriority(GenerationPriority newGenerationPriority)
            {
                Config.Client.WorldGenerator.generationPriority.set(newGenerationPriority);
            }


            @Override
            public DistanceGenerationMode getDistanceGenerationMode()
            {
                return Config.Client.WorldGenerator.distanceGenerationMode.get();
            }
            @Override
            public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode)
            {
                Config.Client.WorldGenerator.distanceGenerationMode.set(newDistanceGenerationMode);
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
                return Config.Client.WorldGenerator.blocksToAvoid.get();
            }
            @Override
            public void setBlockToAvoid(BlocksToAvoid newBlockToAvoid)
            {
                Config.Client.WorldGenerator.blocksToAvoid.set(newBlockToAvoid);
            }
            @Override
            public boolean getEnableDistantGeneration()
            {
                return Config.Client.WorldGenerator.enableDistantGeneration.get();
            }
            @Override
            public void setEnableDistantGeneration(boolean newEnableDistantGeneration)
            {
                Config.Client.WorldGenerator.enableDistantGeneration.set(newEnableDistantGeneration);
            }
            @Override
            public LightGenerationMode getLightGenerationMode()
            {
                return Config.Client.WorldGenerator.lightGenerationMode.get();
            }
            @Override
            public void setLightGenerationMode(LightGenerationMode newLightGenerationMode)
            {
                Config.Client.WorldGenerator.lightGenerationMode.set(newLightGenerationMode);
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
                    return Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get();
                }
                @Override
                public void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads)
                {
                    Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.set(newNumberOfWorldGenerationThreads);
                }


                @Override
                public int getNumberOfBufferBuilderThreads()
                {
                    return Config.Client.Advanced.Threading.numberOfBufferBuilderThreads.get();
                }
                @Override
                public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads)
                {
                    Config.Client.Advanced.Threading.numberOfBufferBuilderThreads.set(newNumberOfWorldBuilderThreads);
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
                    return Config.Client.Advanced.Debugging.drawLods.get();
                }
                @Override
                public void setDrawLods(boolean newDrawLods)
                {
                    Config.Client.Advanced.Debugging.drawLods.set(newDrawLods);
                }


                @Override
                public DebugMode getDebugMode()
                {
                    return Config.Client.Advanced.Debugging.debugMode.get();
                }
                @Override
                public void setDebugMode(DebugMode newDebugMode)
                {
                    Config.Client.Advanced.Debugging.debugMode.set(newDebugMode);
                }


                @Override
                public boolean getDebugKeybindingsEnabled()
                {
                    return Config.Client.Advanced.Debugging.enableDebugKeybindings.get();
                }
                @Override
                public void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings)
                {
                    Config.Client.Advanced.Debugging.enableDebugKeybindings.set(newEnableDebugKeybindings);
                }
            }


            public static class Buffers implements IBuffers
            {

                @Override
                public GpuUploadMethod getGpuUploadMethod()
                {
                    return Config.Client.Advanced.Buffers.gpuUploadMethod.get();
                }
                @Override
                public void setGpuUploadMethod(GpuUploadMethod newDisableVanillaFog)
                {
                    Config.Client.Advanced.Buffers.gpuUploadMethod.set(newDisableVanillaFog);
                }


                @Override
                public int getGpuUploadPerMegabyteInMilliseconds()
                {
                    return Config.Client.Advanced.Buffers.gpuUploadPerMegabyteInMilliseconds.get();
                }
                @Override
                public void setGpuUploadPerMegabyteInMilliseconds(int newMilliseconds) {
                    Config.Client.Advanced.Buffers.gpuUploadPerMegabyteInMilliseconds.set(newMilliseconds);
                }


                @Override
                public BufferRebuildTimes getRebuildTimes()
                {
                    return Config.Client.Advanced.Buffers.rebuildTimes.get();
                }
                @Override
                public void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes)
                {
                    Config.Client.Advanced.Buffers.rebuildTimes.set(newBufferRebuildTimes);
                }
            }
        }
    }
}
