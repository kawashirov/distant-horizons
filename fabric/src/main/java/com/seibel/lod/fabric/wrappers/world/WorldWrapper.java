package com.seibel.lod.fabric.wrappers.world;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.core.enums.WorldType;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.fabric.wrappers.block.BlockPosWrapper;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 
 * @author James Seibel
 * @author ??
 * @version 11-15-2021
 */
public class WorldWrapper implements IWorldWrapper
{
	private static final ConcurrentMap<Level, WorldWrapper> worldWrapperMap = new ConcurrentHashMap<>();
	private final Level world;
	public final WorldType worldType;
	
	
	public WorldWrapper(Level newWorld)
	{
		world = newWorld;
		
		if (world.getClass() == ServerLevel.class)
			worldType = WorldType.ServerWorld;	
		else if (world.getClass() == ClientLevel.class)
			worldType = WorldType.ClientWorld;	
		else
			worldType = WorldType.Unknown;
	}
	
	
	
	public static WorldWrapper getWorldWrapper(Level world)
	{
		if (world == null) return null;
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

	@Override
	public WorldType getWorldType() {
		return worldType;
	}
	
	@Override
	public DimensionTypeWrapper getDimensionType()
	{
		return DimensionTypeWrapper.getDimensionTypeWrapper(world.dimensionType());
	}

	@Override
	public int getBlockLight(AbstractBlockPosWrapper blockPos)
	{
		return world.getLightEngine().blockEngine.getLightValue(((BlockPosWrapper) blockPos).getBlockPos());
	}
	
	@Override
	public int getSkyLight(AbstractBlockPosWrapper blockPos)
	{
		return world.getLightEngine().skyEngine.getLightValue(((BlockPosWrapper) blockPos).getBlockPos());
	}
	
	@Override
	public BiomeWrapper getBiome(AbstractBlockPosWrapper blockPos)
	{
		return BiomeWrapper.getBiomeWrapper(world.getBiome(((BlockPosWrapper) blockPos).getBlockPos()));
	}
	
	public Level getWorld()
	{
		return world;
	}
	
	@Override
	public boolean hasCeiling()
	{
		return world.dimensionType().hasCeiling();
	}
	
	@Override
	public boolean hasSkyLight()
	{
		return world.dimensionType().hasSkyLight();
	}
	
	@Override
	public boolean isEmpty()
	{
		return world == null;
	}
	
	@Override
	public int getHeight()
	{
		return world.getHeight();
	}
	
	/** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
	@Override
	public File getSaveFolder() throws UnsupportedOperationException
	{
		if (worldType != WorldType.ServerWorld)
			throw new UnsupportedOperationException("getSaveFolder can only be called for ServerWorlds.");
		
		ServerChunkCache chunkSource = ((ServerLevel) world).getChunkSource();
		return chunkSource.dataStorage.dataFolder;
	}
	
	
	/** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
	public ServerLevel getServerWorld() throws UnsupportedOperationException
	{
		if (worldType != WorldType.ServerWorld)
			throw new UnsupportedOperationException("getSaveFolder can only be called for ServerWorlds.");
		
		return (ServerLevel) world;
	}
	
	@Override
	public int getSeaLevel()
	{
		// TODO this is depreciated, what should we use instead?
		return world.getSeaLevel();
	}
	
	
}
