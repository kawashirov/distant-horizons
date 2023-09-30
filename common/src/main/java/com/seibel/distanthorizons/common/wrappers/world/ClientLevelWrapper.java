package com.seibel.distanthorizons.common.wrappers.world;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiLevelType;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiDimensionTypeWrapper;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;
import com.seibel.distanthorizons.common.wrappers.block.cache.ClientBlockDetailMap;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.level.IKeyedClientLevelManager;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public class ClientLevelWrapper implements IClientLevelWrapper
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(ClientLevelWrapper.class.getSimpleName());
	private static final ConcurrentHashMap<ClientLevel, ClientLevelWrapper> LEVEL_WRAPPER_BY_CLIENT_LEVEL = new ConcurrentHashMap<>();
	private static final IKeyedClientLevelManager KEYED_CLIENT_LEVEL_MANAGER = SingletonInjector.INSTANCE.get(IKeyedClientLevelManager.class);
	
	private final ClientLevel level;
	private final ClientBlockDetailMap blockMap = new ClientBlockDetailMap(this);
	
	
	
	//=============//
	// constructor //
	//=============//
	
	protected ClientLevelWrapper(ClientLevel level) { this.level = level; }
	
	
	
	//===============//
	// wrapper logic //
	//===============//
	
	public static IClientLevelWrapper getWrapper(ClientLevel level)
	{
		// used if the client is connected to a server that defines the currently loaded level
		if (KEYED_CLIENT_LEVEL_MANAGER.getUseOverrideWrapper())
		{
			return KEYED_CLIENT_LEVEL_MANAGER.getOverrideWrapper();
		}
		
		return getWrapperIgnoringOverride(level);
	}
	public static IClientLevelWrapper getWrapperIgnoringOverride(ClientLevel level)
	{
		return LEVEL_WRAPPER_BY_CLIENT_LEVEL.computeIfAbsent(level, ClientLevelWrapper::new);
	}
	
	@Nullable
	@Override
	public IServerLevelWrapper tryGetServerSideWrapper()
	{
		try
		{
			Iterable<ServerLevel> serverLevels = MinecraftClientWrapper.INSTANCE.mc.getSingleplayerServer().getAllLevels();
			
			// attempt to find the server level with the same dimension type
			// TODO this assumes only one level per dimension type, the SubDimensionLevelMatcher will need to be added for supporting multiple levels per dimension
			ServerLevelWrapper foundLevelWrapper = null;
			
			// TODO: Surely there is a more efficient way to write this code
			for (ServerLevel serverLevel : serverLevels)
			{
				if (serverLevel.dimension() == this.level.dimension())
				{
					foundLevelWrapper = ServerLevelWrapper.getWrapper(serverLevel);
					break;
				}
			}
			
			return foundLevelWrapper;
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to get server side wrapper for client level: " + level);
			return null;
		}
	}
	
	
	
	//====================//
	// base level methods //
	//====================//
	
	@Override
	public int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper blockState)
	{
		return this.blockMap.getColor(((BlockStateWrapper) blockState).blockState, (BiomeWrapper) biome, pos);
	}
	
	@Override
	public IDimensionTypeWrapper getDimensionType() { return DimensionTypeWrapper.getDimensionTypeWrapper(this.level.dimensionType()); }
	
	@Override
	public EDhApiLevelType getLevelType() { return EDhApiLevelType.CLIENT_LEVEL; }
	
	public ClientLevel getLevel() { return this.level; }
	
	@Override
	public boolean hasCeiling() { return this.level.dimensionType().hasCeiling(); }
	
	@Override
	public boolean hasSkyLight() { return this.level.dimensionType().hasSkyLight(); }
	
	@Override
	public int getHeight() { return this.level.getHeight(); }
	
	@Override
	public int getMinHeight()
	{
        #if PRE_MC_1_17_1
        return 0;
        #else
		return this.level.getMinBuildHeight();
        #endif
	}
	
	@Override
	public IChunkWrapper tryGetChunk(DhChunkPos pos)
	{
		if (!this.level.hasChunk(pos.x, pos.z))
		{
			return null;
		}
		
		ChunkAccess chunk = this.level.getChunk(pos.x, pos.z, ChunkStatus.EMPTY, false);
		if (chunk == null)
		{
			return null;
		}
		
		return new ChunkWrapper(chunk, this.level, this);
	}
	
	@Override
	public boolean hasChunkLoaded(int chunkX, int chunkZ)
	{
		ChunkSource source = this.level.getChunkSource();
		return source.hasChunk(chunkX, chunkZ);
	}
	
	@Override
	public IBlockStateWrapper getBlockState(DhBlockPos pos)
	{
		return BlockStateWrapper.fromBlockState(this.level.getBlockState(McObjectConverter.Convert(pos)), this);
	}
	
	@Override
	public IBiomeWrapper getBiome(DhBlockPos pos) { return BiomeWrapper.getBiomeWrapper(this.level.getBiome(McObjectConverter.Convert(pos)), this); }
	
	@Override
	public ClientLevel getWrappedMcObject() { return this.level; }
	
	@Override
	public void onUnload() { LEVEL_WRAPPER_BY_CLIENT_LEVEL.remove(this.level); }
	
	@Override
	public String toString()
	{
		if (this.level == null)
		{
			return "Wrapped{null}";
		}
		
		return "Wrapped{" + this.level.toString() + "@" + this.getDimensionType().getDimensionName() + "}";
	}
	
}
