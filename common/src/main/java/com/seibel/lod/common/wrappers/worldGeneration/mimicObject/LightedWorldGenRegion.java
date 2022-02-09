package com.seibel.lod.common.wrappers.worldGeneration.mimicObject;

import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment.EmptyChunkGenerator;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.LightGenerationMode;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class LightedWorldGenRegion extends WorldGenRegion {
	public final WorldGenLevelLightEngine light;
	public final LightGenerationMode lightMode;
	public final EmptyChunkGenerator generator;
	public final int writeRadius;
	public final int size;
	private final ChunkPos firstPos;
	private final List<ChunkAccess> cache;
	private final StructureFeatureManager structFeat;
	Long2ObjectOpenHashMap<ChunkAccess> chunkMap = new Long2ObjectOpenHashMap<ChunkAccess>();
	private ChunkPos overrideCenterPos = null;
	
	public void setOverrideCenter(ChunkPos pos) {overrideCenterPos = pos;}
	@Override
	public ChunkPos getCenter() {
		return overrideCenterPos==null ? super.getCenter() : overrideCenterPos;
	}

	public LightedWorldGenRegion(ServerLevel serverLevel, WorldGenLevelLightEngine lightEngine,
			StructureFeatureManager structFeat, List<ChunkAccess> list, ChunkStatus chunkStatus, int i,
			LightGenerationMode lightMode, EmptyChunkGenerator generator) {
		super(serverLevel, list, chunkStatus, i);
		this.lightMode = lightMode;
		this.firstPos = list.get(0).getPos();
		this.generator = generator;
		this.structFeat = structFeat;
		light = lightEngine;
		writeRadius = i;
		cache = list;
		size = Mth.floor(Math.sqrt(list.size()));
	}

	// Bypass BCLib mixin overrides.
    @Override
    public boolean ensureCanWrite(BlockPos blockPos) {
        int i = SectionPos.blockToSectionCoord(blockPos.getX());
        int j = SectionPos.blockToSectionCoord(blockPos.getZ());
        ChunkPos chunkPos = this.getCenter();
        int k = Math.abs(chunkPos.x - i);
        int l = Math.abs(chunkPos.z - j);
        if (k > this.writeRadius || l > this.writeRadius) {
            return false;
        }
        return true;
    }
    
	@Override
	public Stream<? extends StructureStart<?>> startsForFeature(SectionPos sectionPos,
			StructureFeature<?> structureFeature) {
		return structFeat.startsForFeature(sectionPos, structureFeature);
	}

	// Skip updating the related tile entities
	@Override
	public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
		ChunkAccess chunkAccess = this.getChunk(blockPos);
		if (chunkAccess instanceof LevelChunk)
			return true;
		chunkAccess.setBlockState(blockPos, blockState, false);
		// This is for post ticking for water on gen and stuff like that. Not enabled
		// for now.
		// if (blockState.hasPostProcess(this, blockPos))
		// this.getChunk(blockPos).markPosForPostprocessing(blockPos);
		return true;
	}

	// Skip Dropping the item on destroy
	@Override
	public boolean destroyBlock(BlockPos blockPos, boolean bl, @Nullable Entity entity, int i) {
		BlockState blockState = this.getBlockState(blockPos);
		if (blockState.isAir()) {
			return false;
		}
		return this.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3, i);
	}

	// Skip BlockEntity stuff. It aren't really needed
	@Override
	public BlockEntity getBlockEntity(BlockPos blockPos) {
		return null;
	}

	// Skip BlockEntity stuff. It aren't really needed
	@Override
	public boolean addFreshEntity(Entity entity) {
		return true;
	}

	// Allays have empty chunks even if it's outside the worldGenRegion
	// @Override
	// public boolean hasChunk(int i, int j) {
	// return true;
	// }

	// Override to ensure no other mod mixins cause skipping the overrided
	// getChunk(...)
	@Override
	public ChunkAccess getChunk(int i, int j) {
		return this.getChunk(i, j, ChunkStatus.EMPTY);
	}

	// Override to ensure no other mod mixins cause skipping the overrided
	// getChunk(...)
	@Override
	public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus) {
		return this.getChunk(i, j, chunkStatus, true);
	}

	// Use this instead of super.getChunk() to bypass C2ME concurrency checks
	private ChunkAccess superGetChunk(int x, int z, ChunkStatus cs) {
		int k = x - firstPos.x;
		int l = z - firstPos.z;
		return cache.get(k + l * size);
	}

	// Use this instead of super.hasChunk() to bypass C2ME concurrency checks
	private boolean superHasChunk(int x, int z) {
		int k = x - firstPos.x;
		int l = z - firstPos.z;
		return l >= 0 && l < size && k >= 0 && k < size;
	}

	// Allow creating empty chunks even if it's outside the worldGenRegion
	@Override
	@Nullable
	public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl) {
		ChunkAccess chunk = getChunkAccess(i, j, chunkStatus, bl);
		if (chunk instanceof LevelChunk) {
			chunk = new ImposterProtoChunk((LevelChunk) chunk);
		}
		return chunk;
	}

	private static ChunkStatus debugTriggeredForStatus = null;

	private ChunkAccess getChunkAccess(int i, int j, ChunkStatus chunkStatus, boolean bl) {
		ChunkAccess chunk = superHasChunk(i, j) ? superGetChunk(i, j, ChunkStatus.EMPTY) : null;
		if (chunk != null && chunk.getStatus().isOrAfter(chunkStatus)) {
			return chunk;
		}
		if (!bl)
			return null;
		if (chunk == null) {
			chunk = chunkMap.get(ChunkPos.asLong(i, j));
			if (chunk == null) {
				chunk = generator.generate(i, j);
				if (chunk == null)
					throw new NullPointerException("The provided generator should not return null!");
				chunkMap.put(ChunkPos.asLong(i, j), chunk);
			}
		}
		if (chunkStatus != ChunkStatus.EMPTY && chunkStatus != debugTriggeredForStatus) {
			ClientApi.LOGGER.info("WorldGen requiring " + chunkStatus
					+ " outside expected range detected. Force passing EMPTY chunk and seeing if it works.");
			debugTriggeredForStatus = chunkStatus;
		}
		return chunk;
	}

	// Override force use of my own light engine
	@Override
	public LevelLightEngine getLightEngine() {
		return light;
	}

	// Override force use of my own light engine
	@Override
	public int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
		if (lightMode != LightGenerationMode.FAST) {
			return light.getLayerListener(lightLayer).getLightValue(blockPos);
		}
		if (lightLayer == LightLayer.BLOCK)
			return 0;
		BlockPos p = super.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos);
		return (p.getY() <= blockPos.getY()) ? getMaxLightLevel() : 0;
	}

	// Override force use of my own light engine
	@Override
	public int getRawBrightness(BlockPos blockPos, int i) {
		if (lightMode != LightGenerationMode.FAST) {
			return light.getRawBrightness(blockPos, i);
		}
		BlockPos p = super.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos);
		return (p.getY() <= blockPos.getY()) ? getMaxLightLevel() : 0;
	}

	// Override force use of my own light engine
	@Override
	public boolean canSeeSky(BlockPos blockPos) {
		return (getBrightness(LightLayer.SKY, blockPos) >= getMaxLightLevel());
	}

}