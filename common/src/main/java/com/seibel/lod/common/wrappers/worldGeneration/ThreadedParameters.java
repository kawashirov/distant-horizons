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
 

package com.seibel.lod.common.wrappers.worldGeneration;

import com.seibel.lod.common.wrappers.worldGeneration.BatchGenerationEnvironment.PerfCalculator;
import com.seibel.lod.common.wrappers.worldGeneration.mimicObject.WorldGenStructFeatManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
#if POST_MC_1_18_1
import net.minecraft.world.level.levelgen.structure.StructureCheck;
#endif

public final class ThreadedParameters
{
	private static final ThreadLocal<ThreadedParameters> localParam = new ThreadLocal<ThreadedParameters>();
	final ServerLevel level;
	public WorldGenStructFeatManager structFeat = null;
	#if POST_MC_1_18_1
	public final StructureCheck structCheck;
	#endif
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
		#if PRE_MC_1_18_1
		structFeat = new WorldGenStructFeatManager(param.worldGenSettings, level);
		#elif PRE_MC_1_19
		structCheck = new StructureCheck(param.chunkScanner, param.registry, param.structures,
				param.level.dimension(), param.generator, level, param.generator.getBiomeSource(), param.worldSeed,
				param.fixerUpper);
		#else
		structCheck = new StructureCheck(param.chunkScanner, param.registry, param.structures,
				param.level.dimension(), param.generator, param.randomState, level, param.generator.getBiomeSource(), param.worldSeed,
				param.fixerUpper);
		#endif
	}
	
	public void makeStructFeat(WorldGenLevel genLevel, GlobalParameters param)
	{
		structFeat = new WorldGenStructFeatManager(param.worldGenSettings, genLevel #if POST_MC_1_18_1, structCheck #endif);
	}
}