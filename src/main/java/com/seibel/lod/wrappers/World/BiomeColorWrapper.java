package com.seibel.lod.wrappers.World;

import com.seibel.lod.wrappers.Block.BlockPosWrapper;
import net.minecraft.world.biome.BiomeColors;


public class BiomeColorWrapper
{
	
	public static int getGrassColor(LevelWrapper levelWrapper, BlockPosWrapper blockPosWrapper)
	{
		return BiomeColors.getAverageGrassColor(levelWrapper.getWorld(), blockPosWrapper.getBlockPos());
	}
	public static int getWaterColor(LevelWrapper levelWrapper, BlockPosWrapper blockPosWrapper)
	{
		
		return BiomeColors.getAverageWaterColor(levelWrapper.getWorld(), blockPosWrapper.getBlockPos());
	}
	public static int getFoliageColor(LevelWrapper levelWrapper, BlockPosWrapper blockPosWrapper)
	{
		
		return BiomeColors.getAverageFoliageColor(levelWrapper.getWorld(), blockPosWrapper.getBlockPos());
	}
}
