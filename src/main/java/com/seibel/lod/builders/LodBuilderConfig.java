/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
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
package com.seibel.lod.builders;

/**
 * This is used to easily configure how LodChunks are generated.
 * Generally this will only be used if we want to generate a
 * LodChunk using a incomplete Chunk, otherwise the defaults
 * work best for a fully generated chunk (IE has correct surface blocks).
 * 
 * @author James Seibel
 * @version 6-27-2021
 */
public class LodBuilderConfig
{
	/** default false */
	public boolean useHeightmap;
	/** default false */
	public boolean useBiomeColors;
	/** default true */
	public boolean useSolidBlocksInColorGen;
	
	/** default settings for a normal chunk 
	 * useHeightmap = false
	 * useBiomeColors = false
	 * useSolidBlocksInColorGen = true
	 */
	public LodBuilderConfig()
	{
		useHeightmap = false;
		useBiomeColors = false;
		useSolidBlocksInColorGen = true;
	}
	
	/**
	 * @param newUseHeightmap default = false
	 * @param newUseBiomeColors default = false
	 * @param newUseSolidBlocksInBiomeColor default = true
	 */
	public LodBuilderConfig(boolean newUseHeightmap, boolean newUseBiomeColors, boolean newUseSolidBlocksInBiomeColor)
	{
		useHeightmap = newUseHeightmap;
		useBiomeColors = newUseBiomeColors;
		useSolidBlocksInColorGen = newUseSolidBlocksInBiomeColor;
	}
}