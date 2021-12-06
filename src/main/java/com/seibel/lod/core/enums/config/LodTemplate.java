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

import com.seibel.lod.core.builders.bufferBuilding.lodTemplates.AbstractLodTemplate;
import com.seibel.lod.core.builders.bufferBuilding.lodTemplates.CubicLodTemplate;
import com.seibel.lod.core.builders.bufferBuilding.lodTemplates.DynamicLodTemplate;
import com.seibel.lod.core.builders.bufferBuilding.lodTemplates.TriangularLodTemplate;

/**
 * Cubic, Triangular, Dynamic
 * 
 * @author James Seibel
 * @version 10-10-2021
 */
public enum LodTemplate
{
	/**
	 * LODs are rendered as
	 * rectangular prisms.
	 */
	CUBIC(new CubicLodTemplate()),
	
	/**
	 * LODs smoothly transition between
	 * each other.
	 */
	TRIANGULAR(new TriangularLodTemplate()),
	
	/**
	 * LODs smoothly transition between
	 * each other, unless a neighboring LOD
	 * is at a significantly different height.
	 */
	DYNAMIC(new DynamicLodTemplate());
	
	
	public final AbstractLodTemplate template;
	
	LodTemplate(AbstractLodTemplate newTemplate)
	{
		template = newTemplate;
	}
	
}
