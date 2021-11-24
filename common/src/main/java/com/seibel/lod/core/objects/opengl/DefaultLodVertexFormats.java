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

package com.seibel.lod.core.objects.opengl;

import com.google.common.collect.ImmutableList;

/**
 * A (almost) exact copy of MC's
 * DefaultVertexFormats class.
 * 
 * @author James Seibel
 * @version 11-13-2021
 */
public class DefaultLodVertexFormats
{
	public static final LodVertexFormatElement ELEMENT_POSITION = new LodVertexFormatElement(0, LodVertexFormatElement.DataType.FLOAT, 3);
	public static final LodVertexFormatElement ELEMENT_COLOR = new LodVertexFormatElement(0, LodVertexFormatElement.DataType.UBYTE, 4);
	public static final LodVertexFormatElement ELEMENT_UV = new LodVertexFormatElement(0, LodVertexFormatElement.DataType.FLOAT, 2);
	public static final LodVertexFormatElement ELEMENT_LIGHT_MAP_UV = new LodVertexFormatElement(1, LodVertexFormatElement.DataType.SHORT, 2);
	public static final LodVertexFormatElement ELEMENT_NORMAL = new LodVertexFormatElement(0, LodVertexFormatElement.DataType.BYTE, 3);
	public static final LodVertexFormatElement ELEMENT_PADDING = new LodVertexFormatElement(0, LodVertexFormatElement.DataType.BYTE, 1);
	
	
	public static final LodVertexFormat POSITION = 						new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).build());
	public static final LodVertexFormat POSITION_COLOR = 				new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_COLOR).build());
	public static final LodVertexFormat POSITION_COLOR_LIGHTMAP = 		new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_COLOR).add(ELEMENT_LIGHT_MAP_UV).build());
	public static final LodVertexFormat POSITION_TEX = 					new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_UV).build());
	public static final LodVertexFormat POSITION_COLOR_TEX = 			new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_COLOR).add(ELEMENT_UV).build());
	public static final LodVertexFormat POSITION_COLOR_TEX_LIGHTMAP = 	new LodVertexFormat(ImmutableList.<LodVertexFormatElement>builder().add(ELEMENT_POSITION).add(ELEMENT_COLOR).add(ELEMENT_UV).add(ELEMENT_LIGHT_MAP_UV).build());
	
}
