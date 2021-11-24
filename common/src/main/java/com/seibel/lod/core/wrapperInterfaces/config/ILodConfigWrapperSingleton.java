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

package com.seibel.lod.core.wrapperInterfaces.config;

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
import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawOverride;
import com.seibel.lod.core.objects.MinDefaultMax;
import com.seibel.lod.core.util.LodUtil;

/**
 * This holds the config defaults, setters/getters
 * that should be hooked into the host mod loader (Fabric, Forge, etc.), and
 * the options that should be implemented in a configWrapperSingleton.
 * 
 * @author James Seibel
 * @version 11-21-2021
 */
public interface ILodConfigWrapperSingleton
{
	public IClient client();
	
	
	public interface IClient
	{
		public IGraphics graphics();
		public IWorldGenerator worldGenerator();
		public IAdvanced advanced();
		
		
		//==================//
		// Graphics Configs //
		//==================//
		public interface IGraphics
		{
			public static final String DESC = "These settings control how the mod will look in game";
			
			public IQuality quality();
			public IFogQuality fogQuality();
			public IAdvancedGraphics advancedGraphics();
			
			
			public interface IQuality
			{
				public static final String DESC = "These settings control how detailed the fake chunks will be.";
				
				HorizontalResolution DRAW_RESOLUTION_DEFAULT = HorizontalResolution.BLOCK;
				public static final String DRAW_RESOLUTION_DESC = ""
						+ " What is the maximum detail fake chunks should be drawn at? \n"
						+ " " + HorizontalResolution.CHUNK + ": render 1 LOD for each Chunk. \n"
						+ " " + HorizontalResolution.HALF_CHUNK + ": render 4 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.FOUR_BLOCKS + ": render 16 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.TWO_BLOCKS + ": render 64 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.BLOCK + ": render 256 LODs for each Chunk. \n";
				public HorizontalResolution getDrawResolution();
				public void setDrawResolution(HorizontalResolution newHorizontalResolution);
				
				MinDefaultMax<Integer> LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(16, 64, 1024);
				String LOD_CHUNK_RENDER_DISTANCE_DESC = ""
						+ " The mod's render distance, measured in chunks. \n";
				public int getLodChunkRenderDistance();
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance);
				
				VerticalQuality VERTICAL_QUALITY_DEFAULT = VerticalQuality.MEDIUM;
				String VERTICAL_QUALITY_DESC = ""
						+ " This indicates how detailed fake chunks will represent \n"
						+ " overhangs, caves, floating islands, ect. \n"
						+ " Higher options will use more memory and increase GPU usage. \n"
						+ " " + VerticalQuality.LOW + ": uses at max 2 columns per position. \n"
						+ " " + VerticalQuality.MEDIUM + ": uses at max 4 columns per position. \n"
						+ " " + VerticalQuality.HIGH + ": uses at max 8 columns per position. \n";
				public VerticalQuality getVerticalQuality();
				public void setVerticalQuality(VerticalQuality newVerticalQuality);
				
				HorizontalScale HORIZONTAL_SCALE_DEFAULT = HorizontalScale.MEDIUM;
				String HORIZONTAL_SCALE_DESC = ""
						+ " This indicates how quickly fake chunks drop off in quality. \n"
						+ " " + HorizontalScale.LOW + ": quality drops every " + HorizontalScale.LOW.distanceUnit / 16 + " chunks. \n"
						+ " " + HorizontalScale.MEDIUM + ": quality drops every " + HorizontalScale.MEDIUM.distanceUnit / 16 + " chunks. \n"
						+ " " + HorizontalScale.HIGH + ": quality drops every " + HorizontalScale.HIGH.distanceUnit / 16 + " chunks. \n";
				public HorizontalScale getHorizontalScale();
				public void setHorizontalScale(HorizontalScale newHorizontalScale);
				
