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

package com.seibel.lod.enums;

/**
 * Minecraft, Lod_Builder, None
 * 
 * @author James Seibel
 * @version 10-1-2021
 */
public enum GlProxyContext
{
	/** Minecraft's render thread */
	MINECRAFT,
	
	/** The context we send buffers to the GPU on */
	LOD_BUILDER,
	
	/** used to un-bind threads */
	NONE,
}