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
package com.seibel.lod.enums;

/**
 * Low <br>
 * Medium <br>
 * High <br>
 * <br>
 * this indicate the base of the quadratic function we use for the quality drop off
 * 
 * @author Leonardo Amato
 * @version 9-29-2021
 */
public enum HorizontalQuality
{
	/** Lods are 2D with heightMap */
	LINEAR(1.0f),

	/** Lods are 2D with heightMap */
	LOW(1.5f),

	/** Lods expand in three dimension */
	MEDIUM(2f),

	/** Lods expand in three dimension */
	HIGH(Math.E);

	public double quadraticBase;

	HorizontalQuality(double distanceUnit)
	{
		this.quadraticBase = distanceUnit;
	}
}