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
import com.seibel.lod.core.enums.rendering.FogColorMode;
import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawMode;
import com.seibel.lod.core.objects.MinDefaultMax;
import com.seibel.lod.core.util.LodUtil;

/**
 * This holds the config defaults, setters/getters
 * that should be hooked into the host mod loader (Fabric, Forge, etc.), and
 * the options that should be implemented in a configWrapperSingleton.
 * 
 * @author James Seibel
 * @version 12-1-2021
 */
public interface ILodConfigWrapperSingleton
{
	IClient client();
	
	
	interface IClient
	{
		IGraphics graphics();
		IWorldGenerator worldGenerator();
		IAdvanced advanced();
		
		
		//==================//
		// Graphics Configs //
		//==================//
		interface IGraphics
		{
			String DESC = "These settings control how the mod will look in game";
			
			IQuality quality();
			IFogQuality fogQuality();
			IAdvancedGraphics advancedGraphics();
			
			
			interface IQuality
			{
				String DESC = "These settings control how detailed the fake chunks will be.";
				
				HorizontalResolution DRAW_RESOLUTION_DEFAULT = HorizontalResolution.BLOCK;
				String DRAW_RESOLUTION_DESC = ""
						+ " What is the maximum detail fake chunks should be drawn at? \n"
						+ " This setting will only affect closer chunks.\n"
						+ " Higher settings will increase memory and GPU usage. \n"
						+ "\n"
						+ " " + HorizontalResolution.CHUNK + ": render 1 LOD for each Chunk. \n"
						+ " " + HorizontalResolution.HALF_CHUNK + ": render 4 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.FOUR_BLOCKS + ": render 16 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.TWO_BLOCKS + ": render 64 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.BLOCK + ": render 256 LODs for each Chunk (width of one block). \n"
						+ "\n"
						+ " Lowest Quality: " + HorizontalResolution.CHUNK
						+ " Highest Quality: " + HorizontalResolution.BLOCK;
				HorizontalResolution getDrawResolution();
				void setDrawResolution(HorizontalResolution newHorizontalResolution);
				
				MinDefaultMax<Integer> LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(16, 64, 1024);
				String LOD_CHUNK_RENDER_DISTANCE_DESC = ""
						+ " The radius of the mod's render distance. (measured in chunks) \n";
				int getLodChunkRenderDistance();
				void setLodChunkRenderDistance(int newLodChunkRenderDistance);
				
				VerticalQuality VERTICAL_QUALITY_DEFAULT = VerticalQuality.MEDIUM;
				String VERTICAL_QUALITY_DESC = ""
						+ " This indicates how detailed fake chunks will represent \n"
						+ " overhangs, caves, floating islands, ect. \n"
						+ " Higher options will make the world more accurate, but"
						+ " will increase memory and GPU usage. \n"
						+ "\n"
						+ " " + VerticalQuality.LOW + ": uses at max 2 columns per position. \n"
						+ " " + VerticalQuality.MEDIUM + ": uses at max 4 columns per position. \n"
						+ " " + VerticalQuality.HIGH + ": uses at max 8 columns per position. \n"
						+ "\n"
						+ " Lowest Quality: " + VerticalQuality.LOW
						+ " Highest Quality: " + VerticalQuality.HIGH;
				VerticalQuality getVerticalQuality();
				void setVerticalQuality(VerticalQuality newVerticalQuality);
				
				MinDefaultMax<Integer> HORIZONTAL_SCALE_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(2, 8, 32);
				String HORIZONTAL_SCALE_DESC = ""
						+ " This indicates how quickly fake chunks decrease in quality the further away they are. \n"
						+ " Higher settings will render higher quality fake chunks farther away, \n"
						+ " but will increase memory and GPU usage.";
				int getHorizontalScale();
				void setHorizontalScale(int newHorizontalScale);
				
				HorizontalQuality HORIZONTAL_QUALITY_DEFAULT = HorizontalQuality.MEDIUM;
				String HORIZONTAL_QUALITY_DESC = ""
						+ " This indicates the exponential base of the quadratic drop-off \n"
						+ "\n"
						+ " " + HorizontalQuality.LOWEST + ": base " + HorizontalQuality.LOWEST.quadraticBase + ". \n"
						+ " " + HorizontalQuality.LOW + ": base " + HorizontalQuality.LOW.quadraticBase + ". \n"
						+ " " + HorizontalQuality.MEDIUM + ": base " + HorizontalQuality.MEDIUM.quadraticBase + ". \n"
						+ " " + HorizontalQuality.HIGH + ": base " + HorizontalQuality.HIGH.quadraticBase + ". \n"
						+ "\n"
						+ " Lowest Quality: " + HorizontalQuality.LOWEST
						+ " Highest Quality: " + HorizontalQuality.HIGH;
				HorizontalQuality getHorizontalQuality();
				void setHorizontalQuality(HorizontalQuality newHorizontalQuality);
			}
			
