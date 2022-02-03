
package com.seibel.lod.common.wrappers.worldGeneration;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment.PerfCalculator;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.WorldGenStructFeatManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.structure.StructureCheck;

public final class ThreadedParameters
{
	private static final ThreadLocal<ThreadedParameters> localParam = new ThreadLocal<ThreadedParameters>();
	final ServerLevel level;
	public WorldGenStructFeatManager structFeat;
	public final StructureCheck structCheck;
	boolean isValid = true;
	public final PerfCalculator perf = new PerfCalculator();
	
	public static ThreadedParameters getOrMake(GlobalParameters param)
	{
		ThreadedParameters tParam = localParam.get();
		if (tParam != null && tParam.isValid && tParam.level == param.level)
			return tParam;
		tParam = new ThreadedParameters(param);
		localParam.set(tParam);
		return tParam;
	}
	
	public void markAsInvalid()
	{
		isValid = false;
	}
	
	private ThreadedParameters(GlobalParameters param)
	{
		level = param.level;
		structCheck = new StructureCheck(param.chunkScanner, param.registry, param.structures,
				param.level.dimension(), param.generator, level, param.generator.getBiomeSource(), param.worldSeed,
				param.fixerUpper);
		structFeat = new WorldGenStructFeatManager(level, param.worldGenSettings, null, structCheck);
	}
	
	public void makeStructFeat(WorldGenLevel genLevel, WorldGenSettings worldGenSettings)
	{
		structFeat = new WorldGenStructFeatManager(level, worldGenSettings, genLevel, structCheck);
	}
}