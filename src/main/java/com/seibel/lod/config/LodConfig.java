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
import com.seibel.lod.enums.BufferRebuildTimes;
import com.seibel.lod.enums.DebugMode;
import com.seibel.lod.enums.DetailDropOff;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogDrawOverride;
import com.seibel.lod.enums.GenerationPriority;
import com.seibel.lod.enums.HorizontalQuality;
import com.seibel.lod.enums.HorizontalResolution;
import com.seibel.lod.enums.HorizontalScale;
import com.seibel.lod.enums.LodTemplate;
import com.seibel.lod.enums.VerticalQuality;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * This handles any configuration the user has access to.
 *
 * @author James Seibel
 * @version 10-9-2021
 */
@Mod.EventBusSubscriber
public class LodConfig
{
	public static class Client
	{
		public final Graphics graphics;
		public final WorldGenerator worldGenerator;
		public final Threading threading;
		public final Debugging debugging;
		public final Buffers buffers;
		
		public Client(ForgeConfigSpec.Builder builder)
		{
			builder.push("client");
			{
				graphics = new Graphics(builder);
				worldGenerator = new WorldGenerator(builder);
				threading = new Threading(builder);
				debugging = new Debugging(builder);
				buffers = new Buffers(builder);
			}
			builder.pop();
		}
	}
	
	//================//
	// Client Configs //
	//================//
	
	public static class Graphics
	{
		public ForgeConfigSpec.EnumValue<FogDistance> fogDistance;
		public ForgeConfigSpec.EnumValue<FogDrawOverride> fogDrawOverride;
		
		public ForgeConfigSpec.EnumValue<LodTemplate> lodTemplate;
		
		public ForgeConfigSpec.EnumValue<HorizontalResolution> drawResolution;
		
		public ForgeConfigSpec.EnumValue<DetailDropOff> detailDropOff;
		
		public ForgeConfigSpec.IntValue lodChunkRenderDistance;
		
		public ForgeConfigSpec.BooleanValue disableDirectionalCulling;
		
		public ForgeConfigSpec.BooleanValue alwaysDrawAtMaxQuality;
		
