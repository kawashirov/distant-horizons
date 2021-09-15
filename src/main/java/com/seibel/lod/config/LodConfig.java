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

import com.seibel.lod.enums.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.seibel.lod.ModInfo;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * This handles any configuration the user has access to.
 *
 * @author James Seibel
 * @version 9-1-2021
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
		public ForgeConfigSpec.BooleanValue drawLODs;
		
		public ForgeConfigSpec.EnumValue<FogDistance> fogDistance;
		public ForgeConfigSpec.EnumValue<FogDrawOverride> fogDrawOverride;
		
		public ForgeConfigSpec.EnumValue<LodTemplate> lodTemplate;
		
		public ForgeConfigSpec.EnumValue<LodDetail> maxDrawDetail;
		
		public ForgeConfigSpec.EnumValue<ShadingMode> shadingMode;
		
		public ForgeConfigSpec.IntValue lodQuality;
		
		public ForgeConfigSpec.IntValue lodChunkRenderDistance;
		
		public ForgeConfigSpec.DoubleValue brightnessMultiplier;
		public ForgeConfigSpec.DoubleValue saturationMultiplier;
		
		
		Graphics(ForgeConfigSpec.Builder builder)
		{
			builder.comment("These settings control how the LODs look.").push(this.getClass().getSimpleName());
			
			drawLODs = builder
					.comment("\n\n"
							+ " If false LODs will not be drawn, \n"
							+ " however they will still be generated \n"
							+ " and saved to file for later use. \n")
					.define("drawLODs", true);
			
			fogDistance = builder
					.comment("\n\n"
							+ " At what distance should Fog be drawn on the LODs? \n"
							+ " If the fog cuts off ubruptly or you are using Optifine's \"fast\" fog option \n"
							+ " set this to " + FogDistance.NEAR.toString() + " or " + FogDistance.FAR.toString() + ". \n")
					.defineEnum("fogDistance", FogDistance.NEAR_AND_FAR);
			
			fogDrawOverride = builder
					.comment("\n\n"
							+ " When should fog be drawn? \n"
							+ " " + FogDrawOverride.USE_OPTIFINE_FOG_SETTING.toString() + ": Use whatever Fog setting Optifine is using. If Optifine isn't installed this defaults to " + FogDrawOverride.ALWAYS_DRAW_FOG_FANCY.toString() + ". \n"
							+ " " + FogDrawOverride.NEVER_DRAW_FOG.toString() + ": Never draw fog on the LODs \n"
							+ " " + FogDrawOverride.ALWAYS_DRAW_FOG_FAST.toString() + ": Always draw fast fog on the LODs \n"
							+ " " + FogDrawOverride.ALWAYS_DRAW_FOG_FANCY.toString() + ": Always draw fancy fog on the LODs (if your graphics card supports it) \n")
					.defineEnum("fogDrawOverride", FogDrawOverride.USE_OPTIFINE_FOG_SETTING);
			
			lodTemplate = builder
					.comment("\n\n"
							+ " How should the LODs be drawn? \n"
							+ " NOTE: Currently only " + LodTemplate.CUBIC.toString() + " is implemented! \n"
							+ " \n"
							+ " " + LodTemplate.CUBIC.toString() + ": LOD Chunks are drawn as rectangular prisms (boxes). \n"
							+ " " + LodTemplate.TRIANGULAR.toString() + ": LOD Chunks smoothly transition between other. \n"
							+ " " + LodTemplate.DYNAMIC.toString() + ": LOD Chunks smoothly transition between other, \n"
							+ " " + "         unless a neighboring chunk is at a significantly different height. \n")
					.defineEnum("lodTemplate", LodTemplate.CUBIC);
			
			maxDrawDetail = builder
					.comment("\n\n"
							+ " What is the maximum detail level that LODs should be drawn at? \n"
							+ " " + LodDetail.SINGLE.toString() + ": render 1 LOD for each Chunk. \n"
							+ " " + LodDetail.DOUBLE.toString() + ": render 4 LODs for each Chunk. \n"
							+ " " + LodDetail.QUAD.toString() + ": render 16 LODs for each Chunk. \n"
							+ " " + LodDetail.HALF.toString() + ": render 64 LODs for each Chunk. \n"
							+ " " + LodDetail.FULL.toString() + ": render 256 LODs for each Chunk. \n")
					.defineEnum("lodDrawQuality", LodDetail.FULL);
			
			lodQuality = builder
					.comment("\n\n"
							+ " this value is multiplied by 128 and determine \n"
							+ " how much the quality decrease over distance \n")
					.defineInRange("lodQuality", 1, 1, 4);
			
			lodChunkRenderDistance = builder
					.comment("\n\n"
							+ " This is the render distance of the mod \n")
					.defineInRange("lodChunkRenderDistance", 64, 32, 512);
			
			shadingMode = builder
					.comment("\n\n"
							+ " What kind of shading should the LODs have? \n"
							+ " \n"
							+ " " + ShadingMode.NONE.toString() + " \n"
							+ " " + "LODs will have the same lighting on every side. \n"
							+ " " + "Can make large similarly colored areas hard to differentiate. \n"
							+ "\n"
							+ " " + ShadingMode.DARKEN_SIDES.toString() + " \n"
							+ " " + "LODs will have darker sides and bottoms to simulate Minecraft's flat lighting.")
					.defineEnum("lightingMode", ShadingMode.DARKEN_SIDES);
			
			brightnessMultiplier = builder
					.comment("\n\n"
							+ " Change how bright LOD colors are. \n"
							+ " 0 = black \n"
							+ " 1 = normal color value \n"
							+ " 2 = washed out colors \n")
					.defineInRange("brightnessMultiplier", 1.0, 0, 2);
			
			saturationMultiplier = builder
					.comment("\n\n"
							+ " Change how saturated LOD colors are. \n"
							+ " 0 = black and white \n"
							+ " 1 = normal saturation \n"
							+ " 2 = very saturated \n")
					.defineInRange("saturationMultiplier", 1.0, 0, 2);
			
			
			builder.pop();
		}
	}
	
	public static class WorldGenerator
	{
		public ForgeConfigSpec.EnumValue<LodQualityMode> lodQualityMode;
		public ForgeConfigSpec.EnumValue<LodDetail> maxGenerationDetail;
		public ForgeConfigSpec.EnumValue<DistanceGenerationMode> distanceGenerationMode;
		public ForgeConfigSpec.BooleanValue allowUnstableFeatureGeneration;
		public ForgeConfigSpec.EnumValue<DistanceCalculatorType> lodDistanceCalculatorType;
		
		
		WorldGenerator(ForgeConfigSpec.Builder builder)
		{
			builder.comment("These settings control how LODs outside your normal view range are generated.").push(this.getClass().getSimpleName());

			lodQualityMode = builder
					                 .comment("\n\n"
							                          + " Use 3d lods or 2d lods? \n"
							                          + " " + LodQualityMode.HEIGHTMAP.toString() + ": enable 2d lods with heightmap \n"
							                          + " " + LodQualityMode.MULTI_LOD.toString() + ": enable 3d lods with heightmap \n")
					                 .defineEnum("lodQualityMode", LodQualityMode.HEIGHTMAP);

			maxGenerationDetail = builder
					.comment("\n\n"
							+ " What is the maximum detail level that LODs should be generated at? \n"
							+ " " + LodDetail.SINGLE.toString() + ": render 1 LOD for each Chunk. \n"
							+ " " + LodDetail.DOUBLE.toString() + ": render 4 LODs for each Chunk. \n"
							+ " " + LodDetail.QUAD.toString() + ": render 16 LODs for each Chunk. \n"
							+ " " + LodDetail.HALF.toString() + ": render 64 LODs for each Chunk. \n"
							+ " " + LodDetail.FULL.toString() + ": render 256 LODs for each Chunk. \n")
					.defineEnum("lodGenerationQuality", LodDetail.HALF);
			
			lodDistanceCalculatorType = builder
					.comment("\n\n"
							+ " " + DistanceCalculatorType.LINEAR + " \n"
							+ " with LINEAR calculator the quality of block decrease \n"
							+ " linearly to the distance of the player \n"
							
							+ "\n"
							+ " " + DistanceCalculatorType.QUADRATIC + " \n"
							+ " with LINEAR calculator the quality of block decrease \n"
							+ " quadratically to the distance of the player \n"
							
							+ "\n"
							+ " " + DistanceCalculatorType.RENDER_DEPENDANT + " \n"
							+ " with LINEAR calculator the quality of block decrease \n"
							+ " quadratically to the distance of the player \n")
					.defineEnum("lodDistanceComputation", DistanceCalculatorType.LINEAR);
			
			distanceGenerationMode = builder
					.comment("\n\n"
							+ " Note: The times listed here are the amount of time it took \n"
							+ "       the developer's PC to generate 1 chunk, \n"
							+ "       and are included so you can compare the \n"
							+ "       different generation options. Your mileage may vary. \n"
							+ "\n"

							+ " " + DistanceGenerationMode.NONE.toString() + " \n"
							+ " Don't run the distance generator. \n"
							
							+ " " + DistanceGenerationMode.BIOME_ONLY.toString() + " \n"
							+ " Only generate the biomes and use biome \n"
							+ " grass/foliage color, water color, or snow color \n"
							+ " to generate the color. \n"
							+ " Doesn't generate height, everything is shown at sea level. \n"
							+ " Multithreaded - Fastest (2-5 ms) \n"
							
							+ "\n"
							+ " " + DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT.toString() + " \n"
							+ " Same as BIOME_ONLY, except instead \n"
							+ " of always using sea level as the LOD height \n"
							+ " different biome types (mountain, ocean, forest, etc.) \n"
							+ " use predetermined heights to simulate having height data. \n"
							+ " Multithreaded - Fastest (2-5 ms) \n"
							
							+ "\n"
							+ " " + DistanceGenerationMode.SURFACE.toString() + " \n"
							+ " Generate the world surface, \n"
							+ " this does NOT include caves, trees, \n"
							+ " or structures. \n"
							+ " Multithreaded - Faster (10-20 ms) \n"
							
							+ "\n"
							+ " " + DistanceGenerationMode.FEATURES.toString() + " \n"
							+ " Generate everything except structures. \n"
							+ " WARNING: This may cause world generation bugs or instability! \n"
							+ " Multithreaded - Fast (15-20 ms) \n"
							
							+ "\n"
							+ " " + DistanceGenerationMode.SERVER.toString() + " \n"
							+ " Ask the server to generate/load each chunk. \n"
							+ " This is the most compatible, but causes server/simulation lag. \n"
							+ " This will also show player made structures if you \n"
							+ " are adding the mod to a pre-existing world. \n"
							+ " Singlethreaded - Slow (15-50 ms, with spikes up to 200 ms) \n")
					.defineEnum("distanceGenerationMode", DistanceGenerationMode.SURFACE);
			
			allowUnstableFeatureGeneration = builder
					.comment("\n\n"
							+ " When using the " + DistanceGenerationMode.FEATURES.toString() + " generation mode \n"
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
							+ " The maximum value is the number of processors on your CPU. \n"
							+ " Requires a restart to take effect. \n")
					.defineInRange("numberOfWorldGenerationThreads", Runtime.getRuntime().availableProcessors() / 2, 1, Runtime.getRuntime().availableProcessors());
			
			numberOfBufferBuilderThreads = builder
					.comment("\n\n"
							+ " This is how many threads are used when building vertex buffers \n"
							+ " (The things sent to your GPU to draw the LODs). \n"
							+ " If you experience high CPU useage when NOT generating distant \n"
							+ " LODs, lower this number. \n"
							+ " \n"
							+ " The maximum value is the number of processors on your CPU. \n"
							+ " Requires a restart to take effect. \n")
					.defineInRange("numberOfBufferBuilderThreads", Runtime.getRuntime().availableProcessors(), 1, Runtime.getRuntime().availableProcessors());
			
			builder.pop();
		}
	}
	
	public static class Debugging
	{
		public ForgeConfigSpec.EnumValue<DebugMode> debugMode;
		public ForgeConfigSpec.BooleanValue enableDebugKeybinding;
		
		Debugging(ForgeConfigSpec.Builder builder)
		{
			builder.comment("These settings can be used by to look for bugs, or see how certain parts of the mod are working.").push(this.getClass().getSimpleName());
			
			debugMode = builder
					.comment("\n\n"
							+ " " + DebugMode.OFF.toString() + ": LODs will draw with their normal colors. \n"
							+ " " + DebugMode.SHOW_DETAIL.toString() + ": LOD colors will be based on their detail. \n"
							+ " " + DebugMode.SHOW_DETAIL_WIREFRAME.toString() + ": LOD colors will be based on their detail, drawn with wireframe. \n")
					.defineEnum("debugMode", DebugMode.OFF);
			
			enableDebugKeybinding = builder
					.comment("\n\n"
							+ " If true the F4 key can be used to cycle through the different debug modes. \n")
					.define("enableDebugKeybinding", false);
			
			builder.pop();
		}
	}
	
	public static class Buffers
	{
		public ForgeConfigSpec.IntValue bufferRebuildPlayerMoveTimeout;
		public ForgeConfigSpec.IntValue bufferRebuildChunkChangeTimeout;
		public ForgeConfigSpec.IntValue bufferRebuildLodChangeTimeout;
		
		Buffers(ForgeConfigSpec.Builder builder)
		{
			builder.comment("These settings affect when Vertex Buffers are built.").push(this.getClass().getSimpleName());
			
			bufferRebuildPlayerMoveTimeout = builder
					.comment("\n\n"
							+ " How long in milliseconds should we wait to \n"
							+ " rebuild the vertex buffers when the player moves \n"
							+ " a chunk or more? \n")
					.defineInRange("bufferRebuildPlayerMoveTimeout", 2000, 1, 60000);
			
			bufferRebuildChunkChangeTimeout = builder
					.comment("\n\n"
							+ " How long in milliseconds should we wait to \n"
							+ " rebuild the vertex buffers when the vanilla rendered \n"
							+ " chunks change? \n")
					.defineInRange("bufferRebuildChunkChangeTimeout", 1000, 1, 60000);
			
			bufferRebuildLodChangeTimeout = builder
					.comment("\n\n"
							+ " How long in milliseconds should we wait to \n"
							+ " rebuild the vertex buffers when the LOD regions change? \n")
					.defineInRange("bufferRebuildLodChangeTimeout", 5000, 1, 60000);
			
			
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
