/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.lod.common.forge;

import com.seibel.lod.common.wrappers.minecraft.MinecraftClientWrapper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
#if POST_MC_1_19_1
import net.minecraft.util.RandomSource;
#endif
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Random;

/**
 * used for calling methods that forge modified
 * (forge modifies vanilla methods for some reason)
 * @author Ran
 */
public interface LodForgeMethodCaller {
    #if PRE_MC_1_19_1
    List<BakedQuad> getQuads(MinecraftClientWrapper mc, Block block, BlockState blockState, Direction direction, Random random); // FIXME: For 1.19
    #else
    List<BakedQuad> getQuads(MinecraftClientWrapper mc, Block block, BlockState blockState, Direction direction, RandomSource random); // FIXME: For 1.19
    #endif

    int colorResolverGetColor(ColorResolver resolver, Biome biome, double x, double z);
}