				HorizontalQuality HORIZONTAL_QUALITY_DEFAULT = HorizontalQuality.MEDIUM;
				String HORIZONTAL_QUALITY_DESC = ""
						+ " This indicates the exponential base of the quadratic drop-off \n"
						+ " " + HorizontalQuality.LOWEST + ": base " + HorizontalQuality.LOWEST.quadraticBase + ". \n"
						+ " " + HorizontalQuality.LOW + ": base " + HorizontalQuality.LOW.quadraticBase + ". \n"
						+ " " + HorizontalQuality.MEDIUM + ": base " + HorizontalQuality.MEDIUM.quadraticBase + ". \n"
						+ " " + HorizontalQuality.HIGH + ": base " + HorizontalQuality.HIGH.quadraticBase + ". \n";
				public HorizontalQuality getHorizontalQuality();
				public void setHorizontalQuality(HorizontalQuality newHorizontalQuality);
			}
			
			public interface IFogQuality
			{
				String DESC = "These settings control the fog quality.";	
				
				FogDistance FOG_DISTANCE_DEFAULT = FogDistance.FAR;
				String FOG_DISTANCE_DESC = ""
						+ " At what distance should Fog be drawn on the fake chunks? \n"
						+ " If the fog cuts off abruptly or you are using Optifine's \"fast\" fog option \n"
						+ " set this to " + FogDistance.NEAR + " or " + FogDistance.FAR + ". \n";
				public FogDistance getFogDistance();
				public void setFogDistance(FogDistance newFogDistance);
				
				FogDrawOverride FOG_DRAW_OVERRIDE_DEFAULT = FogDrawOverride.FANCY;
				String FOG_DRAW_OVERRIDE_DESC = ""
						+ " When should fog be drawn? \n"
						+ " " + FogDrawOverride.OPTIFINE_SETTING + ": Use whatever Fog setting Optifine is using. If Optifine isn't installed this defaults to " + FogDrawOverride.FANCY + ". \n"
						+ " " + FogDrawOverride.NO_FOG + ": Never draw fog on the LODs \n"
						+ " " + FogDrawOverride.FAST + ": Always draw fast fog on the LODs \n"
						+ " " + FogDrawOverride.FANCY + ": Always draw fancy fog on the LODs (if your graphics card supports it) \n";
				public FogDrawOverride getFogDrawOverride();
				public void setFogDrawOverride(FogDrawOverride newFogDrawOverride);
				
				boolean DISABLE_VANILLA_FOG_DEFAULT = false;
				String DISABLE_VANILLA_FOG_DESC = ""
						+ " If true disable Minecraft's fog. \n\n"
						+ ""
						+ " Experimental! May cause issues with Sodium. \n\n"
						+ ""
						+ " Unlike Optifine or Sodium's fog disabling option this won't change \n"
						+ " performance (we don't actually disable the fog, we just tell it to render a infinite distance away). \n"
						+ " May or may not play nice with other mods that edit fog. \n";
				public boolean getDisableVanillaFog();
				public void setDisableVanillaFog(boolean newDisableVanillaFog);
			}
			
			public interface IAdvancedGraphics
			{
				String DESC = "Graphics options that are a bit more technical.";
				
				LodTemplate LOD_TEMPLATE_DEFAULT = LodTemplate.CUBIC;
				String LOD_TEMPLATE_DESC = ""
						+ " How should the LODs be drawn? \n"
						+ " NOTE: Currently only " + LodTemplate.CUBIC + " is implemented! \n"
						+ " \n"
						+ " " + LodTemplate.CUBIC + ": LOD Chunks are drawn as rectangular prisms (boxes). \n"
						+ " " + LodTemplate.TRIANGULAR + ": LOD Chunks smoothly transition between other. \n"
						+ " " + LodTemplate.DYNAMIC + ": LOD Chunks smoothly transition between each other, \n"
						+ " " + "         unless a neighboring chunk is at a significantly different height. \n";
				public LodTemplate getLodTemplate();
				public void setLodTemplate(LodTemplate newLodTemplate);
				
