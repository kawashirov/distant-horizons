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

package com.seibel.distanthorizons.common.wrappers.block;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;

import net.minecraft.client.Minecraft;
#if POST_MC_1_17
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryOps;
#endif

#if POST_MC_1_19_2
import net.minecraft.data.worldgen.biome.EndBiomes;
import net.minecraft.data.worldgen.biome.NetherBiomes;
#endif


#if MC_1_16_5 || MC_1_17_1
import net.minecraft.core.Registry;
#elif MC_1_18_2 || MC_1_19_2
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.BuiltinRegistries;
#else
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
#endif

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
#if !PRE_MC_1_18_2
import net.minecraft.world.level.biome.Biomes;
#endif


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
	
	@Override
	public String serialize() // FIXME pass in level to prevent null pointers (or maybe just RegistryAccess?)
	{
		
		net.minecraft.core.RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
		
		ResourceLocation resourceLocation;
		#if MC_1_16_5 || MC_1_17_1
		resourceLocation = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).getKey(this.biome);
		#elif MC_1_18_2 || MC_1_19_2
		resourceLocation = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).getKey(this.biome.value());
		#else
		resourceLocation = registryAccess.registryOrThrow(Registries.BIOME).getKey(this.biome.value());
		#endif
		
		if (resourceLocation == null)
		{
			// shouldn't normally happen, but just in case
			return "";
		}
		else
		{
			String resourceLocationString = resourceLocation.getNamespace()+":"+resourceLocation.getPath();
			return resourceLocationString;	
		}
	}
	
	public static IBiomeWrapper deserialize(String resourceLocationString) throws IOException // FIXME pass in level to prevent null pointers (or maybe just RegistryAccess?)
	{
		if (resourceLocationString.trim().isEmpty() || resourceLocationString.equals(""))
		{
			// shouldn't normally happen, but just in case
			new ResourceLocation("minecraft", "the_void"); // just "void" in MC 1.12 through 1.9 (inclusive)
		}
		
		
		// parse the resource location
		int separatorIndex = resourceLocationString.indexOf(":");
		if (separatorIndex == -1)
		{
			throw new IOException("Unable to parse resource location string: ["+resourceLocationString+"].");
		}
		ResourceLocation resourceLocation = new ResourceLocation(resourceLocationString.substring(0, separatorIndex), resourceLocationString.substring(separatorIndex+1));
		
		
		try
		{
			net.minecraft.core.RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
			
			#if MC_1_16_5 || MC_1_17_1
			Biome biome = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).get(resourceLocation);
			#elif MC_1_18_2 || MC_1_19_2
			Biome unwrappedBiome = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY).get(resourceLocation);
			Holder<Biome> biome = new Holder.Direct<>(unwrappedBiome);
			#else
			Biome unwrappedBiome = registryAccess.registryOrThrow(Registries.BIOME).get(resourceLocation);
			Holder<Biome> biome = new Holder.Direct<>(unwrappedBiome);
			#endif
			
			return getBiomeWrapper(biome);
		}
		catch (Exception e)
		{
			throw new IOException("Failed to deserialize the string ["+resourceLocationString+"] into a BiomeWrapper: "+e.getMessage(), e);
		}
	}
	
	
	@Override 
	public Object getWrappedMcObject() { return this.biome; }
	
}
