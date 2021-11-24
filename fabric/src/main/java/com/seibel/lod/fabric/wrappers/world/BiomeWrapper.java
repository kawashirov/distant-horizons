package com.seibel.lod.fabric.wrappers.world;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.fabric.wrappers.block.BlockColorSingletonWrapper;
import com.seibel.lod.fabric.wrappers.block.BlockColorWrapper;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;


/**
 * This class wraps the minecraft BlockPos.Mutable (and BlockPos) class
 * 
 * @author James Seibel
 * @version 11-15-2021
 */
public class BiomeWrapper implements IBiomeWrapper
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
	@Override
	public int getColorForBiome(int x, int z)
	{
		int colorInt;
		int tintValue = 0;
		
		switch (biome.getBiomeCategory())
		{
		
		case NETHER:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.NETHERRACK).getColor();
			break;
		
		case THEEND:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.END_STONE).getColor();
			break;
		
		case BEACH:
		case DESERT:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.SAND).getColor();
			break;
		
		case EXTREME_HILLS:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.STONE).getColor();
			break;
		
		case MUSHROOM:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.MYCELIUM).getColor();
			break;
		
		case ICY:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.SNOW).getColor();
			break;
		
		case MESA:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.RED_SAND).getColor();
			break;
		
		case OCEAN:
		case RIVER:
			colorInt = BlockColorSingletonWrapper.INSTANCE.getWaterColor().getColor();
			tintValue = biome.getWaterColor();
			break;
		
		case PLAINS:
		case SAVANNA:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.GRASS_BLOCK).getColor();
			tintValue = biome.getGrassColor(x, z);
			colorInt = ColorUtil.multiplyRGBcolors(colorInt,tintValue);
			break;
		
		case TAIGA:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.SPRUCE_LEAVES).getColor();
			tintValue = biome.getFoliageColor();
			colorInt = ColorUtil.multiplyRGBcolors(colorInt,tintValue);
			break;
		case JUNGLE:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.JUNGLE_LEAVES).getColor();
			tintValue = biome.getFoliageColor();
			colorInt = ColorUtil.multiplyRGBcolors(colorInt,tintValue);
			break;
		
		case NONE:
		default:
		case SWAMP:
		case FOREST:
			colorInt = BlockColorWrapper.getBlockColorWrapper(Blocks.OAK_LEAVES).getColor();
			tintValue = biome.getFoliageColor();
			colorInt = ColorUtil.multiplyRGBcolors(colorInt,tintValue);
			break;
		}
		
		return colorInt;
	}
	
	@Override
	public int getGrassTint(int x, int z)
	{
		return biome.getGrassColor(x, z);
	}
	
	@Override
	public int getFolliageTint()
	{
		return biome.getFoliageColor();
	}
	
	@Override
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