				boolean DISABLE_DIRECTIONAL_CULLING_DEFAULT = false;
				String DISABLE_DIRECTIONAL_CULLING_DESC = ""
						+ " If false fake chunks behind the player's camera \n"
						+ " aren't drawn, increasing performance. \n\n"
						+ ""
						+ " If true all LODs are drawn, even those behind \n"
						+ " the player's camera, decreasing performance. \n\n"
						+ ""
						+ " Disable this if you see LODs disappearing. \n"
						+ " (Which may happen if you are using a camera mod) \n";
				public boolean getDisableDirectionalCulling();
				public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling);
				
				boolean ALWAYS_DRAW_AT_MAD_QUALITY_DEFAULT = false;
				String ALWAYS_DRAW_AT_MAD_QUALITY_DESC = ""
						+ " Disable quality falloff, \n"
						+ " all fake chunks will be drawn at the highest \n"
						+ " available detail level. \n\n"
						+ " "
						+ " WARNING: \n"
						+ " This could cause a Out Of Memory crash on render \n"
						+ " distances higher than 128 \n";
				public boolean getAlwaysDrawAtMaxQuality();
				public void setAlwaysDrawAtMaxQuality(boolean newAlwaysDrawAtMaxQuality);
				
				VanillaOverdraw VANILLA_OVERDRAW_DEFAULT = VanillaOverdraw.DYNAMIC;
				String VANILLA_OVERDRAW_DESC = ""
						+ " How often should LODs be drawn on top of regular chunks? \n"
						+ " HALF and ALWAYS will prevent holes in the world, but may look odd for transparent blocks or in caves. \n\n"
						+ " " + VanillaOverdraw.NEVER + ": LODs won't render on top of vanilla chunks. \n"
						+ " " + VanillaOverdraw.BORDER + ": LODs will render only on the border of vanilla chunks preventing only some holes in the world. \n"
						+ " " + VanillaOverdraw.DYNAMIC + ": LODs will render on top of distant vanilla chunks to hide delayed loading. \n"
						+ " " + "     More effective on higher render distances. \n"
						+ " " + "     For vanilla render distances less than or equal to " + LodUtil.MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW + " \n"
						+ " " + "     " + VanillaOverdraw.NEVER + " or " + VanillaOverdraw.ALWAYS + " may be used depending on the dimension. \n"
						+ " " + VanillaOverdraw.ALWAYS + ": LODs will render on all vanilla chunks preventing holes in the world. \n";
				public VanillaOverdraw getVanillaOverdraw();
				public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw);
				
				GpuUploadMethod GPU_UPLOAD_METHOD_DEFAULT = GpuUploadMethod.BUFFER_STORAGE;
				String GPU_UPLOAD_METHOD_DESC = ""
						+ " What method should be used to upload geometry to the GPU? \n"
						+ " Listed in the suggested order of best to worst. \n\n"
						+ ""
						+ " " + GpuUploadMethod.BUFFER_STORAGE + ": Default if OpenGL 4.5 is supported. Fast rendering, no stuttering. \n"
						+ " " + GpuUploadMethod.SUB_DATA + ": Default if OpenGL 4.5 is NOT supported. Fast rendering but may stutter when uploading. \n"
						+ " " + GpuUploadMethod.DATA + ": Fast rendering but will stutter when uploading. \n"
						+ " " + GpuUploadMethod.BUFFER_MAPPING + ": Slow rendering but won't stutter when uploading. Possibly better than " + GpuUploadMethod.SUB_DATA + " if using a integrated GPU. \n";
				public GpuUploadMethod getGpuUploadMethod();
				public void setGpuUploadMethod(GpuUploadMethod newDisableVanillaFog);
				
