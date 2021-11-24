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

package com.seibel.lod.core.api;

import com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.objects.lod.LodWorld;

/**
 * This stores objects and variables that
 * are shared between the different Core api classes.
 * 
 * @author James Seibel
 * @version 11-12-2021
 */
public class ApiShared
{
	public ApiShared INSTANCE = new ApiShared();
	
	public static final LodBufferBuilderFactory lodBufferBuilderFactory = new LodBufferBuilderFactory();
	public static final LodWorld lodWorld = new LodWorld();
	public static final LodBuilder lodBuilder = new LodBuilder();
	
	/** Used to determine if the LODs should be regenerated */
	public static int previousChunkRenderDistance = 0;
	/** Used to determine if the LODs should be regenerated */
	public static int previousLodRenderDistance = 0;
	
	
	
	private ApiShared()
	{
		
	}
	
}
