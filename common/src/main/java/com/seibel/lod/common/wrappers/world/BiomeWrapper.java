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

package com.seibel.lod.common.wrappers.world;

import java.awt.Color;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.biome.EndBiomes;
import net.minecraft.data.worldgen.biome.NetherBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MaterialColor;

//This class wraps the minecraft BlockPos.Mutable (and BlockPos) class
public class BiomeWrapper implements IBiomeWrapper
{

    public static final ConcurrentMap<Biome, BiomeWrapper> biomeWrapperMap = new ConcurrentHashMap<>();
    private final Biome biome;

    public BiomeWrapper(Biome biome)
    {
        this.biome = biome;
    }

    static public IBiomeWrapper getBiomeWrapper(Biome biome)
    {
        //first we check if the biome has already been wrapped
        if(biomeWrapperMap.containsKey(biome) && biomeWrapperMap.get(biome) != null)
            return biomeWrapperMap.get(biome);


        //if it hasn't been created yet, we create it and save it in the map
        BiomeWrapper biomeWrapper = new BiomeWrapper(biome);
        biomeWrapperMap.put(biome, biomeWrapper);

        //we return the newly created wrapper
        return biomeWrapper;
    }

    /** Returns a color int for the given biome. */
    #if PRE_MC_1_19
    @Override
    public int getColorForBiome(int x, int z)
    {
        int colorInt;
        switch (biome.biomeCategory)
        {

            case NETHER:
                colorInt = Blocks.NETHERRACK.defaultBlockState().getMaterial().getColor().col;
                break;

            case THEEND:
                colorInt = Blocks.END_STONE.defaultBlockState().getMaterial().getColor().col;
                break;

            case BEACH:
            case DESERT:
                colorInt = Blocks.SAND.defaultBlockState().getMaterial().getColor().col;
                break;

            case EXTREME_HILLS:
                colorInt = Blocks.STONE.defaultMaterialColor().col;
                break;

            case MUSHROOM:
                colorInt = MaterialColor.COLOR_LIGHT_GRAY.col;
                break;

            case ICY:
                colorInt = Blocks.SNOW.defaultMaterialColor().col;
                break;

            case MESA:
                colorInt = Blocks.RED_SAND.defaultMaterialColor().col;
                break;

            case OCEAN:
            case RIVER:
                colorInt = biome.getWaterColor();
                break;

            case NONE:
            case FOREST:
            case TAIGA:
            case JUNGLE:
            case PLAINS:
            case SAVANNA:
            case SWAMP:
            default:
                colorInt = biome.getGrassColor(x,z);
                //FIXME: Repair what James did - LeeTom
//                Color tmp = LodUtil.intToColor(biome.getGrassColor(x, z));
//                tmp = tmp.darker();
//                colorInt = LodUtil.colorToInt(tmp);
                break;

        }

        return colorInt;
    }
    #else


    private static int _colorEnd(Biome b) {
        return Blocks.END_STONE.defaultMaterialColor().col;
    }
    private static int _colorNether(Biome b) {
        return Blocks.NETHERRACK.defaultMaterialColor().col;
    }
    private static int _colorSand(Biome b) {
        return Blocks.SAND.defaultMaterialColor().col;
    }
    private static int _colorStone(Biome b) {
        return Blocks.STONE.defaultMaterialColor().col;
    }
    private static int _colorGravel(Biome b) {
        return Blocks.GRAVEL.defaultMaterialColor().col;
    }
    private static int _colorDripStone(Biome b) {
        return Blocks.DRIPSTONE_BLOCK.defaultMaterialColor().col;
    }
    private static int _colorMoss(Biome b) {
        return Blocks.MOSS_BLOCK.defaultMaterialColor().col;
    }
    private static int _colorSculk(Biome b) {
        return Blocks.SCULK.defaultMaterialColor().col;
    }
    private static int _colorMushoom(Biome b) {
        return Blocks.MYCELIUM.defaultMaterialColor().col;
    }
    private static int _colorBamboo(Biome b) {
        return Blocks.BAMBOO.defaultMaterialColor().col;
    }
    private static int _colorSnow(Biome b) {
        return Blocks.SNOW.defaultMaterialColor().col;
    }
    private static int _colorIce(Biome b) {
        return Blocks.ICE.defaultMaterialColor().col;
    }
    private static int _colorRedSand(Biome b) {
        return Blocks.RED_SAND.defaultMaterialColor().col;
    }
    private static int _colorSoulSand(Biome b) {
        return Blocks.SOUL_SAND.defaultMaterialColor().col;
    }
    private static int _colorBasalt(Biome b) {
        return Blocks.BASALT.defaultMaterialColor().col;
    }
    private static int _colorWater(Biome b) {
        return b.getWaterColor();
    }
    private static int _colorFoliage(Biome b) {
        return b.getFoliageColor();
    }

    private static Biome _get(ResourceKey<Biome> r) {
        return BuiltinRegistries.BIOME.getOrThrow(r);
    }

