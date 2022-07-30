package com.seibel.lod.common.wrappers.world;

import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.block.BiomeWrapper;
import com.seibel.lod.common.wrappers.block.BlockStateWrapper;
import com.seibel.lod.common.wrappers.block.cache.ClientBlockDetailMap;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.lod.core.api.internal.a7.ClientApi;
import com.seibel.lod.core.api.internal.a7.ServerApi;
import com.seibel.lod.core.enums.ELevelType;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public class ClientLevelWrapper implements IClientLevelWrapper
{
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
    public IServerLevelWrapper tryGetServerSideWrapper() {
        try {
            return ServerLevelWrapper.getWrapper(MinecraftClientWrapper.INSTANCE.mc.getSingleplayerServer().getPlayerList()
                    .getPlayer(MinecraftClientWrapper.INSTANCE.mc.player.getUUID()).getLevel());
        } catch (Exception e) {
            ClientApi.LOGGER.error("Failed to get server side wrapper for client level {}.", level);
            return null;
        }
    }
    public static void cleanCheck() {
        if (!levelWrapperMap.isEmpty()) {
            ServerApi.LOGGER.warn("{} client levels havn't been freed!", levelWrapperMap.size());
            levelWrapperMap.clear();
        }
    }

    @Override
    public int computeBaseColor(DHBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper blockState) {
        return blockMap.getColor(((BlockStateWrapper)blockState).blockState,
                (BiomeWrapper)biome, pos);
    }

    @Override
    public DimensionTypeWrapper getDimensionType()
    {
        return DimensionTypeWrapper.getDimensionTypeWrapper(level.dimensionType());
    }

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
    public short getMinHeight()
    {
        #if PRE_MC_1_17_1
        return (short) 0;
        #else
        return (short) level.getMinBuildHeight();
        #endif
    }

    @Override
    public IChunkWrapper tryGetChunk(DHChunkPos pos) {
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
    public IBlockStateWrapper getBlockState(DHBlockPos pos) {
        return BlockStateWrapper.fromBlockState(level.getBlockState(McObjectConverter.Convert(pos)));
    }

    @Override
    public IBiomeWrapper getBiome(DHBlockPos pos) {
        return BiomeWrapper.getBiomeWrapper(level.getBiome(McObjectConverter.Convert(pos)));
    }

    @Override
    public ClientLevel unwrapLevel()
    {
        return level;
    }

    @Override
    public String toString() {
        return "Wrapped{" + level.toString() + "@" + getDimensionType().getDimensionName() + "}";
    }

}
