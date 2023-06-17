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

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.google.common.collect.ImmutableBiMap;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
#if POST_MC_1_19
import net.minecraft.data.worldgen.biome.EndBiomes;
import net.minecraft.data.worldgen.biome.NetherBiomes;
#endif
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;


/** This class wraps the minecraft BlockPos.Mutable (and BlockPos) class */
public class BiomeWrapper implements IBiomeWrapper
{
    #if PRE_MC_1_18_2
    public static final ConcurrentMap<Biome, BiomeWrapper> biomeWrapperMap = new ConcurrentHashMap<>();
    public final Biome biome;
    #else
    public static final ConcurrentMap<Holder<Biome>, BiomeWrapper> biomeWrapperMap = new ConcurrentHashMap<>();
    public final Holder<Biome> biome;
    #endif

    static public IBiomeWrapper getBiomeWrapper(#if PRE_MC_1_18_2 Biome #else Holder<Biome> #endif biome)
    {
        return biomeWrapperMap.computeIfAbsent(biome, BiomeWrapper::new);
    }

    private BiomeWrapper(#if PRE_MC_1_18_2 Biome #else Holder<Biome> #endif biome)
    {
        this.biome = biome;
    }

    @Override
    public String getName()
    {
        #if PRE_MC_1_18_2
        return biome.toString();
        #else
        return biome.unwrapKey().orElse(Biomes.THE_VOID).registry().toString();
        #endif
    }

    @Override
    public String serialize() {
        //FIXME: Pass in a level obj
        String data = Biome.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, Minecraft.getInstance().level.registryAccess()),
                biome).get().orThrow().toString();
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiomeWrapper that = (BiomeWrapper) o;
        return Objects.equals(biome, that.biome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(biome);
    }
	
	public static IBiomeWrapper deserialize(String str) throws IOException
	{
		try
		{
         #if PRE_MC_1_18_2 Biome #else
			Holder<Biome> #endif
					biome = Biome.CODEC.decode(RegistryOps.create(JsonOps.INSTANCE, Minecraft.getInstance().level.registryAccess()),
					JsonParser.parseString(str)).get().orThrow().getFirst();
			return getBiomeWrapper(biome);
		}
		catch (Exception e)
		{
			throw new IOException("Failed to deserialize biome wrapper", e);
		}
	}
	
	
	@Override 
	public Object getWrappedMcObject() { return this.biome; }
	
}
