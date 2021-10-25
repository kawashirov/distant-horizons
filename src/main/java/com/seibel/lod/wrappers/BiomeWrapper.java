package com.seibel.lod.wrappers;

import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.LodUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.world.biome.Biome;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


//This class wraps the minecraft BlockPos.Mutable (and BlockPos) class
public class BiomeWrapper
{
	
	public static final ConcurrentMap<Biome, BiomeWrapper> biomeWrapperMap = new ConcurrentHashMap<>();
	private Biome biome;
	
	public BiomeWrapper(Biome biome)
	{
		this.biome = biome;
	}
	
	static public BiomeWrapper getBiomeWrapper(Biome biome)
	{
		//first we check if the biome has already been wrapped
		if(biomeWrapperMap.containsKey(biome) && biomeWrapperMap.get(biome) != null)
			return biomeWrapperMap.get(biome);
		
		
		//if it hasn't been created yet, we create it and save it in the map
		BiomeWrapper biomeWrapper = new BiomeWrapper(biome);
		biomeWrapperMap.put(biome, biomeWrapper);
		
		//we return the newly created wrapper
		return biomeWrapper;
	}
	
	
	/** Returns a color int for the given biome. */
	public int getColorForBiome(int x, int z)
	{
		int colorInt;
		int color;
		int tint;
		
		switch (biome.getBiomeCategory())
		{
		
		case NETHER:
			colorInt = BlockWrapper.getBlockWrapper(Blocks.NETHERRACK).getColor();
			break;
		
		case THEEND:
			colorInt = BlockWrapper.getBlockWrapper(Blocks.END_STONE).getColor();
			break;
		
		case BEACH:
		case DESERT:
			colorInt = BlockWrapper.getBlockWrapper(Blocks.SAND).getColor();
			break;
		
		case EXTREME_HILLS:
			colorInt = BlockWrapper.getBlockWrapper(Blocks.STONE).getColor();
			break;
		
		case MUSHROOM:
			colorInt = BlockWrapper.getBlockWrapper(Blocks.MYCELIUM).getColor();
			break;
		
		case ICY:
			colorInt = BlockWrapper.getBlockWrapper(Blocks.SNOW).getColor();
			break;
		
		case MESA:
			colorInt = BlockWrapper.getBlockWrapper(Blocks.RED_SAND).getColor();
			break;
		
		case OCEAN:
		case RIVER:
			colorInt = biome.getWaterColor();
			break;
			
		case SWAMP:
		case FOREST:
			color = BlockWrapper.getBlockWrapper(Blocks.OAK_LEAVES).getColor();
			tint = biome.getFoliageColor();
			colorInt = ColorUtil.multiplyRGBcolors(color, tint);
			break;
			
		case TAIGA:
			color = BlockWrapper.getBlockWrapper(Blocks.SPRUCE_LEAVES).getColor();
			tint = biome.getFoliageColor();
			colorInt = ColorUtil.multiplyRGBcolors(color, tint);
			break;
			
		case JUNGLE:
			color = BlockWrapper.getBlockWrapper(Blocks.JUNGLE_LEAVES).getColor();
			tint = biome.getFoliageColor();
			colorInt = ColorUtil.multiplyRGBcolors(color, tint);
			break;
			
		default:
		case NONE:
		case PLAINS:
		case SAVANNA:
			color = BlockWrapper.getBlockWrapper(Blocks.GRASS_BLOCK).getColor();
			tint = biome.getGrassColor(x,z);
			colorInt = ColorUtil.multiplyRGBcolors(color, tint);
			break;
			
		}
		
		return colorInt;
	}
	
	@Override public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof BiomeWrapper))
			return false;
		BiomeWrapper that = (BiomeWrapper) o;
		return Objects.equals(biome, that.biome);
	}
	
	@Override public int hashCode()
	{
		return Objects.hash(biome);
	}
	
}
