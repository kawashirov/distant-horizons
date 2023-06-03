package com.seibel.lod.common.wrappers.world;

import com.seibel.lod.api.enums.worldGeneration.EDhApiLevelType;
import com.seibel.lod.api.interfaces.world.IDhApiDimensionTypeWrapper;
import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.block.BiomeWrapper;
import com.seibel.lod.common.wrappers.block.BlockStateWrapper;
import com.seibel.lod.common.wrappers.block.cache.ClientBlockDetailMap;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @version 2023-6-3
 */
public class ClientLevelWrapper implements IClientLevelWrapper
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(ClientLevelWrapper.class.getSimpleName());
    private static final ConcurrentHashMap<ClientLevel, ClientLevelWrapper>
            levelWrapperMap = new ConcurrentHashMap<>();

    public static ClientLevelWrapper getWrapper(ClientLevel level) {
        return levelWrapperMap.computeIfAbsent(level, ClientLevelWrapper::new);
    }
    public static void closeWrapper(ClientLevel level)
    {
        levelWrapperMap.remove(level);
    }

    private ClientLevelWrapper(ClientLevel level) {
        this.level = level;
    }
    final ClientLevel level;
    ClientBlockDetailMap blockMap = new ClientBlockDetailMap(this);
    @Nullable
    @Override
	public IServerLevelWrapper tryGetServerSideWrapper()
	{
		try
		{
			// commented out because this breaks when traveling between dimensions,
			// serverPlayer.getLevel() will return the previously loaded level, which causes issues 
//			PlayerList serverPlayerList = MinecraftClientWrapper.INSTANCE.mc.getSingleplayerServer().getPlayerList();
//			ServerPlayer serverPlayer = serverPlayerList.getPlayer(MinecraftClientWrapper.INSTANCE.mc.player.getUUID());
//			return ServerLevelWrapper.getWrapper(serverPlayer.getLevel());
			
			
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
			LOGGER.error("Failed to get server side wrapper for client level: "+level);
			return null;
		}
	}
    public static void cleanCheck() {
        if (!levelWrapperMap.isEmpty()) {
            LOGGER.warn("{} client levels havn't been freed!", levelWrapperMap.size());
            levelWrapperMap.clear();
        }
    }

    @Override
    public int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper blockState) {
        return blockMap.getColor(((BlockStateWrapper)blockState).blockState,
                (BiomeWrapper)biome, pos);
    }

    @Override
    public IDhApiDimensionTypeWrapper getDimensionType()
    {
        return DimensionTypeWrapper.getDimensionTypeWrapper(level.dimensionType());
    }
	
	@Override 
	public EDhApiLevelType getLevelType() { return EDhApiLevelType.CLIENT_LEVEL; }
	
	@Override
    public int getBlockLight(int x, int y, int z)
    {
        return level.getBrightness(LightLayer.BLOCK, new BlockPos(x,y,z));
    }

    @Override
    public int getSkyLight(int x, int y, int z)
    {
        return level.getBrightness(LightLayer.SKY, new BlockPos(x,y,z));
    }

    public ClientLevel getLevel()
    {
        return level;
    }

    @Override
    public boolean hasCeiling() {
        return level.dimensionType().hasCeiling();
    }

    @Override
    public boolean hasSkyLight() {
        return level.dimensionType().hasSkyLight();
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinHeight()
    {
        #if PRE_MC_1_17_1
        return 0;
        #else
        return level.getMinBuildHeight();
        #endif
    }

    @Override
    public IChunkWrapper tryGetChunk(DhChunkPos pos) {
        if (!level.hasChunk(pos.x, pos.z)) return null;
        ChunkAccess chunk = level.getChunk(pos.getX(), pos.getZ(), ChunkStatus.EMPTY, false);
        if (chunk == null) return null;
        return new ChunkWrapper(chunk, level, this);
    }

    @Override
    public boolean hasChunkLoaded(int chunkX, int chunkZ) {
        ChunkSource source = level.getChunkSource();
        return source.hasChunk(chunkX, chunkZ);
    }

    @Override
    public IBlockStateWrapper getBlockState(DhBlockPos pos) {
        return BlockStateWrapper.fromBlockState(level.getBlockState(McObjectConverter.Convert(pos)));
    }

    @Override
    public IBiomeWrapper getBiome(DhBlockPos pos) {
        return BiomeWrapper.getBiomeWrapper(level.getBiome(McObjectConverter.Convert(pos)));
    }

    @Override
    public ClientLevel getWrappedMcObject_UNSAFE()
    {
        return level;
    }

    @Override
    public String toString() {
        return "Wrapped{" + level.toString() + "@" + getDimensionType().getDimensionName() + "}";
    }

}
