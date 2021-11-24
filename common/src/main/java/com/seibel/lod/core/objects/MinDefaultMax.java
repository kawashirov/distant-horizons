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

package com.seibel.lod.core.objects;

/**
 * Used when setting up configuration fields.
 * 
 * @author James Seibel
 * @version 11-14-2021
 * @param <T> The data type this object is storing
 */
public class MinDefaultMax<T>
{
	public T minValue;
	public T defaultValue;
	public T maxValue;
	
	public MinDefaultMax(T newMinValue, T newDefaultValue, T newMaxValue)
	{
		minValue = newMinValue;
		defaultValue = newDefaultValue;
		maxValue = newMaxValue;
	}
}
