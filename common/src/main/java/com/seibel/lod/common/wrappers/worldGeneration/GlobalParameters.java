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
 
package com.seibel.lod.common.wrappers.worldGeneration;

import com.mojang.datafixers.DataFixer;
import com.seibel.lod.common.wrappers.world.ServerLevelWrapper;
import com.seibel.lod.core.level.IServerLevel;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
#if POST_MC_1_18_1
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
#endif
import net.minecraft.world.level.levelgen.WorldGenSettings;
#if PRE_MC_1_19
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
#else
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
#endif
import net.minecraft.world.level.storage.WorldData;

public final class GlobalParameters
{
	public final ChunkGenerator generator;
	#if PRE_MC_1_19
	public final StructureManager structures;
	#else
	public final StructureTemplateManager structures;
	public final RandomState randomState;
	#endif
	public final WorldGenSettings worldGenSettings;
	public final ThreadedLevelLightEngine lightEngine;
	public final IServerLevel lodLevel;
	public final ServerLevel level;
	public final Registry<Biome> biomes;
	public final RegistryAccess registry;
	public final long worldSeed;
	public final DataFixer fixerUpper;
	#if POST_MC_1_18_1
	public final BiomeManager biomeManager;
	public final ChunkScanAccess chunkScanner; // FIXME: Figure out if this is actually needed
	#endif
	
	public GlobalParameters(IServerLevel lodLevel)
	{
		this.lodLevel = lodLevel;

		level = ((ServerLevelWrapper)lodLevel.getServerLevelWrapper()).unwrapLevel();
		lightEngine = (ThreadedLevelLightEngine) level.getLightEngine();
		MinecraftServer server = level.getServer();
		WorldData worldData = server.getWorldData();
		worldGenSettings = worldData.worldGenSettings();
		registry = server.registryAccess();
		biomes = registry.registryOrThrow(Registry.BIOME_REGISTRY);
		worldSeed = worldGenSettings.seed();
		#if POST_MC_1_18_1
		biomeManager = new BiomeManager(level, BiomeManager.obfuscateSeed(worldSeed));
		chunkScanner = level.getChunkSource().chunkScanner();
		#endif
		structures = server.getStructureManager();
		generator = level.getChunkSource().getGenerator();
		fixerUpper = server.getFixerUpper();
		#if POST_MC_1_19
		randomState = level.getChunkSource().randomState();
		#endif
	}
}