    //FIXME: THIS IS HELL!
    private static final ImmutableBiMap<Biome, Function<Biome, Integer>> BIOME_COLOR_MAP =
            ImmutableBiMap.<Biome, Function<Biome, Integer>>builder()
                    .put(_get(Biomes.SNOWY_PLAINS), BiomeWrapper::_colorSnow)
                    .put(_get(Biomes.ICE_SPIKES), BiomeWrapper::_colorIce)
                    .put(_get(Biomes.DESERT), BiomeWrapper::_colorSand)
                    .put(_get(Biomes.SWAMP), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.MANGROVE_SWAMP), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.FOREST), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.FLOWER_FOREST), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.BIRCH_FOREST), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.DARK_FOREST), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.OLD_GROWTH_BIRCH_FOREST), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.OLD_GROWTH_PINE_TAIGA), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.OLD_GROWTH_SPRUCE_TAIGA), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.TAIGA), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.SNOWY_TAIGA), BiomeWrapper::_colorSnow)
                    .put(_get(Biomes.WINDSWEPT_GRAVELLY_HILLS), BiomeWrapper::_colorGravel)
                    .put(_get(Biomes.WINDSWEPT_FOREST), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.JUNGLE), BiomeWrapper::_colorFoliage)
                    .put(_get(Biomes.BAMBOO_JUNGLE), BiomeWrapper::_colorBamboo)
                    .put(_get(Biomes.BADLANDS), BiomeWrapper::_colorRedSand)
                    .put(_get(Biomes.ERODED_BADLANDS), BiomeWrapper::_colorRedSand)
                    .put(_get(Biomes.WOODED_BADLANDS), BiomeWrapper::_colorStone)
                    .put(_get(Biomes.GROVE), BiomeWrapper::_colorSnow)
                    .put(_get(Biomes.SNOWY_SLOPES), BiomeWrapper::_colorSnow)
                    .put(_get(Biomes.FROZEN_PEAKS), BiomeWrapper::_colorIce)
                    .put(_get(Biomes.JAGGED_PEAKS), BiomeWrapper::_colorSnow)
                    .put(_get(Biomes.STONY_PEAKS), BiomeWrapper::_colorStone)
                    .put(_get(Biomes.RIVER), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.FROZEN_RIVER), BiomeWrapper::_colorIce)
                    .put(_get(Biomes.BEACH), BiomeWrapper::_colorSand)
                    .put(_get(Biomes.SNOWY_BEACH), BiomeWrapper::_colorSnow)
                    .put(_get(Biomes.STONY_SHORE), BiomeWrapper::_colorStone)
                    .put(_get(Biomes.WARM_OCEAN), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.LUKEWARM_OCEAN), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.DEEP_LUKEWARM_OCEAN), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.OCEAN), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.DEEP_OCEAN), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.COLD_OCEAN), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.DEEP_COLD_OCEAN), BiomeWrapper::_colorWater)
                    .put(_get(Biomes.FROZEN_OCEAN), BiomeWrapper::_colorIce)
                    .put(_get(Biomes.DEEP_FROZEN_OCEAN), BiomeWrapper::_colorIce)
                    .put(_get(Biomes.MUSHROOM_FIELDS), BiomeWrapper::_colorMushoom)
                    .put(_get(Biomes.DRIPSTONE_CAVES), BiomeWrapper::_colorDripStone)
                    .put(_get(Biomes.LUSH_CAVES), BiomeWrapper::_colorMoss)
                    .put(_get(Biomes.DEEP_DARK), BiomeWrapper::_colorSculk)
                    .put(_get(Biomes.NETHER_WASTES), BiomeWrapper::_colorNether)
                    .put(_get(Biomes.WARPED_FOREST), BiomeWrapper::_colorNether)
                    .put(_get(Biomes.CRIMSON_FOREST), BiomeWrapper::_colorNether)
                    .put(_get(Biomes.SOUL_SAND_VALLEY), BiomeWrapper::_colorSoulSand)
                    .put(_get(Biomes.BASALT_DELTAS), BiomeWrapper::_colorBasalt)
                    .put(_get(Biomes.THE_END), BiomeWrapper::_colorEnd)
                    .put(_get(Biomes.END_HIGHLANDS), BiomeWrapper::_colorEnd)
                    .put(_get(Biomes.END_MIDLANDS), BiomeWrapper::_colorEnd)
                    .put(_get(Biomes.SMALL_END_ISLANDS), BiomeWrapper::_colorEnd)
                    .put(_get(Biomes.END_BARRENS), BiomeWrapper::_colorEnd)
                    .build();

    @Override
    public int getColorForBiome(int x, int z)
    {
        int colorInt;
        Function<Biome, Integer> colorFunction = BIOME_COLOR_MAP.get(biome);
        if (colorFunction != null)
        {
            colorInt = colorFunction.apply(biome);
        }
        else
        {
            colorInt = biome.getGrassColor(x, z);
        }
        return colorInt;
    }
    #endif
    @Override public String getName()
    {
        return biome.toString();
    }
    
    @Override
    public int getGrassTint(int x, int z)
    {
        return biome.getGrassColor(x, z);
    }

    @Override
    public int getFolliageTint()
    {
        return biome.getFoliageColor();
    }

    @Override
    public int getWaterTint()
    {
        return biome.getWaterColor();
    }


    @Override public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof BiomeWrapper))
            return false;
        BiomeWrapper that = (BiomeWrapper) obj;
        return Objects.equals(biome, that.biome);
    }

    @Override public int hashCode()
    {
        return Objects.hash(biome);
    }

}
