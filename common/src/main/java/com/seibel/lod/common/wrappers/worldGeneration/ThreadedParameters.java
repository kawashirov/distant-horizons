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
import net.minecraft.world.level.levelgen.WorldGenSettings;
#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
import net.minecraft.world.level.levelgen.structure.StructureCheck;
#endif

public final class ThreadedParameters
{
	private static final ThreadLocal<ThreadedParameters> localParam = new ThreadLocal<ThreadedParameters>();
	final ServerLevel level;
	#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
	public WorldGenStructFeatManager structFeat = null;
	public final StructureCheck structCheck;
	#elif MC_VERSION_1_17_1 || MC_VERSION_1_16_5
	public WorldGenStructFeatManager structFeat;
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
		#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
		level = param.level;
		structCheck = new StructureCheck(param.chunkScanner, param.registry, param.structures,
				param.level.dimension(), param.generator, level, param.generator.getBiomeSource(), param.worldSeed,
				param.fixerUpper);
		#elif MC_VERSION_1_17_1 || MC_VERSION_1_16_5
		level = param.level;
		structFeat = new WorldGenStructFeatManager(param.worldGenSettings, level);
		#endif
	}
	
	public void makeStructFeat(WorldGenLevel genLevel, GlobalParameters param)
	{
		#if MC_VERSION_1_18_2 || MC_VERSION_1_18_1
		structFeat = new WorldGenStructFeatManager(param.worldGenSettings, genLevel, structCheck);
		#elif MC_VERSION_1_17_1
		structFeat = new WorldGenStructFeatManager(param.worldGenSettings, genLevel);
		#endif
	}
}