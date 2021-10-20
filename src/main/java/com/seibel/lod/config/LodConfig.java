/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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

package com.seibel.lod.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.seibel.lod.ModInfo;
import com.seibel.lod.enums.BlockToAvoid;
import com.seibel.lod.enums.BufferRebuildTimes;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogDrawOverride;
import com.seibel.lod.enums.GenerationPriority;
import com.seibel.lod.enums.HorizontalQuality;
import com.seibel.lod.enums.HorizontalResolution;
import com.seibel.lod.enums.HorizontalScale;
import com.seibel.lod.enums.LodTemplate;
import com.seibel.lod.enums.VanillaOverdraw;
import com.seibel.lod.enums.VerticalQuality;
import com.seibel.lod.util.LodUtil;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * This handles any configuration the user has access to.
 * @author James Seibel
 * @version 10-19-2021
 */
@Mod.EventBusSubscriber
public class LodConfig
{
	// CONFIG STRUCTURE
	// 	-> Client
	//		|
	//		|-> Graphics
	//		|		|-> QualityOption
	//		|		|-> FogQualityOption
	//		|		|-> AdvancedGraphicsOption
	//		|
	//		|-> World Generation
	//		|
	//		|-> Advanced Mod Option
	//				|-> Threads
	//				|-> Buffers
	//				|-> Debugging
	
	
	
	public static class Client
	{
		public final Graphics graphics;
		public final WorldGenerator worldGenerator;
		public final AdvancedModOptions advancedModOptions;
		
		
		//================//
		// Client Configs //
		//================//
		public Client(ForgeConfigSpec.Builder builder)
		{
			builder.push(this.getClass().getSimpleName());
			{
				graphics = new Graphics(builder);
				worldGenerator = new WorldGenerator(builder);
				advancedModOptions = new AdvancedModOptions(builder);
			}
			builder.pop();
		}
		
		
		//==================//
		// Graphics Configs //
		//==================//
		public static class Graphics
		{
			
			public final QualityOption qualityOption;
			public final FogQualityOption fogQualityOption;
			public final AdvancedGraphicsOption advancedGraphicsOption;
			
			Graphics(ForgeConfigSpec.Builder builder)
			{
				builder.comment("These settings control how the mod will look in game").push("Graphics");
				{
					qualityOption = new QualityOption(builder);
					advancedGraphicsOption = new AdvancedGraphicsOption(builder);
					fogQualityOption = new FogQualityOption(builder);
				}
				builder.pop();
			}
			
			
			public static class QualityOption
			{
				public final ForgeConfigSpec.EnumValue<HorizontalResolution> drawResolution;
				
				public final ForgeConfigSpec.IntValue lodChunkRenderDistance;
				
				public final ForgeConfigSpec.EnumValue<VerticalQuality> verticalQuality;
				
				public final ForgeConfigSpec.EnumValue<HorizontalScale> horizontalScale;
				