				boolean USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT = false;
				String USE_EXTENDED_NEAR_CLIP_PLANE_DESC = ""
						+ " Will prevent some overdraw issues, but may cause nearby fake chunks to render incorrectly \n"
						+ " especially when in/near an ocean. \n";
				public boolean getUseExtendedNearClipPlane();
				public void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane);
			}
		}
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		public interface IWorldGenerator
		{
			String DESC = "These settings control how fake chunks outside your normal view range are generated.";
			
			GenerationPriority GENERATION_PRIORITY_DEFAULT = GenerationPriority.FAR_FIRST;
			String GENERATION_PRIORITY_DESC = ""
					+ " " + GenerationPriority.FAR_FIRST + " \n"
					+ " LODs are generated from low to high detail \n"
					+ " with a small priority for far away regions. \n"
					+ " This fills in the world fastest. \n\n"
					+ ""
					+ " " + GenerationPriority.NEAR_FIRST + " \n"
					+ " LODs are generated around the player \n"
					+ " in a spiral, similar to vanilla minecraft. \n";
			public GenerationPriority getGenerationPriority();
			public void setGenerationPriority(GenerationPriority newGenerationPriority);
			
			DistanceGenerationMode DISTANCE_GENERATION_MODE_DEFAULT = DistanceGenerationMode.SURFACE;
			String DISTANCE_GENERATION_MODE_DESC = ""
					+ " Note: The times listed here are the amount of time it took \n"
					+ "       one of the developer's PC to generate 1 chunk, \n"
					+ "       and are included so you can compare the \n"
					+ "       different generation options. Your mileage may vary. \n\n"
					+ ""
					+ " " + DistanceGenerationMode.NONE + " \n"
					+ " Don't run the distance generator. \n\n"
					+ ""
					+ " " + DistanceGenerationMode.BIOME_ONLY + " \n"
					+ " Only generate the biomes and use the biome's \n"
					+ " grass color, water color, or snow color. \n"
					+ " Doesn't generate height, everything is shown at sea level. \n"
					+ " Multithreaded - Fastest (2-5 ms) \n\n"
					+ ""
					+ " " + DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT + " \n"
					+ " Same as BIOME_ONLY, except instead \n"
					+ " of always using sea level as the LOD height \n"
					+ " different biome types (mountain, ocean, forest, etc.) \n"
					+ " use predetermined heights to simulate having height data. \n"
					+ " Multithreaded - Fastest (2-5 ms) \n\n"
					+ ""
					+ " " + DistanceGenerationMode.SURFACE + " \n"
					+ " Generate the world surface, \n"
					+ " this does NOT include trees, \n"
					+ " or structures. \n"
					+ " Multithreaded - Faster (10-20 ms) \n\n"
					+ ""
					+ " " + DistanceGenerationMode.FEATURES + " \n"
					+ " Generate everything except structures. \n"
					+ " WARNING: This may cause world generation bugs or instability! \n"
					+ " Multithreaded - Fast (15-20 ms) \n\n"
					+ ""
					+ " " + DistanceGenerationMode.FULL + " \n"
					+ " Ask the local server to generate/load each chunk. \n"
					+ " This will show player made structures, which can \n"
					+ " be useful if you are adding the mod to a pre-existing world. \n"
					+ " This is the most compatible, but causes server/simulation lag. \n"
					+ " SingleThreaded - Slow (15-50 ms, with spikes up to 200 ms) \n";
			public DistanceGenerationMode getDistanceGenerationMode();
			public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode);
			
			boolean ALLOW_UNSTABLE_FEATURE_GENERATION_DEFAULT = false;
			String ALLOW_UNSTABLE_FEATURE_GENERATION_DESC = ""
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
					+ " https://gitlab.com/jeseibel/minecraft-lod-mod/-/issues/35 \n";
			public boolean getAllowUnstableFeatureGeneration();
			public void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration);
			
			BlocksToAvoid BLOCKS_TO_AVOID_DEFAULT = BlocksToAvoid.BOTH;
			String BLOCKS_TO_AVOID_DESC = ""
					+ " " + BlocksToAvoid.NONE + ": Use all blocks when generating fake chunks \n\n"
					+ ""
					+ " " + BlocksToAvoid.NON_FULL + ": Only use full blocks when generating fake chunks (ignores slabs, lanterns, torches, grass, etc.) \n\n"
					+ ""
					+ " " + BlocksToAvoid.NO_COLLISION + ": Only use solid blocks when generating fake chunks (ignores grass, torches, etc.) \n"
					+ ""
					+ " " + BlocksToAvoid.BOTH + ": Only use full solid blocks when generating fake chunks \n";
			public BlocksToAvoid getBlocksToAvoid();
			public void setBlockToAvoid(BlocksToAvoid newBlockToAvoid);
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		public interface IAdvanced
		{
			public static final String DESC = "Advanced mod settings";
			
			public IThreading threading();
			public IDebugging debugging();
			public IBuffers buffers();
			
			
			public interface IThreading
			{
				String DESC = "These settings control how many CPU threads the mod uses for different tasks.";
				
				MinDefaultMax<Integer> NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT
					= new MinDefaultMax<Integer>(1, 
							Runtime.getRuntime().availableProcessors() / 2,
							Runtime.getRuntime().availableProcessors());
				String NUMBER_OF_WORLD_GENERATION_THREADS_DESC = ""
						+ " This is how many threads are used when generating LODs outside \n"
						+ " the normal render distance. \n"
						+ " If you experience stuttering when generating distant LODs, decrease \n"
						+ " this number. If you want to increase LOD generation speed, \n"
						+ " increase this number. \n\n"
						+ ""
						+ " The maximum value is the number of logical processors on your CPU. \n"
						+ " Requires a restart to take effect. \n";
				public int getNumberOfWorldGenerationThreads();
				public void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads);
				
				MinDefaultMax<Integer> NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX
					= new MinDefaultMax<Integer>(1, 
							Runtime.getRuntime().availableProcessors() / 2, 
							Runtime.getRuntime().availableProcessors());
				String NUMBER_OF_BUFFER_BUILDER_THREADS_DESC = ""
						+ " This is how many threads are used when building vertex buffers \n"
						+ " (The things sent to your GPU to draw the fake chunks). \n"
						+ " If you experience high CPU usage when NOT generating distant \n"
						+ " fake chunks, lower this number. \n"
						+ " \n"
						+ " The maximum value is the number of logical processors on your CPU. \n"
						+ " Requires a restart to take effect. \n";
				public int getNumberOfBufferBuilderThreads();
				public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads);
			}
			
			public interface IDebugging
			{
				String DESC = "These settings can be used to look for bugs, or see how certain aspects of the mod work.";
				
				boolean DRAW_LODS_DEFAULT = true;
				String DRAW_LODS_DESC = ""
						+ " If true, the mod is enabled and fake chunks will be drawn. \n"
						+ " If false, the mod will still generate fake chunks, \n"
						+ " but they won't be rendered. \n";
				public boolean getDrawLods();
				public void setDrawLods(boolean newDrawLods);
				
				DebugMode DEBUG_MODE_DEFAULT = DebugMode.OFF;
				String DEBUG_MODE_DESC = ""
						+ " " + DebugMode.OFF + ": Fake chunks will be drawn with their normal colors. \n"
						+ " " + DebugMode.SHOW_DETAIL + ": Fake chunks color will be based on their detail level. \n"
						+ " " + DebugMode.SHOW_DETAIL_WIREFRAME + ": Fake chunks color will be based on their detail level, drawn as a wireframe. \n";
				public DebugMode getDebugMode();
				public void setDebugMode(DebugMode newDebugMode);
				
				boolean DEBUG_KEYBINDINGS_ENABLED_DEFAULT = true;
				String DEBUG_KEYBINDINGS_ENABLED_DESC = ""
						+ " If true the F4 key can be used to cycle through the different debug modes. \n"
						+ " and the F6 key can be used to enable and disable LOD rendering.";
				public boolean getDebugKeybindingsEnabled();
				public void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings);
			}
			
			public interface IBuffers
			{
				String DESC = "These settings affect how often geometry is rebuilt.";
				
				String REBUILD_TIMES_DESC = ""
						+ " How frequently should geometry be rebuilt and sent to the GPU? \n"
						+ " Higher settings may cause stuttering, but will prevent holes in the world \n";
				BufferRebuildTimes REBUILD_TIMES_DEFAULT = BufferRebuildTimes.NORMAL;
				public BufferRebuildTimes getRebuildTimes();
				public void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes);
			}
		}
	}
	
}
