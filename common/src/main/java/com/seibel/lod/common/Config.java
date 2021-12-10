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
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IWorldGenerator;

/**
 * This handles any configuration the user has access to.
 * @author coolGi2007
 * @version 12-02-2021
 */
public class Config extends ConfigGui
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

	@ScreenEntry(to = "client")
	public static Client client;


	public static class Client
	{
		@Category("client")
		@ScreenEntry(to = "graphics")
		public static Graphics graphics;

		@Category("client")
		@ScreenEntry(to = "worldGenerator")
		public static WorldGenerator worldGenerator;

		@Category("client")
		@ScreenEntry(to = "advanced")
		public static Advanced advanced;


		public static class Graphics
		{
			@Category("client.graphics")
			@ScreenEntry(to = "quality")
			public static Quality quality;

			@Category("client.graphics")
			@ScreenEntry(to = "fogQuality")
			public static FogQuality fogQuality;

			@Category("client.graphics")
			@ScreenEntry(to = "advancedGraphics")
			public static AdvancedGraphics advancedGraphics;


			public static class Quality
			{
				@Category("client.graphics.quality")
				@Entry
				public static HorizontalResolution drawResolution = IQuality.DRAW_RESOLUTION_DEFAULT;

				@Category("client.graphics.quality")
				@Entry(min = 16, max = 1024)
				public static int lodChunkRenderDistance = IQuality.LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX.defaultValue;

				@Category("client.graphics.quality")
				@Entry
				public static VerticalQuality verticalQuality = IQuality.VERTICAL_QUALITY_DEFAULT;

				@Category("client.graphics.quality")
				@Entry(min = 2, max = 32)
				public static int horizontalScale = IQuality.HORIZONTAL_SCALE_MIN_DEFAULT_MAX.defaultValue;

				@Category("client.graphics.quality")
				@Entry
				public static HorizontalQuality horizontalQuality = IQuality.HORIZONTAL_QUALITY_DEFAULT;
			}


			public static class FogQuality
			{
				@Category("client.graphics.fogQuality")
				@Entry
				public static FogDistance fogDistance = IFogQuality.FOG_DISTANCE_DEFAULT;

				@Category("client.graphics.fogQuality")
				@Entry
				public static FogDrawMode fogDrawMode = IFogQuality.FOG_DRAW_MODE_DEFAULT;

				@Category("client.graphics.fogQuality")
				@Entry
				public static FogColorMode fogColorMode = IFogQuality.FOG_COLOR_MODE_DEFAULT;

				@Category("client.graphics.fogQuality")
				@Entry
				public static boolean disableVanillaFog = IFogQuality.DISABLE_VANILLA_FOG_DEFAULT;
			}


			public static class AdvancedGraphics
			{

				@Category("client.graphics.advancedGraphics")
				@Entry
				public static boolean disableDirectionalCulling = IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DEFAULT;

				@Category("client.graphics.advancedGraphics")
				@Entry
				public static boolean alwaysDrawAtMaxQuality = IAdvancedGraphics.ALWAYS_DRAW_AT_MAD_QUALITY_DEFAULT;

				@Category("client.graphics.advancedGraphics")
				@Entry
				public static VanillaOverdraw vanillaOverdraw = IAdvancedGraphics.VANILLA_OVERDRAW_DEFAULT;

				@Category("client.graphics.advancedGraphics")
				@Entry
				public static boolean useExtendedNearClipPlane = IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT;
			}
		}


		public static class WorldGenerator
		{
			@Category("client.worldGenerator")
			@Entry
			public static GenerationPriority generationPriority = IWorldGenerator.GENERATION_PRIORITY_DEFAULT;

			@Category("client.worldGenerator")
			@Entry
			public static DistanceGenerationMode distanceGenerationMode = IWorldGenerator.DISTANCE_GENERATION_MODE_DEFAULT;

			@Category("client.worldGenerator")
			@Entry
			public static boolean allowUnstableFeatureGeneration = IWorldGenerator.ALLOW_UNSTABLE_FEATURE_GENERATION_DEFAULT;

			@Category("client.worldGenerator")
			@Entry
			public static BlocksToAvoid blocksToAvoid = IWorldGenerator.BLOCKS_TO_AVOID_DEFAULT;

//			public static boolean useExperimentalPreGenLoading = false;
		}

		public static class Advanced
		{
			@Category("client.advanced")
			@ScreenEntry(to = "threading")
			public static Threading threading;

			@Category("client.advanced")
			@ScreenEntry(to = "debugging")
			public static Debugging debugging;

			@Category("client.advanced")
			@ScreenEntry(to = "buffers")
			public static Buffers buffers;


			public static class Threading
			{
				@Category("client.advanced.threading")
				@Entry(min = 1, max = 50)
				public static int numberOfWorldGenerationThreads = IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT.defaultValue;

				@Category("client.advanced.threading")
				@Entry(min = 1, max = 50)
				public static int numberOfBufferBuilderThreads = IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX.defaultValue;
			}


			public static class Debugging
			{
				@Category("client.advanced.debugging")
				@Entry
				public static boolean drawLods = IDebugging.DRAW_LODS_DEFAULT;

				@Category("client.advanced.debugging")
				@Entry
				public static DebugMode debugMode = IDebugging.DEBUG_MODE_DEFAULT;

				@Category("client.advanced.debugging")
				@Entry
				public static boolean enableDebugKeybindings = IDebugging.DEBUG_KEYBINDINGS_ENABLED_DEFAULT;
			}


			public static class Buffers
			{
				@Category("client.advanced.buffers")
				@Entry
				public static GpuUploadMethod gpuUploadMethod = IBuffers.GPU_UPLOAD_METHOD_DEFAULT;

				@Category("client.advanced.buffers")
				@Entry(min = 0, max = 5000)
				public static int gpuUploadTimeoutInMilleseconds = IBuffers.GPU_UPLOAD_TIMEOUT_IN_MILLISECONDS_DEFAULT.defaultValue;

				@Category("client.advanced.buffers")
				@Entry
				public static BufferRebuildTimes rebuildTimes = IBuffers.REBUILD_TIMES_DEFAULT;
			}
		}
	}
}