			interface IFogQuality
			{
				String DESC = "These settings control the fog quality.";	
				
				FogDistance FOG_DISTANCE_DEFAULT = FogDistance.FAR;
				String FOG_DISTANCE_DESC = ""
						+ " At what distance should Fog be drawn on the fake chunks? \n"
						+ "\n"
						+ " This setting shouldn't affect performance.";
				FogDistance getFogDistance();
				void setFogDistance(FogDistance newFogDistance);
				
				FogDrawMode FOG_DRAW_MODE_DEFAULT = FogDrawMode.FOG_ENABLED;
				String FOG_DRAW_MODE_DESC = ""
						+ " When should fog be drawn? \n"
						+ "\n"
						+ " " + FogDrawMode.USE_OPTIFINE_SETTING + ": Use whatever Fog setting Optifine is using. If Optifine isn't installed this defaults to " + FogDrawMode.FOG_ENABLED + ". \n"
						+ " " + FogDrawMode.FOG_ENABLED + ": Never draw fog on the LODs \n"
						+ " " + FogDrawMode.FOG_DISABLED + ": Always draw fast fog on the LODs \n"
						+ "\n"
						+ " Disabling fog will improve GPU performance.";
				FogDrawMode getFogDrawMode();
				void setFogDrawMode(FogDrawMode newFogDrawMode);
				
				FogColorMode FOG_COLOR_MODE_DEFAULT = FogColorMode.USE_WORLD_FOG_COLOR;
				String FOG_COLOR_MODE_DESC = ""
						+ " What color should fog use? \n"
						+ "\n"
						+ " " + FogColorMode.USE_WORLD_FOG_COLOR + ": Use the world's fog color. \n"
						+ " " + FogColorMode.USE_SKY_COLOR + ": Use the sky's color. \n"
						+ "\n"
						+ " This setting doesn't affect performance.";
				FogColorMode getFogColorMode();
				void setFogColorMode(FogColorMode newFogColorMode);
				
				boolean DISABLE_VANILLA_FOG_DEFAULT = false;
				String DISABLE_VANILLA_FOG_DESC = ""
						+ " If true disable Minecraft's fog. \n"
						+ "\n"
						+ " Experimental! Will cause issues with Sodium and \n"
						+ " may not play nice with other mods that edit fog. \n";
				boolean getDisableVanillaFog();
				void setDisableVanillaFog(boolean newDisableVanillaFog);
			}
			
			interface IAdvancedGraphics
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
				LodTemplate getLodTemplate();
				void setLodTemplate(LodTemplate newLodTemplate);
				
				boolean DISABLE_DIRECTIONAL_CULLING_DEFAULT = false;
				String DISABLE_DIRECTIONAL_CULLING_DESC = ""
						+ " If false fake chunks behind the player's camera \n"
						+ " aren't drawn, increasing GPU performance. \n"
						+ "\n"
						+ " If true all LODs are drawn, even those behind \n"
						+ " the player's camera, decreasing GPU performance. \n"
						+ "\n"
						+ " Disable this if you see LODs disappearing at the corners of your vision. \n";
				boolean getDisableDirectionalCulling();
				void setDisableDirectionalCulling(boolean newDisableDirectionalCulling);
				
				boolean ALWAYS_DRAW_AT_MAD_QUALITY_DEFAULT = false;
				String ALWAYS_DRAW_AT_MAD_QUALITY_DESC = ""
						+ " Disable quality falloff, \n"
						+ " all fake chunks will be drawn at the highest \n"
						+ " available detail level. \n"
						+ "\n"
						+ " WARNING: \n"
						+ " This could cause an Out Of Memory crash when using render \n"
						+ " distances higher than 128 and will drastically increase GPU usage. \n";
				boolean getAlwaysDrawAtMaxQuality();
				void setAlwaysDrawAtMaxQuality(boolean newAlwaysDrawAtMaxQuality);
				
