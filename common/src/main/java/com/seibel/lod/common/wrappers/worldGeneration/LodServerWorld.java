/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.common.wrappers.worldGeneration;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.common.wrappers.WrapperUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyTickList;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;


/**
 * This is a fake ServerWorld used when generating features.
 * It allows us to keep each LodChunk generation independent
 * of the actual ServerWorld, allowing
 * multithread generation.
 *
 * @author James Seibel
 * @version 7-26-2021
 */
public class LodServerWorld implements WorldGenLevel
{

    public HashMap<Heightmap.Types, Heightmap> heightmaps = new HashMap<>();

    public final ChunkAccess chunk;

    public final ServerLevel serverWorld;

    public LodServerWorld(ServerLevel newServerWorld, ChunkAccess newChunk)
    {
        chunk = newChunk;
        serverWorld = newServerWorld;
    }


    @Override
    public int getHeight(Heightmap.Types heightmapType, int x, int z)
    {
        // make sure the block position is set relative to the chunk
        x = x % LodUtil.CHUNK_WIDTH;
        x = (x < 0) ? x + 16 : x;

        z = z % LodUtil.CHUNK_WIDTH;
        z = (z < 0) ? z + 16 : z;

        return chunk.getOrCreateHeightmapUnprimed(WrapperUtil.DEFAULT_HEIGHTMAP).getFirstAvailable(x, z);
    }

    @Override
    public Biome getBiome(BlockPos pos)
    {
        return chunk.getBiomes().getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft)
    {
        return chunk.setBlockState(pos, state, false) == state;
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        return chunk.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return chunk.getFluidState(pos);
    }


    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state)
    {
        return state.test(chunk.getBlockState(pos));
    }

    @Override
    public TickList<Block> getBlockTicks()
    {
        return EmptyTickList.empty();
    }

    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus requiredStatus, boolean nonnull)
    {
        return chunk;
    }

    @Override
    public Stream<? extends StructureStart<?>> startsForFeature(SectionPos p_241827_1_, StructureFeature<?> p_241827_2_)
    {
        return serverWorld.startsForFeature(p_241827_1_, p_241827_2_);
    }

    @Override
    public TickList<Fluid> getLiquidTicks()
    {
        return EmptyTickList.empty();
    }

    @Override
    public LevelLightEngine getLightEngine()
    {
        return new LevelLightEngine(null, false, false);
    }

    @Override
    public long getSeed()
    {
        return serverWorld.getSeed();
    }

    @Override
    public RegistryAccess registryAccess()
    {
        return serverWorld.registryAccess();
    }


    /**
     * All methods below shouldn't be needed
     * and thus have been left unimplemented.
     */


    @Override
    public Random getRandom()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public void playSound(Player player, BlockPos pos, SoundEvent soundIn, SoundSource category, float volume,
                          float pitch)
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed,
                            double zSpeed)
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public BiomeManager getBiomeManager()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public int getSeaLevel()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public float getShade(Direction p_230487_1_, boolean p_230487_2_)
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public WorldBorder getWorldBorder()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean isMoving)
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock, Entity entity, int recursionLeft)
    {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public ServerLevel getLevel()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public ChunkSource getChunkSource()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos arg0)
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public LevelData getLevelData()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public void levelEvent(Player arg0, int arg1, BlockPos arg2, int arg3)
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    @Override
    public List<Entity> getEntities(Entity arg0, AABB arg1, Predicate<? super Entity> arg2)
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<? extends T> class_, AABB aABB, @Nullable Predicate<? super T> predicate) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public List<? extends Player> players()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public int getSkyDarken()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public Biome getUncachedNoiseBiome(int p_225604_1_, int p_225604_2_, int p_225604_3_)
    {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public boolean isClientSide()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public DimensionType dimensionType()
    {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public BlockEntity getBlockEntity(BlockPos p_175625_1_)
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

}
