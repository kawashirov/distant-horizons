/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.common.wrappers.block;

import com.seibel.lod.common.LodCommonMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
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

public class TintWithoutLevelOverrider implements BlockAndTintGetter {
    final BiomeWrapper biome;

    public TintWithoutLevelOverrider(BiomeWrapper biome) {
        this.biome = biome;
    }
    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        return colorResolver.getColor(_unwrap(biome.biome), blockPos.getX(), blockPos.getZ());
    }
    private Biome _unwrap(#if POST_MC_1_18_2 Holder<Biome> #else Biome #endif biome) {
		#if POST_MC_1_18_2
        return biome.value();
		#else
        return biome;
		#endif
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        throw new UnsupportedOperationException("ERROR: getShade() called on TintWithoutLevelOverrider. Object is for tinting only.");
    }
    @Override
    public LevelLightEngine getLightEngine() {
        throw new UnsupportedOperationException("ERROR: getLightEngine() called on TintWithoutLevelOverrider. Object is for tinting only.");
    }
    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        throw new UnsupportedOperationException("ERROR: getBlockEntity() called on TintWithoutLevelOverrider. Object is for tinting only.");
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        throw new UnsupportedOperationException("ERROR: getBlockState() called on TintWithoutLevelOverrider. Object is for tinting only.");
    }
    @Override
    public FluidState getFluidState(BlockPos pos) {
        throw new UnsupportedOperationException("ERROR: getFluidState() called on TintWithoutLevelOverrider. Object is for tinting only.");
    }
    @Override
    public int getHeight() {
        throw new UnsupportedOperationException("ERROR: getHeight() called on TintWithoutLevelOverrider. Object is for tinting only.");
    }
    @Override
    public int getMinBuildHeight() {
        throw new UnsupportedOperationException("ERROR: getMinBuildHeight() called on TintWithoutLevelOverrider. Object is for tinting only.");
    }
}
