package com.backsun.lod.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import com.backsun.lod.ModInfo;
import com.backsun.lod.enums.FogDistance;
import com.backsun.lod.enums.LodColorStyle;
import com.backsun.lod.enums.LodDetail;
import com.backsun.lod.enums.LodTemplate;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * 
 * @author James Seibel
 * @version 05-31-2021
 */
@Mod.EventBusSubscriber
public class LodConfig
{
	public static class Client
	{
		public ForgeConfigSpec.BooleanValue drawLODs;
		
		public ForgeConfigSpec.EnumValue<FogDistance> fogDistance;
		
		public ForgeConfigSpec.BooleanValue debugMode;
		
		public ForgeConfigSpec.EnumValue<LodTemplate> lodTemplate;
		
		public ForgeConfigSpec.EnumValue<LodDetail> lodDetail;
		
		public ForgeConfigSpec.EnumValue<LodColorStyle> lodColorStyle;
		
		/** this is multiplied by the default view distance
		 * to determine how far out to generate/render LODs */
		public ForgeConfigSpec.IntValue lodChunkRadiusMultiplier;
		
		Client(ForgeConfigSpec.Builder builder)
		{
	        builder.comment(ModInfo.MODNAME + " configuration settings").push("client");
	        
	        drawLODs = builder
	        		.comment("\n"
	        				+ " If false LODs will not be drawn, \n"
	        				+ " however they will still be generated \n"
	        				+ " and saved to file for later use.")
	        		.define("drawLODs", true);
	        
	        fogDistance = builder
	                .comment("\n"
	                		+ " At what distance should Fog be drawn on the LODs? \n"
	                		+ " If the fog cuts off ubruptly or you are using Optifine's \"fast\" \n"
	                		+ " fog option set this to " + FogDistance.NEAR.toString() + " or " + FogDistance.FAR.toString() + ".")
	                .defineEnum("fogDistance", FogDistance.NEAR_AND_FAR);
	        
	        debugMode = builder
	                .comment("\n"
	                		+ " If false the LODs will draw with their normal world colors. \n"
	                		+ " If true they will draw as a black and white checkerboard. \n"
	                		+ " This can be used for debugging or imagining you are playing a \n"
	                		+ " giant game of chess ;)")
	                .define("drawCheckerBoard", false);
	        
	        lodTemplate = builder
	                .comment("\n"
	                		+ " How should the LODs be drawn? \n"
	                		+ " " + LodTemplate.CUBIC.toString() + ": LOD Chunks are drawn as rectangular prisms (boxes). \n"
	                		+ " " + LodTemplate.TRIANGULAR.toString() + ": LOD Chunks smoothly transition between other. \n"
	                		+ " " + LodTemplate.DYNAMIC.toString() + ": LOD Chunks smoothly transition between other, "
	                		+ " " + "         unless a neighboring chunk is at a significantly different height. ")
	                .defineEnum("lodTemplate", LodTemplate.CUBIC);
	        
	        lodDetail = builder
	                .comment("\n"
	                		+ " How detailed should the LODs be? \n"
	                		+ " " + LodDetail.SINGLE.toString() + ": render 1 LOD for each Chunk. \n"
            				+ " " + LodDetail.DOUBLE.toString() + ": render 4 LODs for each Chunk.")
	                .defineEnum("lodGeometryQuality", LodDetail.SINGLE);
	        
	        lodColorStyle = builder
	                .comment("\n"
	                		+ " How should the LODs be colored? \n"
	                		+ " " + LodColorStyle.TOP.toString() + ": Use the color from the top of the LOD chunk for all sides. \n"
	                		+ " " + LodColorStyle.INDIVIDUAL_SIDES.toString() + ": For each side of the LOD use the color corresponding to that side. ")
	                .defineEnum("lodColorStyle", LodColorStyle.TOP);
	        
	        lodChunkRadiusMultiplier = builder
	                .comment("\n"
	                		+ " This is multiplied by the default view distance \n"
	                		+ " to determine how far out to generate/render LODs. \n"
	                		+ " A value of 2 means that there is 1 render distance worth \n"
	                		+ " of LODs in each cardinal direction.")
	                .defineInRange("lodChunkRadiusMultiplier", 6, 2, 1023);
	        
	        builder.pop();
	    }
	}
	
	

    /**
     * {@link Path} to the configuration file of this mod
     */
    private static final Path CONFIG_PATH =
            Paths.get("config", ModInfo.MODID + ".toml");

    public static final ForgeConfigSpec clientSpec;
    public static final Client CLIENT;
    static {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        clientSpec = specPair.getRight();
        CLIENT = specPair.getLeft();
        
        // setup the config file
        CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_PATH)
                .writingMode(WritingMode.REPLACE)
                .build();
        config.load();
        config.save();
        clientSpec.setConfig(config);
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
