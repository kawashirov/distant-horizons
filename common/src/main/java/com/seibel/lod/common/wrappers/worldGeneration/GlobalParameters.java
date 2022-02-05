
package com.seibel.lod.common.wrappers.worldGeneration;

import com.mojang.datafixers.DataFixer;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.objects.lod.LodDimension;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.storage.WorldData;

public final class GlobalParameters
{
	public final ChunkGenerator generator;
	public final StructureManager structures;
	//public final BiomeManager biomeManager;
	public final WorldGenSettings worldGenSettings;
	public final ThreadedLevelLightEngine lightEngine;
	public final LodBuilder lodBuilder;
	public final LodDimension lodDim;
	public final Registry<Biome> biomes;
	public final RegistryAccess registry;
	public final long worldSeed;
	public final ServerLevel level; // TODO: Figure out a way to remove this. Maybe ClientLevel also works?
	public final DataFixer fixerUpper;
	
	public GlobalParameters(ServerLevel level, LodBuilder lodBuilder, LodDimension lodDim)
	{
		this.lodBuilder = lodBuilder;
		this.lodDim = lodDim;
		this.level = level;
		lightEngine = (ThreadedLevelLightEngine) level.getLightEngine();
		MinecraftServer server = level.getServer();
		WorldData worldData = server.getWorldData();
		worldGenSettings = worldData.worldGenSettings();
		registry = server.registryAccess();
		biomes = registry.registryOrThrow(Registry.BIOME_REGISTRY);
		worldSeed = worldGenSettings.seed();
		//biomeManager = new BiomeManager(level, BiomeManager.obfuscateSeed(worldSeed));
		structures = server.getStructureManager();
		generator = level.getChunkSource().getGenerator();
		fixerUpper = server.getFixerUpper();
	}
}