				VanillaOverdraw VANILLA_OVERDRAW_DEFAULT = VanillaOverdraw.DYNAMIC;
				String VANILLA_OVERDRAW_DESC = ""
						+ " How often should LODs be drawn on top of regular chunks? \n"
						+ " HALF and ALWAYS will prevent holes in the world, but may look odd for transparent blocks or in caves. \n"
						+ "\n"
						+ " " + VanillaOverdraw.NEVER + ": LODs won't render on top of vanilla chunks. \n"
						+ " " + VanillaOverdraw.BORDER + ": LODs will render only on the border of vanilla chunks preventing only some holes in the world. \n"
						+ " " + VanillaOverdraw.DYNAMIC + ": LODs will render on top of distant vanilla chunks to hide delayed loading. \n"
						+ " " + "     More effective on higher render distances. \n"
						+ " " + "     For vanilla render distances less than or equal to " + LodUtil.MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW + " \n"
						+ " " + "     " + VanillaOverdraw.NEVER + " or " + VanillaOverdraw.ALWAYS + " will be used depending on the dimension. \n"
						+ " " + VanillaOverdraw.ALWAYS + ": LODs will render on all vanilla chunks preventing holes in the world. \n"
						+ "\n"
						+ " This setting shouldn't affect performance. \n";
				VanillaOverdraw getVanillaOverdraw();
				void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw);
				
