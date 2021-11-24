package com.seibel.lod.fabric.wrappers.world;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * 
 * @author ??
 * @version 11-15-2021
 */
public class DimensionTypeWrapper implements IDimensionTypeWrapper
{
	private static final ConcurrentMap<DimensionType, DimensionTypeWrapper> dimensionTypeWrapperMap = new ConcurrentHashMap<>();
	private final DimensionType dimensionType;
	
	public DimensionTypeWrapper(DimensionType dimensionType)
	{
		this.dimensionType = dimensionType;
	}
	
	public static DimensionTypeWrapper getDimensionTypeWrapper(DimensionType dimensionType)
	{
		//first we check if the biome has already been wrapped
		if(dimensionTypeWrapperMap.containsKey(dimensionType) && dimensionTypeWrapperMap.get(dimensionType) != null)
			return dimensionTypeWrapperMap.get(dimensionType);
		
		
		//if it hasn't been created yet, we create it and save it in the map
		DimensionTypeWrapper dimensionTypeWrapper = new DimensionTypeWrapper(dimensionType);
		dimensionTypeWrapperMap.put(dimensionType, dimensionTypeWrapper);
		
		//we return the newly created wrapper
		return dimensionTypeWrapper;
	}
	
	@Override
	public String getDimensionName()
	{
		return dimensionType.effectsLocation().getPath();
	}
	
	@Override
	public boolean hasCeiling()
	{
		return dimensionType.hasCeiling();
	}

	@Override
	public boolean hasSkyLight()
	{
		return dimensionType.hasSkyLight();
	}
}
