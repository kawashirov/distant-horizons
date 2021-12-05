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

package com.seibel.lod.forge;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.seibel.lod.forge.wrappers.block.BlockPosWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.config.BlocksToAvoid;
import com.seibel.lod.core.enums.config.BufferRebuildTimes;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.GenerationPriority;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.config.HorizontalQuality;
import com.seibel.lod.core.enums.config.HorizontalResolution;
import com.seibel.lod.core.enums.config.HorizontalScale;
import com.seibel.lod.core.enums.config.LodTemplate;
import com.seibel.lod.core.enums.config.VanillaOverdraw;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.FogColorMode;
import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawMode;
import com.seibel.lod.core.objects.MinDefaultMax;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced.IBuffers;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced.IDebugging;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.IAdvancedGraphics;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.IQuality;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IWorldGenerator;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * This handles any configuration the user has access to.
 * @author Leonardo Amato
 * @author James Seibel
 * @version 12-1-2021
 */
@Mod.EventBusSubscriber
public class ForgeConfig
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
	
	
	
	public static class Client
	{
		public final Graphics graphics;
		public final WorldGenerator worldGenerator;
		public final Advanced advanced;
		
		
		//================//
		// Client Configs //
		//================//
		public Client(ForgeConfigSpec.Builder builder)
		{
			builder.push(this.getClass().getSimpleName());
			{
				graphics = new Graphics(builder);
				worldGenerator = new WorldGenerator(builder);
				advanced = new Advanced(builder);
			}
			builder.pop();
		}
		
		
		//==================//
		// Graphics Configs //
		//==================//
		public static class Graphics
		{
			public final Quality quality;
			public final FogQuality fogQuality;
			public final AdvancedGraphics advancedGraphics;
			
			Graphics(ForgeConfigSpec.Builder builder)
			{
				builder.comment(IGraphics.DESC).push("Graphics");
				{
					quality = new Quality(builder);
					advancedGraphics = new AdvancedGraphics(builder);
					fogQuality = new FogQuality(builder);
				}
				builder.pop();
			}
			
			
			public static class Quality
			{
				public final ForgeConfigSpec.EnumValue<HorizontalResolution> drawResolution;
				public final ForgeConfigSpec.IntValue lodChunkRenderDistance;
				public final ForgeConfigSpec.EnumValue<VerticalQuality> verticalQuality;
				public final ForgeConfigSpec.EnumValue<HorizontalScale> horizontalScale;
				public final ForgeConfigSpec.EnumValue<HorizontalQuality> horizontalQuality;
				public final ForgeConfigSpec.IntValue drawResolutionOffset;
				
				Quality(ForgeConfigSpec.Builder builder)
				{
					builder.comment(IQuality.DESC).push(this.getClass().getSimpleName());
					
					verticalQuality = builder
							.comment("\n\n"
									+ IQuality.VERTICAL_QUALITY_DESC)
							.defineEnum("Vertical Quality", IQuality.VERTICAL_QUALITY_DEFAULT);
					
					horizontalScale = builder
							.comment("\n\n"
									+ IQuality.HORIZONTAL_SCALE_DESC)
							.defineEnum("Horizontal Scale", IQuality.HORIZONTAL_SCALE_DEFAULT);
					
					horizontalQuality = builder
							.comment("\n\n"
									+ IQuality.HORIZONTAL_QUALITY_DESC)
							.defineEnum("Horizontal Quality", IQuality.HORIZONTAL_QUALITY_DEFAULT);
					
					drawResolution = builder
							.comment("\n\n"
									+ IQuality.DRAW_RESOLUTION_DESC)
							.defineEnum("Block size", IQuality.DRAW_RESOLUTION_DEFAULT);
					
					MinDefaultMax<Integer> minDefaultMax = IQuality.DRAW_RESOLUTION_OFFSET_MIN_DEFAULT_MAX;
					drawResolutionOffset = builder
							.comment("\n\n"
									+ IQuality.DRAW_RESOLUTION_OFFSET_DESC)
							.defineInRange("Block size offset", minDefaultMax.defaultValue, minDefaultMax.minValue, minDefaultMax.maxValue);
					
					minDefaultMax = IQuality.LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX;
					lodChunkRenderDistance = builder
							.comment("\n\n"
									+ IQuality.LOD_CHUNK_RENDER_DISTANCE_DESC)
							.defineInRange("Lod Render Distance", minDefaultMax.defaultValue, minDefaultMax.minValue, minDefaultMax.maxValue);
					
					builder.pop();
				}
			}
			
			
			public static class FogQuality
			{
				public final ForgeConfigSpec.EnumValue<FogDistance> fogDistance;
				public final ForgeConfigSpec.EnumValue<FogDrawMode> fogDrawMode;
				public final ForgeConfigSpec.EnumValue<FogColorMode> fogColorMode;
				public final ForgeConfigSpec.BooleanValue disableVanillaFog;
				
				FogQuality(ForgeConfigSpec.Builder builder)
				{
					builder.comment(IFogQuality.DESC).push(this.getClass().getSimpleName());
					
					fogDistance = builder
							.comment("\n\n"
									+ IFogQuality.FOG_DISTANCE_DESC)
							.defineEnum("Fog Distance", IFogQuality.FOG_DISTANCE_DEFAULT);
					
					fogDrawMode = builder
							.comment("\n\n"
									+ IFogQuality.FOG_DRAW_MODE_DESC)
							.defineEnum("Fog Draw Mode", IFogQuality.FOG_DRAW_MODE_DEFAULT);
					
					fogColorMode = builder
							.comment("\n\n"
									+ IFogQuality.FOG_COLOR_MODE_DESC)
							.defineEnum("Fog Color Mode", IFogQuality.FOG_COLOR_MODE_DEFAULT);
					
					disableVanillaFog = builder
							.comment("\n\n"
									+ IFogQuality.DISABLE_VANILLA_FOG_DESC)
							.define("Experimental Disable Vanilla Fog", IFogQuality.DISABLE_VANILLA_FOG_DEFAULT);
					
					builder.pop();
				}
			}
			
			
			public static class AdvancedGraphics
			{
				public final ForgeConfigSpec.EnumValue<LodTemplate> lodTemplate;
				public final ForgeConfigSpec.BooleanValue disableDirectionalCulling;
				public final ForgeConfigSpec.BooleanValue alwaysDrawAtMaxQuality;
				public final ForgeConfigSpec.EnumValue<VanillaOverdraw> vanillaOverdraw;
				public final ForgeConfigSpec.BooleanValue useExtendedNearClipPlane;
				
				AdvancedGraphics(ForgeConfigSpec.Builder builder)
				{
					builder.comment(IAdvancedGraphics.DESC).push(this.getClass().getSimpleName());
					
					lodTemplate = builder
							.comment("\n\n"
									+ IAdvancedGraphics.LOD_TEMPLATE_DESC)
							.defineEnum("LOD Template", IAdvancedGraphics.LOD_TEMPLATE_DEFAULT);
					
					disableDirectionalCulling = builder
							.comment("\n\n"
									+ IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DESC)
							.define("Disable Directional Culling", IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DEFAULT);
					
					alwaysDrawAtMaxQuality = builder
							.comment("\n\n"
									+ IAdvancedGraphics.ALWAYS_DRAW_AT_MAD_QUALITY_DESC)
							.define("Always Use Max Quality", IAdvancedGraphics.ALWAYS_DRAW_AT_MAD_QUALITY_DEFAULT);
					
					vanillaOverdraw = builder
							.comment("\n\n"
									+ IAdvancedGraphics.VANILLA_OVERDRAW_DESC)
							.defineEnum("Vanilla Overdraw", IAdvancedGraphics.VANILLA_OVERDRAW_DEFAULT);
					
					// This is a temporary fix (like vanilla overdraw)
					// hopefully we can remove both once we get individual chunk rendering figured out
					useExtendedNearClipPlane = builder
							.comment("\n\n"
									+ IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DESC)
							.define("Use Extended Near Clip Plane", IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT);
					
					
					builder.pop();
				}
			}
		}
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		public static class WorldGenerator
		{
			public final ForgeConfigSpec.EnumValue<GenerationPriority> generationPriority;
			public final ForgeConfigSpec.EnumValue<DistanceGenerationMode> distanceGenerationMode;
			public final ForgeConfigSpec.BooleanValue allowUnstableFeatureGeneration;
			public final ForgeConfigSpec.EnumValue<BlocksToAvoid> blocksToAvoid;
			//public final ForgeConfigSpec.BooleanValue useExperimentalPreGenLoading;
			
			WorldGenerator(ForgeConfigSpec.Builder builder)
			{
				builder.comment(IWorldGenerator.DESC).push("Generation");
				
				generationPriority = builder
						.comment("\n\n"
								+ IWorldGenerator.GENERATION_PRIORITY_DESC)
						.defineEnum("Generation Priority", IWorldGenerator.GENERATION_PRIORITY_DEFAULT);
				
				distanceGenerationMode = builder
						.comment("\n\n"
								+ IWorldGenerator.DISTANCE_GENERATION_MODE_DESC)
						.defineEnum("Distance Generation Mode", IWorldGenerator.DISTANCE_GENERATION_MODE_DEFAULT);
				
				allowUnstableFeatureGeneration = builder
						.comment("\n\n"
								+ IWorldGenerator.ALLOW_UNSTABLE_FEATURE_GENERATION_DESC)
						.define("Allow Unstable Feature Generation", IWorldGenerator.ALLOW_UNSTABLE_FEATURE_GENERATION_DEFAULT);
				
				blocksToAvoid = builder
						.comment("\n\n"
								+ IWorldGenerator.BLOCKS_TO_AVOID_DESC)
						.defineEnum("Blocks to avoid", IWorldGenerator.BLOCKS_TO_AVOID_DEFAULT);
				
				/*useExperimentalPreGenLoading = builder
						 .comment("\n\n"
								+ " if a chunk has been pre-generated, then the mod would use the real chunk for the \n"
								+ "fake chunk creation. May require a deletion of the lod file to see the result. \n")
						 .define("Use pre-generated chunks", false);*/
				builder.pop();
			}
		}
		
		
		
		
		//==================//
		// Advanced Configs //
		//==================//
		public static class Advanced
		{
			public final Threading threading;
			public final Debugging debugging;
			public final Buffers buffers;
			
			public Advanced(ForgeConfigSpec.Builder builder)
			{
				builder.comment(IAdvanced.DESC).push(this.getClass().getSimpleName());
				{
					threading = new Threading(builder);
					debugging = new Debugging(builder);
					buffers = new Buffers(builder);
				}
				builder.pop();
			}
			
			public static class Threading
			{
				public final ForgeConfigSpec.IntValue numberOfWorldGenerationThreads;
				public final ForgeConfigSpec.IntValue numberOfBufferBuilderThreads;
				
				Threading(ForgeConfigSpec.Builder builder)
				{
					builder.comment(IThreading.DESC).push(this.getClass().getSimpleName());
					
					MinDefaultMax<Integer> minDefaultMax = IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT;
					numberOfWorldGenerationThreads = builder
							.comment("\n\n"
									+ IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DESC)
							.defineInRange("numberOfWorldGenerationThreads", minDefaultMax.defaultValue, minDefaultMax.minValue, minDefaultMax.maxValue);
					
					
					minDefaultMax = IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX;
					numberOfBufferBuilderThreads = builder
							.comment("\n\n"
									+ IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX)
							.defineInRange("numberOfBufferBuilderThreads", minDefaultMax.defaultValue, minDefaultMax.minValue, minDefaultMax.maxValue);
					
					builder.pop();
				}
			}

			
			
			
			//===============//
			// Debug Options //
			//===============//
			public static class Debugging
			{
				public final ForgeConfigSpec.BooleanValue drawLods;
				public final ForgeConfigSpec.EnumValue<DebugMode> debugMode;
				public final ForgeConfigSpec.BooleanValue enableDebugKeybindings;
				
				Debugging(ForgeConfigSpec.Builder builder)
				{
					builder.comment(IDebugging.DESC).push(this.getClass().getSimpleName());
					
					drawLods = builder
							.comment("\n\n"
									+ IDebugging.DRAW_LODS_DESC)
							.define("Enable Rendering", IDebugging.DRAW_LODS_DEFAULT);
					
					debugMode = builder
							.comment("\n\n"
									+ IDebugging.DEBUG_MODE_DESC)
							.defineEnum("Debug Mode", IDebugging.DEBUG_MODE_DEFAULT);
					
					enableDebugKeybindings = builder
							.comment("\n\n"
									+ IDebugging.DEBUG_KEYBINDINGS_ENABLED_DESC)
							.define("Enable Debug Keybinding", IDebugging.DEBUG_KEYBINDINGS_ENABLED_DEFAULT);
					
					builder.pop();
				}
			}
			
			
			public static class Buffers
			{

				public final ForgeConfigSpec.EnumValue<GpuUploadMethod> gpuUploadMethod;
				public final ForgeConfigSpec.IntValue gpuUploadTimeoutInMilleseconds;
				public final ForgeConfigSpec.EnumValue<BufferRebuildTimes> rebuildTimes;
				
				Buffers(ForgeConfigSpec.Builder builder)
				{
					builder.comment(IBuffers.DESC).push(this.getClass().getSimpleName());
					
					gpuUploadMethod = builder
							.comment("\n\n"
									+ IBuffers.GPU_UPLOAD_METHOD_DESC)
							.defineEnum("GPU Upload Method", IBuffers.GPU_UPLOAD_METHOD_DEFAULT);
					
					MinDefaultMax<Integer> minDefaultMax = IBuffers.GPU_UPLOAD_TIMEOUT_IN_MILLISECONDS_DEFAULT;
					gpuUploadTimeoutInMilleseconds = builder
							.comment("\n\n"
									+ IBuffers.GPU_UPLOAD_TIMEOUT_IN_MILLISECONDS_DESC)
							.defineInRange("GPU Upload Timeout in Milleseconds", minDefaultMax.defaultValue, minDefaultMax.minValue, minDefaultMax.maxValue);
					
					
					rebuildTimes = builder
							.comment("\n\n"
									+ IBuffers.REBUILD_TIMES_DESC)
							.defineEnum("rebuildFrequency", IBuffers.REBUILD_TIMES_DEFAULT);
					
					builder.pop();
				}
			}
		}
	}
	
	
	/** {@link Path} to the configuration file of this mod */
	private static final Path CONFIG_PATH = Paths.get("config", ModInfo.NAME + ".toml");
	
	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final Client CLIENT;
	
	static
	{
		final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = specPair.getRight();
		CLIENT = specPair.getLeft();
		CommentedFileConfig clientConfig = CommentedFileConfig.builder(CONFIG_PATH)
				.writingMode(WritingMode.REPLACE)
				.build();
		clientConfig.load();
		clientConfig.save();
		CLIENT_SPEC.setConfig(clientConfig);
	}
	
	@SubscribeEvent
	public static void onLoad(final ModConfig.Loading configEvent)
	{
		LogManager.getLogger().debug(ModInfo.NAME, "Loaded forge config file {}", configEvent.getConfig().getFileName());
	}
	
	@SubscribeEvent
	public static void onFileChange(final ModConfig.Reloading configEvent)
	{
		LogManager.getLogger().debug(ModInfo.NAME, "Forge config just got changed on the file system!");
	}
	
}
