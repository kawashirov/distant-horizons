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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
#if POST_MC_1_19
import net.minecraft.data.worldgen.biome.EndBiomes;
import net.minecraft.data.worldgen.biome.NetherBiomes;
#endif
import net.minecraft.core.RegistryAccess;
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
	public static ConcurrentMap<Holder<Biome>, BiomeWrapper> WRAPPER_BY_BIOME = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<Holder<Biome>, String> SERIAL_BY_BIOME = new ConcurrentHashMap<>();
	
    public final Holder<Biome> biome;
    #endif
	
	private static boolean registryOpsOutdated = false;
	private static RegistryOps<JsonElement> registryOps = null;
	private static RegistryOps<JsonElement> getRegistryOps() 
	{
		ClientLevel level = Minecraft.getInstance().level;
		if (registryOps != null && level == null)
		{
			// request a new registryOps the next time a world is loaded,
			// if the world is reset the old registryOps will throw exceptions
			registryOpsOutdated = true;
		}
		
		if (registryOps == null || (registryOpsOutdated && level != null))
		{
			registryOps = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
		}
		return registryOps;
	}
	
	
	
	//==============//
	// constructors //
	//==============//
	
    private BiomeWrapper(#if PRE_MC_1_18_2 Biome #else Holder<Biome> #endif biome)
    {
        this.biome = biome;
	
		SERIAL_BY_BIOME.put(this.biome, this.serialize());
    }
	
	static public IBiomeWrapper getBiomeWrapper(#if PRE_MC_1_18_2 Biome #else Holder<Biome> #endif biome)
	{
		return WRAPPER_BY_BIOME.computeIfAbsent(biome, BiomeWrapper::new);
	}
	
	
	
	//=========//
	// methods //
	//=========//
	
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
    public String serialize()
	{
		if (!SERIAL_BY_BIOME.containsKey(this.biome))
		{
			String newSerial = Biome.CODEC.encodeStart(getRegistryOps(), biome).get().orThrow().toString();
			SERIAL_BY_BIOME.put(this.biome, newSerial);
		}
		String serial = SERIAL_BY_BIOME.get(this.biome);
		return serial;
	}
	
	
    @Override 
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		else if (obj == null || getClass() != obj.getClass())
		{
			return false;
		}
		
		BiomeWrapper that = (BiomeWrapper) obj;
		return Objects.equals(biome, that.biome);
	}

    @Override
    public int hashCode() { return Objects.hash(biome); }
	
	public static IBiomeWrapper deserialize(String serial) throws IOException
	{
		try
		{
         #if PRE_MC_1_18_2 Biome #else
			Holder<Biome> #endif
					biome = Biome.CODEC.decode(getRegistryOps(), JsonParser.parseString(serial)).get().orThrow().getFirst();
			return getBiomeWrapper(biome);
		}
		catch (Exception e)
		{
			throw new IOException("Failed to deserialize biome wrapper", e);
		}
	}
	
	@Override 
	public Object getWrappedMcObject_UNSAFE() { return this.biome; }
	
}