				public final ForgeConfigSpec.EnumValue<HorizontalQuality> horizontalQuality;
				
				
				QualityOption(ForgeConfigSpec.Builder builder)
				{
					builder.comment("These settings control how detailed the fake chunks will be.").push(this.getClass().getSimpleName());
					
					verticalQuality = builder
							.comment("\n\n"
									+ " This indicates how detailed fake chunks will represent \n"
									+ " overhangs, caves, floating islands, ect. \n"
									+ " Higher options will use more memory and increase GPU usage. \n"
									+ " " + VerticalQuality.LOW + ": uses at max 2 columns per position. \n"
									+ " " + VerticalQuality.MEDIUM + ": uses at max 4 columns per position. \n"
									+ " " + VerticalQuality.HIGH + ": uses at max 8 columns per position. \n")
							.defineEnum("Vertical Quality", VerticalQuality.MEDIUM);
					
					horizontalScale = builder
							.comment("\n\n"
									+ " This indicates how quickly fake chunks drop off in quality. \n"
									+ " " + HorizontalScale.LOW + ": quality drops every " + HorizontalScale.LOW.distanceUnit / 16 + " chunks. \n"
									+ " " + HorizontalScale.MEDIUM + ": quality drops every " + HorizontalScale.MEDIUM.distanceUnit / 16 + " chunks. \n"
									+ " " + HorizontalScale.HIGH + ": quality drops every " + HorizontalScale.HIGH.distanceUnit / 16 + " chunks. \n")
							.defineEnum("Horizontal Scale", HorizontalScale.MEDIUM);
					
					horizontalQuality = builder
							.comment("\n\n"
									+ " This indicates the exponential base of the quadratic drop-off \n"
									+ " " + HorizontalQuality.LOWEST + ": base " + HorizontalQuality.LOWEST.quadraticBase + ". \n"
									+ " " + HorizontalQuality.LOW + ": base " + HorizontalQuality.LOW.quadraticBase + ". \n"
									+ " " + HorizontalQuality.MEDIUM + ": base " + HorizontalQuality.MEDIUM.quadraticBase + ". \n"
									+ " " + HorizontalQuality.HIGH + ": base " + HorizontalQuality.HIGH.quadraticBase + ". \n")
							.defineEnum("Horizontal Quality", HorizontalQuality.MEDIUM);
					
					drawResolution = builder
							.comment("\n\n"
									+ " What is the maximum detail fake chunks should be drawn at? \n"
									+ " " + HorizontalResolution.CHUNK + ": render 1 LOD for each Chunk. \n"
									+ " " + HorizontalResolution.HALF_CHUNK + ": render 4 LODs for each Chunk. \n"
									+ " " + HorizontalResolution.FOUR_BLOCKS + ": render 16 LODs for each Chunk. \n"
									+ " " + HorizontalResolution.TWO_BLOCKS + ": render 64 LODs for each Chunk. \n"
									+ " " + HorizontalResolution.BLOCK + ": render 256 LODs for each Chunk. \n")
							.defineEnum("Block size", HorizontalResolution.BLOCK);
					
					lodChunkRenderDistance = builder
							.comment("\n\n"
									+ " The mod's render distance, measured in chunks. \n")
							.defineInRange("Lod Render Distance", 64, 32, 1024);
					
					builder.pop();
				}
			}
			
			
			public static class FogQualityOption
			{
				public final ForgeConfigSpec.EnumValue<FogDistance> fogDistance;
				
				public final ForgeConfigSpec.EnumValue<FogDrawOverride> fogDrawOverride;
				
				FogQualityOption(ForgeConfigSpec.Builder builder)
				{
					
					builder.comment("These settings control the fog quality.").push(this.getClass().getSimpleName());
					
					fogDistance = builder
							.comment("\n\n"
									+ " At what distance should Fog be drawn on the fake chunks? \n"
									+ " If the fog cuts off abruptly or you are using Optifine's \"fast\" fog option \n"
									+ " set this to " + FogDistance.NEAR + " or " + FogDistance.FAR + ". \n")
							.defineEnum("Fog Distance", FogDistance.NEAR_AND_FAR);
					
					fogDrawOverride = builder
							.comment("\n\n"
									+ " When should fog be drawn? \n"
									+ " " + FogDrawOverride.USE_OPTIFINE_FOG_SETTING + ": Use whatever Fog setting Optifine is using. If Optifine isn't installed this defaults to " + FogDrawOverride.ALWAYS_DRAW_FOG_FANCY + ". \n"
									+ " " + FogDrawOverride.NEVER_DRAW_FOG + ": Never draw fog on the LODs \n"
									+ " " + FogDrawOverride.ALWAYS_DRAW_FOG_FAST + ": Always draw fast fog on the LODs \n"
									+ " " + FogDrawOverride.ALWAYS_DRAW_FOG_FANCY + ": Always draw fancy fog on the LODs (if your graphics card supports it) \n")
							.defineEnum("Fog Draw Override", FogDrawOverride.ALWAYS_DRAW_FOG_FANCY);
					builder.pop();
				}
			}
			
			
			public static class AdvancedGraphicsOption
			{
				public final ForgeConfigSpec.EnumValue<LodTemplate> lodTemplate;
				
				public final ForgeConfigSpec.BooleanValue disableDirectionalCulling;
				
