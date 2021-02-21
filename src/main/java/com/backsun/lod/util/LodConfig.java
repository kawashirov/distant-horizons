package com.backsun.lod.util;

import org.apache.commons.lang3.tuple.Pair;

import com.backsun.lod.ModInfo;
import com.backsun.lod.util.enums.FogDistance;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

/**
 * 
 * @author James Seibel
 * @version 02-14-2021
 */
@Mod.EventBusSubscriber
public class LodConfig
{
	public static final LodConfig.Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;
    
    // create the required variables
    static
    {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(LodConfig.Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
	
	
	public static class Common
	{
		public ForgeConfigSpec.BooleanValue drawLODs;
		
		public ForgeConfigSpec.EnumValue<FogDistance> fogDistance;
		
		public ForgeConfigSpec.BooleanValue drawCheckerBoard;
		
		
		Common(ForgeConfigSpec.Builder builder)
		{
	        builder.comment(ModInfo.MODNAME + " configuration settings").push("common");
	        
	        drawLODs = builder
	        		.comment("If true LODs will be drawn, if false LODs will "
	        				+ "not be rendered. However they will "
	        				+ "still be generated.")
	        		.define("drawLODs", true);
	        
	        fogDistance = builder
	                .comment("At what distance should Fog be drawn on the LODs?")
	                .defineEnum("fogDistance", FogDistance.NEAR_AND_FAR);
	        
	        drawCheckerBoard = builder
	                .comment("If false the LODs will draw with their normal world colors. "
	                		+ "If true they will draw as a black and white checkerboard. "
	                		+ "This can be used for debugging or imagining you are playing a "
	                		+ "giant game of chess ;)")
	                .define("drawCheckerBoard", false);
	        
	        builder.pop();
	    }
	}
	
}