		public ForgeConfigSpec.BooleanValue drawLods;
		
		
		Graphics(ForgeConfigSpec.Builder builder)
		{
			builder.comment("These settings control how the LODs look.").push(this.getClass().getSimpleName());
			
			drawLods = builder
					.comment("\n\n"
							+ " If true, the mod is enabled and LODs will be drawn. \n"
							+ " If false, the mod will still generate LODs, \n"
							+ " but they won't be rendered. \n")
					.define("drawLODs", true);
			
			fogDistance = builder
					.comment("\n\n"
							+ " At what distance should Fog be drawn on the LODs? \n"
							+ " If the fog cuts off ubruptly or you are using Optifine's \"fast\" fog option \n"
							+ " set this to " + FogDistance.NEAR + " or " + FogDistance.FAR + ". \n")
					.defineEnum("fogDistance", FogDistance.NEAR_AND_FAR);
			
			fogDrawOverride = builder
					.comment("\n\n"
							+ " When should fog be drawn? \n"
							+ " " + FogDrawOverride.USE_OPTIFINE_FOG_SETTING + ": Use whatever Fog setting Optifine is using. If Optifine isn't installed this defaults to " + FogDrawOverride.ALWAYS_DRAW_FOG_FANCY + ". \n"
							+ " " + FogDrawOverride.NEVER_DRAW_FOG + ": Never draw fog on the LODs \n"
							+ " " + FogDrawOverride.ALWAYS_DRAW_FOG_FAST + ": Always draw fast fog on the LODs \n"
							+ " " + FogDrawOverride.ALWAYS_DRAW_FOG_FANCY + ": Always draw fancy fog on the LODs (if your graphics card supports it) \n")
					.defineEnum("fogDrawOverride", FogDrawOverride.ALWAYS_DRAW_FOG_FANCY);
			
			lodTemplate = builder
					.comment("\n\n"
							+ " How should the LODs be drawn? \n"
							+ " NOTE: Currently only " + LodTemplate.CUBIC + " is implemented! \n"
							+ " \n"
							+ " " + LodTemplate.CUBIC + ": LOD Chunks are drawn as rectangular prisms (boxes). \n"
							+ " " + LodTemplate.TRIANGULAR + ": LOD Chunks smoothly transition between other. \n"
							+ " " + LodTemplate.DYNAMIC + ": LOD Chunks smoothly transition between other, \n"
							+ " " + "         unless a neighboring chunk is at a significantly different height. \n")
					.defineEnum("lodTemplate", LodTemplate.CUBIC);
			
			detailDropOff = builder
					.comment("\n\n"
							+ " How smooth should the detail transition for LODs be? \n"
							+ DetailDropOff.FANCY + ": quality is determined per-block (best quality option, may cause stuttering when moving)\n"
							+ DetailDropOff.FAST + ": quality is determined per-region (performance option)\n")
					.defineEnum("detailDropOff", DetailDropOff.FANCY);
			
			drawResolution = builder
					.comment("\n\n"
							+ " What is the maximum detail LODs should be drawn at? \n"
							+ " " + HorizontalResolution.CHUNK + ": render 1 LOD for each Chunk. \n"
							+ " " + HorizontalResolution.HALF_CHUNK + ": render 4 LODs for each Chunk. \n"
							+ " " + HorizontalResolution.FOUR_BLOCKS + ": render 16 LODs for each Chunk. \n"
							+ " " + HorizontalResolution.TWO_BLOCKS + ": render 64 LODs for each Chunk. \n"
							+ " " + HorizontalResolution.BLOCK + ": render 256 LODs for each Chunk. \n")
					.defineEnum("Draw resolution", HorizontalResolution.BLOCK);
			
			
			lodChunkRenderDistance = builder
					.comment("\n\n"
							+ " The mod's render distance, measured in chunks. \n")
					.defineInRange("lodChunkRenderDistance", 64, 32, 1024);
			
			disableDirectionalCulling = builder
					.comment("\n\n"
							+ " If false LODs that are behind the player's camera \n"
							+ " aren't drawn, increasing performance. \n\n"
							+ ""
							+ " If true all LODs are drawn, even those behind \n"
							+ " the player's camera, decreasing performance. \n\n"
							+ ""
							+ " Disable this if you see LODs disapearing. \n"
							+ " (This may happen if you are using a camera mod) \n")
					.define("disableDirectionalCulling", false);
			
			alwaysDrawAtMaxQuality = builder
					.comment("\n\n"
							+ " Disable LOD quality falloff, "
							+ " all LODs will be drawn at the highest "
							+ " available detail level. "
							+ " "
							+ " WARNING "
							+ " This could cause a Out Of Memory crash on render "
							+ " distances higher than 128 \n")
					.define("alwaysDrawAtMaxQuality", false);
			
			builder.pop();
		}
	}
	
	public static class WorldGenerator
	{
		public ForgeConfigSpec.EnumValue<VerticalQuality> verticalQuality;
		public ForgeConfigSpec.EnumValue<HorizontalResolution> generationResolution;
		public ForgeConfigSpec.EnumValue<DistanceGenerationMode> distanceGenerationMode;
		public ForgeConfigSpec.EnumValue<GenerationPriority> generationPriority;
		public ForgeConfigSpec.BooleanValue allowUnstableFeatureGeneration;
		public ForgeConfigSpec.EnumValue<HorizontalScale> horizontalScale;
		public ForgeConfigSpec.EnumValue<HorizontalQuality> horizontalQuality;
		
