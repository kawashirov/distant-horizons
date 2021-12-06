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

package com.seibel.lod.core.enums.config;

/**
 * Lowest <br>
 * Low <br>
 * Medium <br>
 * High <br>
 * <br>
 * this indicates the base of the quadratic function we use for the quality drop off
 * 
 * @author Leonardo Amato
 * @version 9-29-2021
 */
public enum HorizontalQuality
{
	/** 1.0 AKA Linear */
	LOWEST(1.0f),
	
	/** exponent 1.5 */
	LOW(1.5f),
	
	/** exponent 2.0 */
	MEDIUM(2.0f),
	
	/** exponent 2.2 */
	HIGH(2.2f);
	
	public final double quadraticBase;
	
	HorizontalQuality(double distanceUnit)
	{
		this.quadraticBase = distanceUnit;
	}
}