				public final ForgeConfigSpec.BooleanValue alwaysDrawAtMaxQuality;
				
				public final ForgeConfigSpec.EnumValue<VanillaOverdraw> vanillaOverdraw;
				
				AdvancedGraphicsOption(ForgeConfigSpec.Builder builder)
				{
					
					builder.comment("Advanced graphics option for the mod").push(this.getClass().getSimpleName());
					
					lodTemplate = builder
							.comment("\n\n"
									+ " How should the LODs be drawn? \n"
									+ " NOTE: Currently only " + LodTemplate.CUBIC + " is implemented! \n"
									+ " \n"
									+ " " + LodTemplate.CUBIC + ": LOD Chunks are drawn as rectangular prisms (boxes). \n"
									+ " " + LodTemplate.TRIANGULAR + ": LOD Chunks smoothly transition between other. \n"
									+ " " + LodTemplate.DYNAMIC + ": LOD Chunks smoothly transition between each other, \n"
									+ " " + "         unless a neighboring chunk is at a significantly different height. \n")
							.defineEnum("LOD Template", LodTemplate.CUBIC);
					
					disableDirectionalCulling = builder
							.comment("\n\n"
									+ " If false fake chunks behind the player's camera \n"
									+ " aren't drawn, increasing performance. \n\n"
									+ ""
									+ " If true all LODs are drawn, even those behind \n"
									+ " the player's camera, decreasing performance. \n\n"
									+ ""
									+ " Disable this if you see LODs disappearing. \n"
									+ " (Which may happen if you are using a camera mod) \n")
							.define("Disable Directional Culling", false);
					
					alwaysDrawAtMaxQuality = builder
							.comment("\n\n"
									+ " Disable quality falloff, \n"
									+ " all fake chunks will be drawn at the highest \n"
									+ " available detail level. \n\n"
									+ " "
									+ " WARNING: \n"
									+ " This could cause a Out Of Memory crash on render \n"
									+ " distances higher than 128 \n")
							.define("Always Use Max Quality", false);
					
					vanillaOverdraw = builder
							.comment("\n\n"
									+ " How often should LODs be drawn on top of regular chunks? \n"
									+ " HALF and ALWAYS will prevent holes in the world, but may look odd for transparent blocks or in caves. \n\n"
									+ " " + VanillaOverdraw.NEVER + ": LODs won't render on top of vanilla chunks. \n"
									+ " " + VanillaOverdraw.DYNAMIC + ": LODs will render on top of distant vanilla chunks to hide delayed loading. \n"
									+ " " + "     More effective on higher render distances. \n"
									+ " " + "     For vanilla render distances less than or equal to " + LodUtil.MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW + " \n"
									+ " " + "     " + VanillaOverdraw.NEVER + " or " + VanillaOverdraw.ALWAYS + " may be used depending on the dimension. \n"
									+ " " + VanillaOverdraw.ALWAYS + ": LODs will render on all vanilla chunks preventing holes in the world. \n")
							.defineEnum("Vanilla Overdraw", VanillaOverdraw.DYNAMIC);
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
			public final ForgeConfigSpec.EnumValue<BlockToAvoid> blockToAvoid;
			//public final ForgeConfigSpec.BooleanValue useExperimentalPreGenLoading;
			
			WorldGenerator(ForgeConfigSpec.Builder builder)
			{
				builder.comment("These settings control how fake chunks outside your normal view range are generated.").push("Generation");
				
				generationPriority = builder
						.comment("\n\n"
								+ " " + GenerationPriority.FAR_FIRST + " \n"
								+ " LODs are generated from low to high detail \n"
								+ " with a small priority for far away regions. \n"
								+ " This fills in the world fastest. \n\n"
								+ ""
								+ " " + GenerationPriority.NEAR_FIRST + " \n"
								+ " LODs are generated around the player \n"
								+ " in a spiral, similar to vanilla minecraft. \n")
						.defineEnum("Generation Priority", GenerationPriority.FAR_FIRST);
				
				distanceGenerationMode = builder
						.comment("\n\n"
								+ " Note: The times listed here are the amount of time it took \n"
								+ "       one of the developer's PC to generate 1 chunk, \n"
								+ "       and are included so you can compare the \n"
								+ "       different generation options. Your mileage may vary. \n"
								+ "\n"
								
								+ " " + DistanceGenerationMode.NONE + " \n"
								+ " Don't run the distance generator. \n"
								
								+ "\n"
								+ " " + DistanceGenerationMode.BIOME_ONLY + " \n"
								+ " Only generate the biomes and use the biome's \n"
								+ " grass color, water color, or snow color. \n"
								+ " Doesn't generate height, everything is shown at sea level. \n"
								+ " Multithreaded - Fastest (2-5 ms) \n"
								
								+ "\n"
								+ " " + DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT + " \n"
								+ " Same as BIOME_ONLY, except instead \n"
								+ " of always using sea level as the LOD height \n"
								+ " different biome types (mountain, ocean, forest, etc.) \n"
								+ " use predetermined heights to simulate having height data. \n"
								+ " Multithreaded - Fastest (2-5 ms) \n"
								
								+ "\n"
								+ " " + DistanceGenerationMode.SURFACE + " \n"
								+ " Generate the world surface, \n"
								+ " this does NOT include trees, \n"
								+ " or structures. \n"
								+ " Multithreaded - Faster (10-20 ms) \n"
								
								+ "\n"
								+ " " + DistanceGenerationMode.FEATURES + " \n"
								+ " Generate everything except structures. \n"
								+ " WARNING: This may cause world generation bugs or instability! \n"
								+ " Multithreaded - Fast (15-20 ms) \n"
								
								+ "\n"
								+ " " + DistanceGenerationMode.SERVER + " \n"
								+ " Ask the server to generate/load each chunk. \n"
								+ " This will show player made structures, which can \n"
								+ " be useful if you are adding the mod to a pre-existing world. \n"
								+ " This is the most compatible, but causes server/simulation lag. \n"
								+ " SingleThreaded - Slow (15-50 ms, with spikes up to 200 ms) \n")
						.defineEnum("Distance Generation Mode", DistanceGenerationMode.SURFACE);
				
				allowUnstableFeatureGeneration = builder
						.comment("\n\n"
								+ " When using the " + DistanceGenerationMode.FEATURES + " generation mode \n"
								+ " some features may not be thread safe, which could \n"
								+ " cause instability and crashes. \n"
								+ " By default (false) those features are skipped, \n"
								+ " improving stability, but decreasing how many features are \n"
								+ " actually generated. \n"
								+ " (for example: some tree generation is unstable, \n"
								+ "               so some trees may not be generated.) \n"
								+ " By setting this to true, all features will be generated, \n"
								+ " but your game will be more unstable and crashes may occur. \n"
								+ " \n"
								+ " I would love to remove this option and always generate everything, \n"
								+ " but I'm not sure how to do that. \n"
								+ " If you are a Java wizard, check out the git issue here: \n"
								+ " https://gitlab.com/jeseibel/minecraft-lod-mod/-/issues/35 \n")
						.define("Allow Unstable Feature Generation", false);
				
				blockToAvoid = builder
						.comment("\n\n"
								+ " " + BlockToAvoid.NONE + ": Use all blocks when generating fake chunks \n\n"
								+ ""
								+ " " + BlockToAvoid.NON_FULL + ": Only use full blocks when generating fake chunks (ignores slabs, lanterns, torches, grass, etc.) \n\n"
								+ ""
								+ " " + BlockToAvoid.NO_COLLISION + ": Only use solid blocks when generating fake chunks (ignores grass, torches, etc.) \n"
								+ ""
								+ " " + BlockToAvoid.BOTH + ": Only use full solid blocks when generating fake chunks \n"
								+ "\n")
						.defineEnum("Block to avoid", BlockToAvoid.BOTH);
				
				/*useExperimentalPreGenLoading = builder
						 .comment("\n\n"
								+ " if a chunk has been pre-generated, then the mod would use the real chunk for the \n"
								+ "fake chunk creation. May require a deletion of the lod file to see the result. \n")
						 .define("Use pre-generated chunks", false);*/
				builder.pop();
			}
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		public static class AdvancedModOptions
		{
			
			public final Threading threading;
			public final Debugging debugging;
			public final Buffers buffers;
			
			public AdvancedModOptions(ForgeConfigSpec.Builder builder)
			{
				builder.comment("Advanced mod settings").push(this.getClass().getSimpleName());
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
					builder.comment("These settings control how many CPU threads the mod uses for different tasks.").push(this.getClass().getSimpleName());
					
					numberOfWorldGenerationThreads = builder
							.comment("\n\n"
									+ " This is how many threads are used when generating LODs outside \n"
									+ " the normal render distance. \n"
									+ " If you experience stuttering when generating distant LODs, decrease \n"
									+ " this number. If you want to increase LOD generation speed, \n"
									+ " increase this number. \n\n"
									+ ""
									+ " The maximum value is the number of logical processors on your CPU. \n"
									+ " Requires a restart to take effect. \n")
							.defineInRange("numberOfWorldGenerationThreads", Math.max(1, Runtime.getRuntime().availableProcessors() / 2), 1, Runtime.getRuntime().availableProcessors());
					
					numberOfBufferBuilderThreads = builder
							.comment("\n\n"
									+ " This is how many threads are used when building vertex buffers \n"
									+ " (The things sent to your GPU to draw the fake chunks). \n"
									+ " If you experience high CPU usage when NOT generating distant \n"
									+ " fake chunks, lower this number. \n"
									+ " \n"
									+ " The maximum value is the number of logical processors on your CPU. \n"
									+ " Requires a restart to take effect. \n")
							.defineInRange("numberOfBufferBuilderThreads", Math.max(1, Runtime.getRuntime().availableProcessors() / 2), 1, Runtime.getRuntime().availableProcessors());
					
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
					builder.comment("These settings can be used to look for bugs, or see how certain aspects of the mod work.").push(this.getClass().getSimpleName());
					
					drawLods = builder
							.comment("\n\n"
									+ " If true, the mod is enabled and fake chunks will be drawn. \n"
									+ " If false, the mod will still generate fake chunks, \n"
									+ " but they won't be rendered. \n")
							.define("Disable Drawing", true);
					
					debugMode = builder
							.comment("\n\n"
									+ " " + DebugMode.OFF + ": Fake chunks will be drawn with their normal colors. \n"
									+ " " + DebugMode.SHOW_DETAIL + ": Fake chunks color will be based on their detail level. \n"
									+ " " + DebugMode.SHOW_DETAIL_WIREFRAME + ": Fake chunks color will be based on their detail level, drawn as a wireframe. \n")
							.defineEnum("Debug Mode", DebugMode.OFF);
					
					enableDebugKeybindings = builder
							.comment("\n\n"
									+ " If true the F4 key can be used to cycle through the different debug modes. \n"
									+ " and the F6 key can be used to enable and disable LOD rendering.")
							.define("Enable Debug Keybinding", false);
					
					builder.pop();
				}
			}
			
			
			public static class Buffers
			{
				public final ForgeConfigSpec.EnumValue<BufferRebuildTimes> rebuildTimes;
				
				Buffers(ForgeConfigSpec.Builder builder)
				{
					builder.comment("These settings affect how often geometry is are built.").push(this.getClass().getSimpleName());
					
					rebuildTimes = builder
							.comment("\n\n"
									+ " How frequently should geometry be rebuilt and sent to the GPU? \n"
									+ " Higher settings may cause stuttering, but will prevent holes in the world \n")
							.defineEnum("rebuildFrequency", BufferRebuildTimes.NORMAL);
					
					builder.pop();
				}
			}
		}
	}
	
	
	/** {@link Path} to the configuration file of this mod */
	private static final Path CONFIG_PATH = Paths.get("config", ModInfo.MODNAME + ".toml");
	
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
		LogManager.getLogger().debug(ModInfo.MODNAME, "Loaded forge config file {}", configEvent.getConfig().getFileName());
	}
	
	@SubscribeEvent
	public static void onFileChange(final ModConfig.Reloading configEvent)
	{
		LogManager.getLogger().debug(ModInfo.MODNAME, "Forge config just got changed on the file system!");
	}
	
}