		WorldGenerator(ForgeConfigSpec.Builder builder)
		{
			builder.comment("These settings control how LODs outside your normal view range are generated.").push(this.getClass().getSimpleName());
			
			verticalQuality = builder
					.comment("\n\n"
							+ " this indicate how complex the vertical data will be \n"
							+ " the higher value will take more memory and lower the performance \n"
							+ " " + VerticalQuality.LOW + ": uses at max 2 column per position. (performance option)\n"
							+ " " + VerticalQuality.MEDIUM + ": uses at max 4 column per position. \n"
							+ " " + VerticalQuality.HIGH + ": uses at max 8 column per position. \n"
							+ " " + "(Yes we know voxels are generally cubes, but voxel sounds better than rectangular_prism) \n")
					.defineEnum("Vertical Quality", VerticalQuality.MEDIUM);
			
			generationResolution = builder
					.comment("\n\n"
							+ " What is the maximum detail level that LODs should be generated at? \n"
							+ " " + HorizontalResolution.CHUNK + ": render 1 LOD for each Chunk. \n"
							+ " " + HorizontalResolution.HALF_CHUNK + ": render 4 LODs for each Chunk. \n"
							+ " " + HorizontalResolution.FOUR_BLOCKS + ": render 16 LODs for each Chunk. \n"
							+ " " + HorizontalResolution.TWO_BLOCKS + ": render 64 LODs for each Chunk. \n"
							+ " " + HorizontalResolution.BLOCK + ": render 256 LODs for each Chunk. \n")
					.defineEnum("Generation Resolution", HorizontalResolution.BLOCK);
			
			horizontalScale = builder
					.comment("\n\n"
							+ " This indicates how quickly LODs drop off in quality. \n"
							+ " " + HorizontalScale.LOW + ": quality drops every " + HorizontalScale.LOW.distanceUnit / 16 + " chunks. \n"
							+ " " + HorizontalScale.MEDIUM + ": quality drops every " + HorizontalScale.MEDIUM.distanceUnit / 16 + " chunks. \n"
							+ " " + HorizontalScale.HIGH + ": quality drops every " + HorizontalScale.HIGH.distanceUnit / 16 + " chunks. \n")
					.defineEnum("horizontal scale", HorizontalScale.MEDIUM);
			
			horizontalQuality = builder
					.comment("\n\n"
							+ " This indicates the exponential base of the quadratic drop-off \n"
							+ " " + HorizontalQuality.LINEAR + ": base " + HorizontalQuality.LINEAR.quadraticBase + ". \n"
							+ " " + HorizontalQuality.LOW + ": base " + HorizontalQuality.LOW.quadraticBase + ". \n"
							+ " " + HorizontalQuality.MEDIUM + ": base " + HorizontalQuality.MEDIUM.quadraticBase + ". \n"
							+ " " + HorizontalQuality.HIGH + ": base " + HorizontalQuality.HIGH.quadraticBase + ". \n")
					.defineEnum("horizontal quality", HorizontalQuality.MEDIUM);
			generationPriority = builder
					.comment("\n\n"
							+ " " + GenerationPriority.FAR_FIRST + " \n"
							+ " LODs are generated from low to high detail\n"
							+ " with a small priority for far regions. \n"
							+ " This fills in the world fastest. \n"
							
														  + "\n"
														  + " " + GenerationPriority.NEAR_FIRST + " \n"
														  + " LODs are generated around the player \n"
														  + " in a spiral, similar to vanilla minecraft. \n")
					.defineEnum("Generation priority", GenerationPriority.NEAR_FIRST);
			
			distanceGenerationMode = builder
					.comment("\n\n"
							+ " Note: The times listed here are the amount of time it took \n"
							+ "       one of the developer's PC to generate 1 chunk, \n"
							+ "       and are included so you can compare the \n"
							+ "       different generation options. Your mileage may vary. \n"
							+ "\n"
							
															  + " " + DistanceGenerationMode.NONE + " \n"
															  + " Don't run the distance generator. \n"
															  
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
															  + " this does NOT include caves, trees, \n"
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
															  + " This is the most compatible, but causes server/simulation lag. \n"
															  + " This will show player made structures, which can \n"
															  + " be useful if you are adding the mod to a pre-existing world. \n"
															  + " Singlethreaded - Slow (15-50 ms, with spikes up to 200 ms) \n")
					.defineEnum("distanceGenerationMode", DistanceGenerationMode.SURFACE);
			
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
					.define("allowUnstableFeatureGeneration", false);
			
			builder.pop();
		}
	}
	
