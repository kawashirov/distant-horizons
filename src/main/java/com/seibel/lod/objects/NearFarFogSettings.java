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

package com.seibel.lod.objects;

import com.seibel.lod.enums.FogDistance;
import com.seibel.lod.enums.FogQuality;

/**
 * This object is just a replacement for an array
 * to make things easier to understand in the LodRenderer.
 *
 * @author James Seibel
 * @version 7-03-2021
 */
public class NearFarFogSettings
{
	public final NearOrFarSetting near = new NearOrFarSetting(FogDistance.NEAR);
	public final NearOrFarSetting far = new NearOrFarSetting(FogDistance.FAR);
	
	/** If true that means Minecraft is
	 * rendering fog alongside us */
	public boolean vanillaIsRenderingFog = true;
	
	public NearFarFogSettings()
	{
		
	}
	
	
	
	/**
	 * This holds all relevant data to rendering fog at either
	 * near or far distances.
	 */
	public static class NearOrFarSetting
	{
		public FogQuality quality = FogQuality.FANCY;
		public FogDistance distance;
		
		public NearOrFarSetting(FogDistance newFogDistance)
		{
			distance = newFogDistance;
		}
	}
}
