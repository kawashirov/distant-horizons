package com.seibel.lod.wrappers.World;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.wrappers.Block.BlockPosWrapper;

import net.minecraft.world.level.Level;

public class WorldWrapper
{
	private static final ConcurrentMap<Level, WorldWrapper> worldWrapperMap = new ConcurrentHashMap<>();
	private Level world;
	
	public WorldWrapper(Level world)
	{
		this.world = world;
	}
	
	
	public static WorldWrapper getWorldWrapper(Level world)
	{
		//first we check if the biome has already been wrapped
		if(worldWrapperMap.containsKey(world) && worldWrapperMap.get(world) != null)
			return worldWrapperMap.get(world);
		
		
		//if it hasn't been created yet, we create it and save it in the map
		WorldWrapper worldWrapper = new WorldWrapper(world);
		worldWrapperMap.put(world, worldWrapper);
		
		//we return the newly created wrapper
		return worldWrapper;
	}
	
	public static void clearMap()
	{
		worldWrapperMap.clear();
	}
	
	public DimensionTypeWrapper getDimensionType()
	{
		return DimensionTypeWrapper.getDimensionTypeWrapper(world.dimensionType());
	}
	
	public int getBlockLight(BlockPosWrapper blockPos)
	{
		return world.getLightEngine().blockEngine.getLightValue(blockPos.getBlockPos());
	}
	
	public int getSkyLight(BlockPosWrapper blockPos)
	{
		return world.getLightEngine().skyEngine.getLightValue(blockPos.getBlockPos());
	}
	
	public BiomeWrapper getBiome(BlockPosWrapper blockPos)
	{
		return BiomeWrapper.getBiomeWrapper(world.getBiome(blockPos.getBlockPos()));
	}
	
	public Level getWorld()
	{
		return world;
	}
	
	public boolean hasCeiling()
	{
		return world.dimensionType().hasCeiling();
	}
	
	public boolean hasSkyLight()
	{
		return world.dimensionType().hasSkyLight();
	}
	
	public boolean isEmpty()
	{
		return world == null;
	}
}
