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

package com.seibel.lod.builders.lodBuilding;

import com.seibel.lod.enums.DistanceGenerationMode;

/**
 * This is used to easily configure how LodChunks are generated.
 * Generally this will only be used if we want to generate a
 * LodChunk using an incomplete Chunk, otherwise the defaults
 * work best for a fully generated chunk (IE has correct surface blocks).
 * @author James Seibel
 * @version 8-14-2021
 */
public class LodBuilderConfig
{
	/** default: false */
	public boolean useHeightmap;
	/** default: false */
	public boolean useBiomeColors;
	/** default: true */
	public boolean useSolidBlocksInColorGen;
	/** default: server */
	public DistanceGenerationMode distanceGenerationMode;
	
	/**
	 * default settings for a normal chunk <br>
	 * useHeightmap = false <br>
	 * useBiomeColors = false <br>
	 * useSolidBlocksInColorGen = true <br>
	 * generationMode = Server <br>
	 */
	public LodBuilderConfig()
	{
		useHeightmap = false;
		useBiomeColors = false;
		useSolidBlocksInColorGen = true;
		distanceGenerationMode = DistanceGenerationMode.SERVER;
	}
	
	/**
	 * @param newUseHeightmap default = false
	 * @param newUseBiomeColors default = false
	 * @param newUseSolidBlocksInBiomeColor default = true
	 * @param newDistanceGenerationMode default = Server
	 */
	public LodBuilderConfig(boolean newUseHeightmap, boolean newUseBiomeColors,
			boolean newUseSolidBlocksInBiomeColor, DistanceGenerationMode newDistanceGenerationMode)
	{
		useHeightmap = newUseHeightmap;
		useBiomeColors = newUseBiomeColors;
		useSolidBlocksInColorGen = newUseSolidBlocksInBiomeColor;
		distanceGenerationMode = newDistanceGenerationMode;
	}
	
	/**
	 * @param newUseHeightmap default = false
	 * @param newUseBiomeColors default = false
	 * @param newUseSolidBlocksInBiomeColor default = true
	 */
	public LodBuilderConfig(boolean newUseHeightmap, boolean newUseBiomeColors, boolean newUseSolidBlocksInBiomeColor)
	{
		this();
		useHeightmap = newUseHeightmap;
		useBiomeColors = newUseBiomeColors;
		useSolidBlocksInColorGen = newUseSolidBlocksInBiomeColor;
		distanceGenerationMode = newUseHeightmap ? DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT : DistanceGenerationMode.BIOME_ONLY;
	}
	
	/**
	 * @param newDistanceGenerationMode default = Server
	 */
	public LodBuilderConfig(DistanceGenerationMode newDistanceGenerationMode)
	{
		this();
		distanceGenerationMode = newDistanceGenerationMode;
	}
}