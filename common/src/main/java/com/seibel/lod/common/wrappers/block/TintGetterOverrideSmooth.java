package com.seibel.lod.common.wrappers.block;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TintGetterOverrideSmooth implements BlockAndTintGetter {
    LevelReader parent;
    private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches;
    public int smoothingRange;

    public TintGetterOverrideSmooth(LevelReader parent, int smoothingRange) {
        this.parent = parent;
        this.smoothingRange = smoothingRange;
        this.tintCaches = Util.make(new Object2ObjectArrayMap(3), object2ObjectArrayMap -> {
            object2ObjectArrayMap.put(BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache((pos) -> calculateBlockTint(pos, BiomeColors.GRASS_COLOR_RESOLVER)));
            object2ObjectArrayMap.put(BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache((pos) -> calculateBlockTint(pos, BiomeColors.FOLIAGE_COLOR_RESOLVER)));
            object2ObjectArrayMap.put(BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache((pos) -> calculateBlockTint(pos, BiomeColors.WATER_COLOR_RESOLVER)));
        });
    }

    private Biome _getBiome(BlockPos pos) {
		#if MC_VERSION_1_18_2
        return parent.getBiome(pos).value();
		#elif MC_VERSION_1_18_1
		return parent.getBiome(pos);
		#endif
    }

    public int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver)
    {
        int i = smoothingRange;
        if (i == 0)
            return colorResolver.getColor(_getBiome(blockPos), blockPos.getX(), blockPos.getZ());
        int j = (i * 2 + 1) * (i * 2 + 1);
        int k = 0;
        int l = 0;
        int m = 0;
        Cursor3D cursor3D = new Cursor3D(blockPos.getX() - i, blockPos.getY(), blockPos.getZ() - i, blockPos.getX() + i, blockPos.getY(), blockPos.getZ() + i);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        while (cursor3D.advance())
        {
            mutableBlockPos.set(cursor3D.nextX(), cursor3D.nextY(), cursor3D.nextZ());
            int n = colorResolver.getColor(_getBiome(mutableBlockPos), mutableBlockPos.getX(), mutableBlockPos.getZ());
            k += (n & 0xFF0000) >> 16;
            l += (n & 0xFF00) >> 8;
            m += n & 0xFF;
        }
        return (k / j & 0xFF) << 16 | (l / j & 0xFF) << 8 | m / j & 0xFF;
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        BlockTintCache blockTintCache = this.tintCaches.get(colorResolver);
        return blockTintCache.getColor(blockPos);
    }

    @Override
    public float getShade(Direction direction, boolean bl) {
        return parent.getShade(direction, bl);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return parent.getLightEngine();
    }

    @Override
    public int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
        return parent.getBrightness(lightLayer, blockPos);
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int i) {
        return parent.getRawBrightness(blockPos, i);
    }

    @Override
    public boolean canSeeSky(BlockPos blockPos) {
        return parent.canSeeSky(blockPos);
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos blockPos) {
        return parent.getBlockEntity(blockPos);
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos blockPos, BlockEntityType<T> blockEntityType) {
        return parent.getBlockEntity(blockPos, blockEntityType);
    }

    @Override
    public BlockState getBlockState(BlockPos blockPos) {
        return parent.getBlockState(blockPos);
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        return parent.getFluidState(blockPos);
    }

    @Override
    public int getLightEmission(BlockPos blockPos) {
        return parent.getLightEmission(blockPos);
    }

    @Override
    public int getMaxLightLevel() {
        return parent.getMaxLightLevel();
    }

    @Override
    public Stream<BlockState> getBlockStates(AABB aABB) {
        return parent.getBlockStates(aABB);
    }

    @Override
    public BlockHitResult isBlockInLine(ClipBlockStateContext clipBlockStateContext) {
        return parent.isBlockInLine(clipBlockStateContext);
    }

    @Override
    public BlockHitResult clip(ClipContext clipContext) {
        return parent.clip(clipContext);
    }

    @Override
    @Nullable
    public BlockHitResult clipWithInteractionOverride(Vec3 vec3, Vec3 vec32, BlockPos blockPos, VoxelShape voxelShape, BlockState blockState) {
        return parent.clipWithInteractionOverride(vec3, vec32, blockPos, voxelShape, blockState);
    }

    @Override
    public double getBlockFloorHeight(VoxelShape voxelShape, Supplier<VoxelShape> supplier) {
        return parent.getBlockFloorHeight(voxelShape, supplier);
    }

    @Override
    public double getBlockFloorHeight(BlockPos blockPos) {
        return parent.getBlockFloorHeight(blockPos);
    }

    @Override
    public int getHeight() {
        return parent.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return parent.getMinBuildHeight();
    }

    @Override
    public int getMaxBuildHeight() {
        return parent.getMaxBuildHeight();
    }

    @Override
    public int getSectionsCount() {
        return parent.getSectionsCount();
    }

    @Override
    public int getMinSection() {
        return parent.getMinSection();
    }

    @Override
    public int getMaxSection() {
        return parent.getMaxSection();
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos blockPos) {
        return parent.isOutsideBuildHeight(blockPos);
    }

    @Override
    public boolean isOutsideBuildHeight(int i) {
        return parent.isOutsideBuildHeight(i);
    }

    @Override
    public int getSectionIndex(int i) {
        return parent.getSectionIndex(i);
    }

    @Override
    public int getSectionIndexFromSectionY(int i) {
        return parent.getSectionIndexFromSectionY(i);
    }

    @Override
    public int getSectionYFromSectionIndex(int i) {
        return parent.getSectionYFromSectionIndex(i);
    }
}