	public static class Threading
	{
		public ForgeConfigSpec.IntValue numberOfWorldGenerationThreads;
		public ForgeConfigSpec.IntValue numberOfBufferBuilderThreads;
		
		Threading(ForgeConfigSpec.Builder builder)
		{
			builder.comment("These settings control how many CPU threads the mod uses for different tasks.").push(this.getClass().getSimpleName());
			
			numberOfWorldGenerationThreads = builder
					.comment("\n\n"
							+ " This is how many threads are used when generating LODs outside \n"
							+ " the normal render distance. \n"
							+ " If you experience stuttering when generating distant LODs, decrease \n"
							+ " this number. If you want to increase LOD generation speed, \n"
							+ " increase this number. \n"
							+ " \n"
							+ " The maximum value is the number of logical processors on your CPU. \n"
							+ " Requires a restart to take effect. \n")
					.defineInRange("numberOfWorldGenerationThreads", Runtime.getRuntime().availableProcessors() / 2, 1, Runtime.getRuntime().availableProcessors());
			
			numberOfBufferBuilderThreads = builder
					.comment("\n\n"
							+ " This is how many threads are used when building vertex buffers \n"
							+ " (The things sent to your GPU to draw the LODs). \n"
							+ " If you experience high CPU useage when NOT generating distant \n"
							+ " LODs, lower this number. \n"
							+ " \n"
							+ " The maximum value is the number of logical processors on your CPU. \n"
							+ " Requires a restart to take effect. \n")
					.defineInRange("numberOfBufferBuilderThreads", Runtime.getRuntime().availableProcessors(), 1, Runtime.getRuntime().availableProcessors());
			
			builder.pop();
		}
	}
	
	public static class Debugging
	{
		public ForgeConfigSpec.EnumValue<DebugMode> debugMode;
		public ForgeConfigSpec.BooleanValue enableDebugKeybindings;
		
		Debugging(ForgeConfigSpec.Builder builder)
		{
			builder.comment("These settings can be used to look for bugs, or see how certain aspects of the mod work.").push(this.getClass().getSimpleName());
			
			debugMode = builder
					.comment("\n\n"
							+ " " + DebugMode.OFF + ": LODs will draw with their normal colors. \n"
							+ " " + DebugMode.SHOW_DETAIL + ": LOD colors will be based on their detail level. \n"
							+ " " + DebugMode.SHOW_DETAIL_WIREFRAME + ": LOD colors will be based on their detail level, drawn as a wireframe. \n")
					.defineEnum("debugMode", DebugMode.OFF);
			
			enableDebugKeybindings = builder
					.comment("\n\n"
							+ " If true the F4 key can be used to cycle through the different debug modes. \n"
							+ " and the F6 key can be used to enable and disable LOD rendering.")
					.define("enableDebugKeybinding", false);
			
			builder.pop();
		}
	}
	
	public static class Buffers
	{
		public ForgeConfigSpec.EnumValue<BufferRebuildTimes> rebuildTimes;
		
		Buffers(ForgeConfigSpec.Builder builder)
		{
			builder.comment("These settings affect when Vertex Buffers are built.").push(this.getClass().getSimpleName());
			
			rebuildTimes = builder
					.comment("\n\n"
							+ "How frequently we rebuild the buffers?" + "\n"
							+ "more frequent rebuild implies more stuttering" + "\n")
					.defineEnum("rebuildFrequency", BufferRebuildTimes.NORMAL);
			
			builder.pop();
		}
	}
	
	/**
	 * {@link Path} to the configuration file of this mod
	 */
	private static final Path CONFIG_PATH = Paths.get("config", ModInfo.MODID + ".toml");
	
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
