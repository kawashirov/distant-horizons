package com.seibel.lod.wrappers.World;

import com.seibel.lod.wrappers.Block.BlockPosWrapper;

import net.minecraft.client.renderer.BiomeColors;


public class BiomeColorWrapper
{
	
	public static int getGrassColor(WorldWrapper worldWrapper, BlockPosWrapper blockPosWrapper)
	{
		return BiomeColors.getAverageGrassColor(worldWrapper.getWorld(), blockPosWrapper.getBlockPos());
	}
	public static int getWaterColor(WorldWrapper worldWrapper, BlockPosWrapper blockPosWrapper)
	{
		
		return BiomeColors.getAverageWaterColor(worldWrapper.getWorld(), blockPosWrapper.getBlockPos());
	}
	public static int getFoliageColor(WorldWrapper worldWrapper, BlockPosWrapper blockPosWrapper)
	{
		
		return BiomeColors.getAverageFoliageColor(worldWrapper.getWorld(), blockPosWrapper.getBlockPos());
	}
}
