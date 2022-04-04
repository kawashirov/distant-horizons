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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TintGetterOverrideFast implements BlockAndTintGetter {
    LevelReader parent;

    public TintGetterOverrideFast(LevelReader parent) {
        this.parent = parent;
    }

    private Biome _getBiome(BlockPos pos) {
		#if MC_VERSION_1_18_2
        return parent.getBiome(pos).value();
		#else
		return parent.getBiome(pos);
		#endif
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        Biome b = _getBiome(blockPos);
        return colorResolver.getColor(b, blockPos.getX(), blockPos.getZ());
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
