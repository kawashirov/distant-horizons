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

package com.seibel.lod.common.wrappers.world;

import java.awt.Color;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MaterialColor;

//This class wraps the minecraft BlockPos.Mutable (and BlockPos) class
public class BiomeWrapper implements IBiomeWrapper
{

    public static final ConcurrentMap<Biome, BiomeWrapper> biomeWrapperMap = new ConcurrentHashMap<>();
    private Biome biome;

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

    @Override
    public String getName() {
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