				boolean USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT = false;
				String USE_EXTENDED_NEAR_CLIP_PLANE_DESC = ""
						+ " Will prevent some overdraw issues, but may cause nearby fake chunks to render incorrectly \n"
						+ " especially when in/near an ocean. \n"
						+ "\n"
						+ " This setting shouldn't affect performance. \n";
				boolean getUseExtendedNearClipPlane();
				void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane);
			}
		}
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		interface IWorldGenerator
		{
			String DESC = "These settings control how fake chunks outside your normal view range are generated.";
			
			GenerationPriority GENERATION_PRIORITY_DEFAULT = GenerationPriority.AUTO;
			String GENERATION_PRIORITY_DESC = ""
					+ " In what order should fake chunks be generated outside the vanilla render distance? \n"
					+ "\n"
					+ " " + GenerationPriority.FAR_FIRST + " \n"
					+ " Fake chunks are generated from lowest to highest detail \n"
					+ " with a small priority for far away regions. \n"
					+ " This fills in the world fastest, but you will have large low detail \n"
					+ " blocks for a while while the generation happens. \n"
					+ "\n"
					+ " " + GenerationPriority.NEAR_FIRST + " \n"
					+ " Fake chunks are generated around the player \n"
					+ " in a spiral, similar to vanilla minecraft. \n"
					+ " Best used when on a server since we can't generate \n"
					+ " fake chunks. \n"
					+ "\n"
					+ " " + GenerationPriority.AUTO + " \n"
					+ " Uses " + GenerationPriority.FAR_FIRST + " when on a single player world \n"
					+ " and " + GenerationPriority.NEAR_FIRST + " when connected to a server. \n"
					+ "\n"
					+ " This shouldn't affect performance.";
			GenerationPriority getGenerationPriority();
			void setGenerationPriority(GenerationPriority newGenerationPriority);
			
			DistanceGenerationMode DISTANCE_GENERATION_MODE_DEFAULT = DistanceGenerationMode.SURFACE;
			String DISTANCE_GENERATION_MODE_DESC = ""
					+ " How detailed should fake chunks be generated outside the vanilla render distance? \n"
					+ "\n"
					+ " " + DistanceGenerationMode.NONE + " \n"
					+ " Don't run the distance generator. \n"
					+ " No CPU usage - Fastest \n"
					+ "\n"
					+ " " + DistanceGenerationMode.BIOME_ONLY + " \n"
					+ " Only generate the biomes and use the biome's \n"
					+ " grass color, water color, or snow color. \n"
					+ " Doesn't generate height, everything is shown at sea level. \n"
					+ " Multithreaded - Fastest (2-5 ms) \n"
					+ "\n"
					+ " " + DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT + " \n"
					+ " Same as " + DistanceGenerationMode.BIOME_ONLY + ", except instead \n"
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
					+ " " + DistanceGenerationMode.FULL + " \n"
					+ " Ask the local server to generate/load each chunk. \n"
					+ " This will show player made structures, which can \n"
					+ " be useful if you are adding the mod to a pre-existing world. \n"
					+ " This is the most compatible, but causes server/simulation lag. \n"
					+ " SingleThreaded - Slow (15-50 ms, with spikes up to 200 ms) \n"
					+ "\n"
					+ " The multithreaded options may increase CPU load significantly (while generating) \n"
					+ " depending on how many world generation threads you have allocated. \n";
			DistanceGenerationMode getDistanceGenerationMode();
			void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode);
			
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
					+ "\n"
					+ " I would love to remove this option and always generate everything, \n"
					+ " but I'm not sure how to do that. \n"
					+ " If you are a Java wizard, check out the git issue here: \n"
					+ " https://gitlab.com/jeseibel/minecraft-lod-mod/-/issues/35 \n";
			boolean getAllowUnstableFeatureGeneration();
			void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration);
			
			BlocksToAvoid BLOCKS_TO_AVOID_DEFAULT = BlocksToAvoid.BOTH;
			String BLOCKS_TO_AVOID_DESC = ""
					+ " When generating fake chunks, what blocks should be ignored? \n"
					+ " Ignored blocks don't affect the height of the fake chunk, but might affect the color. \n"
					+ " So using " + BlocksToAvoid.BOTH + " will prevent snow covered blocks from appearing one block too tall, \n"
					+ " but will still show the snow's color.\n"
					+ "\n"
					+ " " + BlocksToAvoid.NONE + ": Use all blocks when generating fake chunks \n"
					+ " " + BlocksToAvoid.NON_FULL + ": Only use full blocks when generating fake chunks (ignores slabs, lanterns, torches, tall grass, etc.) \n"
					+ " " + BlocksToAvoid.NO_COLLISION + ": Only use solid blocks when generating fake chunks (ignores tall grass, torches, etc.) \n"
					+ " " + BlocksToAvoid.BOTH + ": Only use full solid blocks when generating fake chunks \n"
					+ "\n"
					+ " This wont't affect performance.";
			BlocksToAvoid getBlocksToAvoid();
			void setBlockToAvoid(BlocksToAvoid newBlockToAvoid);
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		interface IAdvanced
		{
			String DESC = "Advanced mod settings";
			
			IThreading threading();
			IDebugging debugging();
			IBuffers buffers();
			
			
			interface IThreading
			{
				String DESC = "These settings control how many CPU threads the mod uses for different tasks.";
				
				MinDefaultMax<Integer> NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT
					= new MinDefaultMax<Integer>(1, 
							Runtime.getRuntime().availableProcessors() / 2,
							Runtime.getRuntime().availableProcessors());
				String NUMBER_OF_WORLD_GENERATION_THREADS_DESC = ""
						+ " How many threads should be used when generating fake chunks outside \n"
						+ " the normal render distance? \n"
						+ "\n"
						+ " If you experience stuttering when generating distant LODs, decrease \n"
						+ " this number. If you want to increase LOD generation speed, \n"
						+ " increase this number. \n"
						+ "\n"
						+ " This and the number of buffer builder threads are independent, \n"
						+ " so if they add up to more threads than your CPU has cores, \n"
						+ " that shouldn't cause an issue. \n"
						+ "\n"
						+ " The maximum value is the number of logical processors on your CPU. \n"
						+ " Requires a restart to take effect. \n";
				int getNumberOfWorldGenerationThreads();
				void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads);
				
				MinDefaultMax<Integer> NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX
					= new MinDefaultMax<Integer>(1, 
							Runtime.getRuntime().availableProcessors() / 2, 
							Runtime.getRuntime().availableProcessors());
				String NUMBER_OF_BUFFER_BUILDER_THREADS_DESC = ""
						+ " How many threads are used when building vertex buffers? \n"
						+ " (The things sent to your GPU to draw the fake chunks). \n"
						+ "\n"
						+ " If you experience high CPU usage when NOT generating distant \n"
						+ " fake chunks, lower this number. A higher number will make fake\n"
						+ " fake chunks' transition faster when moving around the world. \n"
						+ "\n"
						+ " This and the number of world generator threads are independent, \n"
						+ " so if they add up to more threads than your CPU has cores, \n"
						+ " that shouldn't cause an issue. \n"
						+ "\n"
						+ " The maximum value is the number of logical processors on your CPU. \n"
						+ " Requires a restart to take effect. \n";
				int getNumberOfBufferBuilderThreads();
				void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads);
			}
			
			interface IDebugging
			{
				String DESC = "These settings can be used to look for bugs, or see how certain aspects of the mod work.";
				
				boolean DRAW_LODS_DEFAULT = true;
				String DRAW_LODS_DESC = ""
						+ " If true, the mod is enabled and fake chunks will be drawn. \n"
						+ " If false, the mod will still generate fake chunks, \n"
						+ " but they won't be rendered. \n"
						+ "\n"
						+ " Disabling rendering will reduce GPU usage \n";
				boolean getDrawLods();
				void setDrawLods(boolean newDrawLods);
				
				DebugMode DEBUG_MODE_DEFAULT = DebugMode.OFF;
				String DEBUG_MODE_DESC = ""
						+ " Should specialized colors/rendering modes be used? \n"
						+ "\n"
						+ " " + DebugMode.OFF + ": Fake chunks will be drawn with their normal colors. \n"
						+ " " + DebugMode.SHOW_DETAIL + ": Fake chunks color will be based on their detail level. \n"
						+ " " + DebugMode.SHOW_DETAIL_WIREFRAME + ": Fake chunks color will be based on their detail level, drawn as a wireframe. \n";
				DebugMode getDebugMode();
				void setDebugMode(DebugMode newDebugMode);
				
				boolean DEBUG_KEYBINDINGS_ENABLED_DEFAULT = true;
				String DEBUG_KEYBINDINGS_ENABLED_DESC = ""
						+ " If true the F4 key can be used to cycle through the different debug modes. \n"
						+ " and the F6 key can be used to enable and disable LOD rendering.";
				boolean getDebugKeybindingsEnabled();
				void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings);
			}
			
			interface IBuffers
			{
				String DESC = "These settings affect how often geometry is rebuilt.";
				
				GpuUploadMethod GPU_UPLOAD_METHOD_DEFAULT = GpuUploadMethod.AUTO;
				String GPU_UPLOAD_METHOD_DESC = ""
						+ " What method should be used to upload geometry to the GPU? \n"
						+ "\n"
						+ " " + GpuUploadMethod.AUTO + ": Picks the best option based on the GPU you have. \n"
						+ " " + GpuUploadMethod.BUFFER_STORAGE + ": Default for NVIDIA if OpenGL 4.5 is supported. \n"
						+ "                 Fast rendering, no stuttering. \n"
						+ " " + GpuUploadMethod.SUB_DATA + ": Backup option for NVIDIA. \n"
						+ "           Fast rendering but may stutter when uploading. \n"
						+ " " + GpuUploadMethod.BUFFER_MAPPING + ": Slow rendering but won't stutter when uploading. Possibly the best option for integrated GPUs. \n"
						+ "                Default option for AMD/Intel. \n"
						+ "                May end up storing buffers in System memory. \n"
						+ "                Fast rending if in GPU memory, slow if in system memory, \n"
						+ "                but won't stutter when uploading.  \n"
						+ " " + GpuUploadMethod.DATA + ": Fast rendering but will stutter when uploading. \n"
						+ "       Backup option for AMD/Intel. \n"
						+ "       Fast rendering but may stutter when uploading. \n"  
						+ "\n"
						+ " If you don't see any difference when changing these settings, or the world looks corrupted: \n"
						+ " Restart the game to clear the old buffers. \n";
				GpuUploadMethod getGpuUploadMethod();
				void setGpuUploadMethod(GpuUploadMethod newGpuUploadMethod);
				
				MinDefaultMax<Integer> GPU_UPLOAD_TIMEOUT_IN_MILLISECONDS_DEFAULT = new MinDefaultMax<Integer>(0, 0, 5000);
				String GPU_UPLOAD_TIMEOUT_IN_MILLISECONDS_DESC = ""
						+ " How long should we wait before uploading a buffer to the GPU? \n"
						+ " Helpful resource for frame times: https://fpstoms.com \n"
						+ "\n"
						+ " Longer times may reduce stuttering but will make fake chunks \n"
						+ " transition and load slower. \n"
						+ "\n"
						+ " NOTE:\n"
						+ " This should be a last resort option."
						+ " Only change this from [0], after you have tried all of the \n"
						+ " \"GPU Upload methods\" and determined even the best stutters with yoru hardware.";
				int getGpuUploadTimeoutInMilliseconds();
				void setGpuUploadTimeoutInMilliseconds(int newTimeoutInMilliseconds);
				
				String REBUILD_TIMES_DESC = ""
						+ " How frequently should vertex buffers (geometry) be rebuilt and sent to the GPU? \n"
						+ " Higher settings may cause stuttering, but will prevent holes in the world \n";
				BufferRebuildTimes REBUILD_TIMES_DEFAULT = BufferRebuildTimes.NORMAL;
				BufferRebuildTimes getRebuildTimes();
				void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes);
			}
		}
	}
	
}
