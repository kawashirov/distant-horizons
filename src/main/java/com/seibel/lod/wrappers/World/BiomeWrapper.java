package com.seibel.lod.wrappers.World;

import com.seibel.lod.util.ColorUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.Block.BlockColorWrapper;
import com.seibel.lod.wrappers.Block.BlockPosWrapper;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColors;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


//This class wraps the minecraft BlockPos.Mutable (and BlockPos) class
public class BiomeWrapper
{
	
	public static final ConcurrentMap<Biome, BiomeWrapper> biomeWrapperMap = new ConcurrentHashMap<>();
	private final Biome biome;
	
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
		
		switch (biome.getBiomeCategory())
		{
		
		case NETHER:
			colorInt = Blocks.NETHERRACK.defaultBlockState().materialColor.col;
			break;
		
		case THEEND:
			colorInt = Blocks.END_STONE.defaultBlockState().materialColor.col;
			break;
		
		case BEACH:
		case DESERT:
			colorInt = Blocks.SAND.defaultBlockState().materialColor.col;
			break;
		
		case EXTREME_HILLS:
			colorInt = Blocks.STONE.defaultMaterialColor().col;
			break;
		
		case MUSHROOM:
			colorInt = MaterialColor.COLOR_LIGHT_GRAY.col;
			break;
		
		case ICY:
			colorInt = Blocks.SNOW.defaultMaterialColor().col;
			break;
		
		case MESA:
			colorInt = Blocks.RED_SAND.defaultMaterialColor().col;
			break;
		
		case OCEAN:
		case RIVER:
			colorInt = biome.getWaterColor();
			break;
		
		case NONE:
		case FOREST:
		case TAIGA:
		case JUNGLE:
		case PLAINS:
		case SAVANNA:
		case SWAMP:
		default:
			Color tmp = LodUtil.intToColor(biome.getGrassColor(x, z));
			tmp = tmp.darker();
			colorInt = LodUtil.colorToInt(tmp);
			break;
			
		}
		
		return colorInt;
	}
	
	public int getGrassTint(int x, int z)
	{
		return biome.getGrassColor(x, z);
	}
	
	public int getFolliageTint()
	{
		return biome.getFoliageColor();
	}
	
	public int getWaterTint()
	{
		return biome.getWaterColor();
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
