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

package com.seibel.lod.common;

import com.seibel.lod.common.wrappers.config.ConfigGui;
import com.seibel.lod.core.config.*;
import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.IAdvancedFog;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.IAdvancedFog.IHeightFog;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IMultiplayer;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IWorldGenerator;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced.IDebugging.*;

/**
 * This handles any configuration the user has access to.
 * @author coolGi2007
 * @version 12-12-2021
 */
public class Config extends ConfigGui
//public class Config extends TinyConfig
{
	// CONFIG STRUCTURE
	// 	-> Client
	//		|
	//		|-> Graphics
	//		|		|-> Quality
	//		|		|-> FogQuality
	//		|		|-> AdvancedGraphics
	//		|
	//		|-> World Generation
	//		|
	//		|-> Advanced
	//				|-> Threads
	//				|-> Buffers
	//				|-> Debugging

	// Since the original config system uses forge stuff, that means we have to rewrite the whole config system

	@ConfigAnnotations.ScreenEntry
	public static Client client;

	@ConfigAnnotations.FileComment
	public static String _optionsButton = ILodConfigWrapperSingleton.IClient.OPTIONS_BUTTON_DESC;
	// I know this option should be in Client
	// This is a hacky method to not show the button in the options screen but show it in the mod menu
	// Tough it is in client in the wrapper singleton
	@ConfigAnnotations.Entry
	public static boolean optionsButton = true;

	public static class Client {
		@ConfigAnnotations.ScreenEntry
		public static Graphics graphics;

		@ConfigAnnotations.ScreenEntry
		public static WorldGenerator worldGenerator;

		@ConfigAnnotations.ScreenEntry
		public static Multiplayer multiplayer;

		@ConfigAnnotations.ScreenEntry
		public static Advanced advanced;


		public static class Graphics {
			@ConfigAnnotations.ScreenEntry
			public static Quality quality;

			@ConfigAnnotations.ScreenEntry
			public static FogQuality fogQuality;

			@ConfigAnnotations.ScreenEntry
			public static AdvancedGraphics advancedGraphics;


			public static class Quality {
				@ConfigAnnotations.FileComment
				public static String _drawResolution = IQuality.DRAW_RESOLUTION_DESC;
				@ConfigAnnotations.Entry
				public static HorizontalResolution drawResolution = IQuality.DRAW_RESOLUTION_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _lodChunkRenderDistance = IQuality.LOD_CHUNK_RENDER_DISTANCE_DESC;
				@ConfigAnnotations.Entry(minValue = 16, maxValue = 2048)
				public static int lodChunkRenderDistance = IQuality.LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX.defaultValue;

				@ConfigAnnotations.FileComment
				public static String _verticalQuality = IQuality.VERTICAL_QUALITY_DESC;
				@ConfigAnnotations.Entry
				public static VerticalQuality verticalQuality = IQuality.VERTICAL_QUALITY_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _horizontalScale = IQuality.HORIZONTAL_SCALE_DESC;
				@ConfigAnnotations.Entry(minValue = 2, maxValue = 32)
				public static int horizontalScale = IQuality.HORIZONTAL_SCALE_MIN_DEFAULT_MAX.defaultValue;

				@ConfigAnnotations.FileComment
				public static String _horizontalQuality = IQuality.HORIZONTAL_SCALE_DESC;
				@ConfigAnnotations.Entry
				public static HorizontalQuality horizontalQuality = IQuality.HORIZONTAL_QUALITY_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _dropoffQuality = IQuality.DROPOFF_QUALITY_DESC;
				@ConfigAnnotations.Entry
				public static DropoffQuality dropoffQuality = IQuality.DROPOFF_QUALITY_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _lodBiomeBlending = IQuality.LOD_BIOME_BLENDING_DESC;
				@ConfigAnnotations.Entry(minValue = 0, maxValue = 7)
				public static int lodBiomeBlending = IQuality.LOD_BIOME_BLENDING_MIN_DEFAULT_MAX.defaultValue;
			}


			public static class FogQuality {
				@ConfigAnnotations.FileComment
				public static String _fogDistance = IFogQuality.FOG_DISTANCE_DESC;
				@ConfigAnnotations.Entry
				public static FogDistance fogDistance = IFogQuality.FOG_DISTANCE_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _fogDrawMode = IFogQuality.FOG_DRAW_MODE_DESC;
				@ConfigAnnotations.Entry
				public static FogDrawMode fogDrawMode = IFogQuality.FOG_DRAW_MODE_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _fogColorMode = IFogQuality.FOG_COLOR_MODE_DESC;
				@ConfigAnnotations.Entry
				public static FogColorMode fogColorMode = IFogQuality.FOG_COLOR_MODE_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _disableVanillaFog = IFogQuality.DISABLE_VANILLA_FOG_DESC;
				@ConfigAnnotations.Entry
				public static boolean disableVanillaFog = IFogQuality.DISABLE_VANILLA_FOG_DEFAULT;

				@ConfigAnnotations.ScreenEntry
				public static AdvancedFog advancedFog;

				public static class AdvancedFog {
					static final double SQRT2 = 1.4142135623730951;

					@ConfigAnnotations.FileComment
					public static String _farFogStart = IAdvancedFog.FAR_FOG_START_DESC;
					@ConfigAnnotations.Entry(minValue = 0.0, maxValue = SQRT2)
					public static double farFogStart = IAdvancedFog.FAR_FOG_START_MIN_DEFAULT_MAX.defaultValue;

					@ConfigAnnotations.FileComment
					public static String _farFogEnd = IAdvancedFog.FAR_FOG_END_DESC;
					@ConfigAnnotations.Entry(minValue = 0.0, maxValue = SQRT2)
					public static double farFogEnd = IAdvancedFog.FAR_FOG_END_MIN_DEFAULT_MAX.defaultValue;

					@ConfigAnnotations.FileComment
					public static String _farFogMin = IAdvancedFog.FAR_FOG_MIN_DESC;
					@ConfigAnnotations.Entry(minValue = -5.0, maxValue = SQRT2)
					public static double farFogMin = IAdvancedFog.FAR_FOG_MIN_MIN_DEFAULT_MAX.defaultValue;

					@ConfigAnnotations.FileComment
					public static String _farFogMax = IAdvancedFog.FAR_FOG_MAX_DESC;
					@ConfigAnnotations.Entry(minValue = 0.0, maxValue = 5.0)
					public static double farFogMax = IAdvancedFog.FAR_FOG_MAX_MIN_DEFAULT_MAX.defaultValue;

					@ConfigAnnotations.FileComment
					public static String _farFogType = IAdvancedFog.FAR_FOG_TYPE_DESC;
					@ConfigAnnotations.Entry
					public static FogSetting.FogType farFogType = IAdvancedFog.FAR_FOG_TYPE_DEFAULT;

					@ConfigAnnotations.FileComment
					public static String _farFogDensity = IAdvancedFog.FAR_FOG_DENSITY_DESC;
					@ConfigAnnotations.Entry(minValue = 0.01, maxValue = 50.0)
					public static double farFogDensity = IAdvancedFog.FAR_FOG_DENSITY_MIN_DEFAULT_MAX.defaultValue;

					@ConfigAnnotations.ScreenEntry
					public static HeightFog heightFog;

					public static class HeightFog {

						@ConfigAnnotations.FileComment
						public static String _heightFogMixMode = IHeightFog.HEIGHT_FOG_MIX_MODE_DESC;
						@ConfigAnnotations.Entry
						public static HeightFogMixMode heightFogMixMode = IHeightFog.HEIGHT_FOG_MIX_MODE_DEFAULT;
						@ConfigAnnotations.FileComment
						public static String _heightFogMode = IHeightFog.HEIGHT_FOG_MODE_DESC;
						@ConfigAnnotations.Entry
						public static HeightFogMode heightFogMode = IHeightFog.HEIGHT_FOG_MODE_DEFAULT;

						@ConfigAnnotations.FileComment
						public static String _heightFogHeight = IHeightFog.HEIGHT_FOG_HEIGHT_DESC;
						@ConfigAnnotations.Entry(minValue = -4096.0, maxValue = 4096.0)
						public static double heightFogHeight = IHeightFog.HEIGHT_FOG_HEIGHT_MIN_DEFAULT_MAX.defaultValue;

						@ConfigAnnotations.FileComment
						public static String _heightFogStart = IHeightFog.HEIGHT_FOG_START_DESC;
						@ConfigAnnotations.Entry(minValue = 0.0, maxValue = SQRT2)
						public static double heightFogStart = IHeightFog.HEIGHT_FOG_START_MIN_DEFAULT_MAX.defaultValue;

						@ConfigAnnotations.FileComment
						public static String _heightFogEnd = IHeightFog.HEIGHT_FOG_END_DESC;
						@ConfigAnnotations.Entry(minValue = 0.0, maxValue = SQRT2)
						public static double heightFogEnd = IHeightFog.HEIGHT_FOG_END_MIN_DEFAULT_MAX.defaultValue;

						@ConfigAnnotations.FileComment
						public static String _heightFogMin = IHeightFog.HEIGHT_FOG_MIN_DESC;
						@ConfigAnnotations.Entry(minValue = -5.0, maxValue = SQRT2)
						public static double heightFogMin = IHeightFog.HEIGHT_FOG_MIN_MIN_DEFAULT_MAX.defaultValue;

						@ConfigAnnotations.FileComment
						public static String _heightFogMax = IHeightFog.HEIGHT_FOG_MAX_DESC;
						@ConfigAnnotations.Entry(minValue = 0.0, maxValue = 5.0)
						public static double heightFogMax = IHeightFog.HEIGHT_FOG_MAX_MIN_DEFAULT_MAX.defaultValue;

						@ConfigAnnotations.FileComment
						public static String _heightFogType = IHeightFog.HEIGHT_FOG_TYPE_DESC;
						@ConfigAnnotations.Entry
						public static FogSetting.FogType heightFogType = IHeightFog.HEIGHT_FOG_TYPE_DEFAULT;

						@ConfigAnnotations.FileComment
						public static String _heightFogDensity = IHeightFog.HEIGHT_FOG_DENSITY_DESC;
						@ConfigAnnotations.Entry(minValue = 0.01, maxValue = 50.0)
						public static double heightFogDensity = IHeightFog.HEIGHT_FOG_DENSITY_MIN_DEFAULT_MAX.defaultValue;
					}
				}
			}


			public static class AdvancedGraphics {
				@ConfigAnnotations.FileComment
				public static String _disableDirectionalCulling = IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DESC;
				@ConfigAnnotations.Entry
				public static boolean disableDirectionalCulling = IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _vanillaOverdraw = IAdvancedGraphics.VANILLA_OVERDRAW_DESC;
				@ConfigAnnotations.Entry
				public static VanillaOverdraw vanillaOverdraw = IAdvancedGraphics.VANILLA_OVERDRAW_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _overdrawOffset = IAdvancedGraphics.OVERDRAW_OFFSET_DESC;
				@ConfigAnnotations.Entry(minValue = -16, maxValue = 16)
				public static int overdrawOffset = IAdvancedGraphics.OVERDRAW_OFFSET_MIN_DEFAULT_MAX.defaultValue;

				@ConfigAnnotations.FileComment
				public static String _useExtendedNearClipPlane = IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DESC;
				@ConfigAnnotations.Entry
				public static boolean useExtendedNearClipPlane = IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _brightnessMultiplier = IAdvancedGraphics.BRIGHTNESS_MULTIPLIER_DESC;
				@ConfigAnnotations.Entry
				public static double brightnessMultiplier = IAdvancedGraphics.BRIGHTNESS_MULTIPLIER_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _saturationMultiplier = IAdvancedGraphics.SATURATION_MULTIPLIER_DESC;
				@ConfigAnnotations.Entry
				public static double saturationMultiplier = IAdvancedGraphics.SATURATION_MULTIPLIER_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _enableCaveCulling = IAdvancedGraphics.ENABLE_CAVE_CULLING_DESC;
				@ConfigAnnotations.Entry
				public static boolean enableCaveCulling = IAdvancedGraphics.ENABLE_CAVE_CULLING_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _caveCullingHeight = IAdvancedGraphics.CAVE_CULLING_HEIGHT_DESC;
				@ConfigAnnotations.Entry(minValue = -4096, maxValue = 4096)
				public static int caveCullingHeight = IAdvancedGraphics.CAVE_CULLING_HEIGHT_MIN_DEFAULT_MAX.defaultValue;

				/*
				@ConfigAnnotations.FileComment
				public static String _backsideCullingRange = IAdvancedGraphics.VANILLA_CULLING_RANGE_DESC;
				@ConfigAnnotations.Entry(minValue = 0, maxValue = 512)
				public static int backsideCullingRange = IAdvancedGraphics.VANILLA_CULLING_RANGE_MIN_DEFAULT_MAX.defaultValue;
				*/
			}
		}


		public static class WorldGenerator {
			@ConfigAnnotations.FileComment
			public static String _enableDistantGeneration = IWorldGenerator.ENABLE_DISTANT_GENERATION_DESC;
			@ConfigAnnotations.Entry
			public static boolean enableDistantGeneration = IWorldGenerator.ENABLE_DISTANT_GENERATION_DEFAULT;

			//			@ConfigAnnotations.FileComment
//			public static String _distanceGenerationMode = IWorldGenerator.getDistanceGenerationModeDesc();
			@ConfigAnnotations.Entry
			public static DistanceGenerationMode distanceGenerationMode = IWorldGenerator.DISTANCE_GENERATION_MODE_DEFAULT;

			@ConfigAnnotations.FileComment
			public static String _lightGenerationMode = IWorldGenerator.LIGHT_GENERATION_MODE_DESC;
			@ConfigAnnotations.Entry
			public static LightGenerationMode lightGenerationMode = IWorldGenerator.LIGHT_GENERATION_MODE_DEFAULT;

			@ConfigAnnotations.FileComment
			public static String _generationPriority = IWorldGenerator.GENERATION_PRIORITY_DESC;
			@ConfigAnnotations.Entry
			public static GenerationPriority generationPriority = IWorldGenerator.GENERATION_PRIORITY_DEFAULT;

			/*
			@ConfigAnnotations.FileComment
			public static String _allowUnstableFeatureGeneration = IWorldGenerator.ALLOW_UNSTABLE_FEATURE_GENERATION_DESC;
			// FIXME: Temperary override. In 1.18, the newer Unstable gnerator is more usable
			@ConfigAnnotations.Entry
			public static boolean allowUnstableFeatureGeneration = true;//IWorldGenerator.ALLOW_UNSTABLE_FEATURE_GENERATION_DEFAULT;
			*/

			@ConfigAnnotations.FileComment
			public static String _blocksToAvoid = IWorldGenerator.BLOCKS_TO_AVOID_DESC;
			@ConfigAnnotations.Entry
			public static BlocksToAvoid blocksToAvoid = IWorldGenerator.BLOCKS_TO_AVOID_DEFAULT;
		}


		public static class Multiplayer {
			@ConfigAnnotations.FileComment
			public static String _serverFolderNameMode = IMultiplayer.SERVER_FOLDER_NAME_MODE_DESC;
			@ConfigAnnotations.Entry
			public static ServerFolderNameMode serverFolderNameMode = IMultiplayer.SERVER_FOLDER_NAME_MODE_DEFAULT;

			@ConfigAnnotations.FileComment
			public static String _multiDimensionRequiredSimilarity = IMultiplayer.MULTI_DIMENSION_REQUIRED_SIMILARITY_DESC;
			@ConfigAnnotations.Entry(minValue = 0.0, maxValue = 1.0)
			public static double multiDimensionRequiredSimilarity = IMultiplayer.MULTI_DIMENSION_REQUIRED_SIMILARITY_MIN_DEFAULT_MAX.defaultValue;
		}


		public static class Advanced {
			@ConfigAnnotations.ScreenEntry
			public static Threading threading;

			@ConfigAnnotations.ScreenEntry
			public static Debugging debugging;

			@ConfigAnnotations.ScreenEntry
			public static Buffers buffers;

			@ConfigAnnotations.FileComment
			public static String _lodOnlyMode = IAdvanced.LOD_ONLY_MODE_DESC;
			@ConfigAnnotations.Entry
			public static boolean lodOnlyMode = IAdvanced.LOD_ONLY_MODE_DEFAULT;


			public static class Threading {
				@ConfigAnnotations.FileComment
				public static String _numberOfWorldGenerationThreads = IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DESC;
				@ConfigAnnotations.Entry(minValue = 1, maxValue = 50)
				public static int numberOfWorldGenerationThreads = IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT.defaultValue;

				@ConfigAnnotations.FileComment
				public static String _numberOfBufferBuilderThreads = IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_DESC;
				@ConfigAnnotations.Entry(minValue = 1, maxValue = 50)
				public static int numberOfBufferBuilderThreads = IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX.defaultValue;
			}


			public static class Debugging {
				@ConfigAnnotations.FileComment
				public static String _rendererType = IDebugging.RENDERER_TYPE_DESC;
				@ConfigAnnotations.Entry
				public static RendererType rendererType = IDebugging.RENDERER_TYPE_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _debugMode = IDebugging.DEBUG_MODE_DESC;
				@ConfigAnnotations.Entry
				public static DebugMode debugMode = IDebugging.DEBUG_MODE_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _enableDebugKeybindings = IDebugging.DEBUG_KEYBINDINGS_ENABLED_DESC;
				@ConfigAnnotations.Entry
				public static boolean enableDebugKeybindings = IDebugging.DEBUG_KEYBINDINGS_ENABLED_DEFAULT;
			}


			@ConfigAnnotations.ScreenEntry
			public static DebugSwitch debugSwitch;

			public static class DebugSwitch {
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
				@ConfigAnnotations.FileComment
				public static String _logWorldGenEvent = IDebugSwitch.LOG_WORLDGEN_EVENT_DESC;
				@ConfigAnnotations.Entry
				public static LoggerMode logWorldGenEvent = IDebugSwitch.LOG_WORLDGEN_EVENT_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _logWorldGenPerformance = IDebugSwitch.LOG_WORLDGEN_PERFORMANCE_DESC;
				@ConfigAnnotations.Entry
				public static LoggerMode logWorldGenPerformance = IDebugSwitch.LOG_WORLDGEN_PERFORMANCE_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _logWorldGenLoadEvent = IDebugSwitch.LOG_WORLDGEN_LOAD_EVENT_DESC;
				@ConfigAnnotations.Entry
				public static LoggerMode logWorldGenLoadEvent = IDebugSwitch.LOG_WORLDGEN_LOAD_EVENT_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _logLodBuilderEvent = IDebugSwitch.LOG_LODBUILDER_EVENT_DESC;
				@ConfigAnnotations.Entry
				public static LoggerMode logLodBuilderEvent = IDebugSwitch.LOG_LODBUILDER_EVENT_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _logRendererBufferEvent = IDebugSwitch.LOG_RENDERER_BUFFER_EVENT_DESC;
				@ConfigAnnotations.Entry
				public static LoggerMode logRendererBufferEvent = IDebugSwitch.LOG_RENDERER_BUFFER_EVENT_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _logRendererGLEvent = IDebugSwitch.LOG_RENDERER_GL_EVENT_DESC;
				@ConfigAnnotations.Entry
				public static LoggerMode logRendererGLEvent = IDebugSwitch.LOG_RENDERER_GL_EVENT_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _logFileReadWriteEvent = IDebugSwitch.LOG_FILE_READWRITE_EVENT_DESC;
				@ConfigAnnotations.Entry
				public static LoggerMode logFileReadWriteEvent = IDebugSwitch.LOG_FILE_READWRITE_EVENT_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _logFileSubDimEvent = IDebugSwitch.LOG_FILE_SUB_DIM_EVENT_DESC;
				@ConfigAnnotations.Entry
				public static LoggerMode logFileSubDimEvent = IDebugSwitch.LOG_FILE_SUB_DIM_EVENT_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _logNetworkEvent = IDebugSwitch.LOG_NETWORK_EVENT_DESC;
				@ConfigAnnotations.Entry
				public static LoggerMode logNetworkEvent = IDebugSwitch.LOG_NETWORK_EVENT_DEFAULT;
			}


			public static class Buffers {
				@ConfigAnnotations.FileComment
				public static String _gpuUploadMethod = IBuffers.GPU_UPLOAD_METHOD_DESC;
				@ConfigAnnotations.Entry
				public static GpuUploadMethod gpuUploadMethod = IBuffers.GPU_UPLOAD_METHOD_DEFAULT;

				@ConfigAnnotations.FileComment
				public static String _gpuUploadPerMegabyteInMilliseconds = IBuffers.GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DESC;
				@ConfigAnnotations.Entry(minValue = 0, maxValue = 5000)
				public static int gpuUploadPerMegabyteInMilliseconds = IBuffers.GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DEFAULT.defaultValue;

				@ConfigAnnotations.FileComment
				public static String _rebuildTimes = IBuffers.REBUILD_TIMES_DESC;
				@ConfigAnnotations.Entry
				public static BufferRebuildTimes rebuildTimes = IBuffers.REBUILD_TIMES_DEFAULT;
			}
		}
	}
}
