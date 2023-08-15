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

package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import com.mojang.datafixers.DataFixer;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.level.IDhServerLevel;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
#if POST_MC_1_18_2
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
#endif
import net.minecraft.world.level.levelgen.WorldGenSettings;
#if PRE_MC_1_19_2
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
#else
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.RandomState;
#if POST_MC_1_19_4
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.core.registries.Registries;
#endif
#endif
import net.minecraft.world.level.storage.WorldData;

public final class GlobalParameters
{
	public final ChunkGenerator generator;
	#if PRE_MC_1_19_2
	public final StructureManager structures;
	#else
	public final StructureTemplateManager structures;
	public final RandomState randomState;
	#endif
	#if PRE_MC_1_19_4
	public final WorldGenSettings worldGenSettings;
	#else
	public final WorldOptions worldOptions;
	#endif
	public final ThreadedLevelLightEngine lightEngine;
	public final IDhServerLevel lodLevel;
	public final ServerLevel level;
	public final Registry<Biome> biomes;
	public final RegistryAccess registry;
	public final long worldSeed;
	public final DataFixer fixerUpper;
	#if POST_MC_1_18_2
	public final BiomeManager biomeManager;
	public final ChunkScanAccess chunkScanner; // FIXME: Figure out if this is actually needed
	#endif
	
	public GlobalParameters(IDhServerLevel lodLevel)
	{
		this.lodLevel = lodLevel;
		
		level = ((ServerLevelWrapper) lodLevel.getServerLevelWrapper()).getWrappedMcObject();
		lightEngine = (ThreadedLevelLightEngine) level.getLightEngine();
		MinecraftServer server = level.getServer();
		WorldData worldData = server.getWorldData();
		registry = server.registryAccess();

		#if PRE_MC_1_19_4
		worldGenSettings = worldData.worldGenSettings();
		biomes = registry.registryOrThrow(Registry.BIOME_REGISTRY);
		worldSeed = worldGenSettings.seed();
		#else
		worldOptions = worldData.worldGenOptions();
		biomes = registry.registryOrThrow(Registries.BIOME);
		worldSeed = worldOptions.seed();
		#endif
		#if POST_MC_1_18_2
		biomeManager = new BiomeManager(level, BiomeManager.obfuscateSeed(worldSeed));
		chunkScanner = level.getChunkSource().chunkScanner();
		#endif
		structures = server.getStructureManager();
		generator = level.getChunkSource().getGenerator();
		fixerUpper = server.getFixerUpper();
		#if POST_MC_1_19_2
		randomState = level.getChunkSource().randomState();
		#endif
	}
	
}