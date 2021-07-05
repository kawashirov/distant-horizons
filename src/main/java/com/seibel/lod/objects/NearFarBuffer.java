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

import net.minecraft.client.renderer.BufferBuilder;

/**
 * This object is just a replacement for an array
 * to make things easier to understand in the LodRenderer
 * and BuildBufferThread.
 * 
 * @author James Seibel
 * @version 03-25-2021
 */
public class NearFarBuffer
{
	public BufferBuilder nearBuffer;
	
	public BufferBuilder farBuffer;
	
	/**
	 * @param newNearBuffer
	 * @param newFarBuffer
	 */
	public NearFarBuffer(BufferBuilder newNearBuffer, BufferBuilder newFarBuffer)
	{
		nearBuffer = newNearBuffer;
		farBuffer = newFarBuffer;
	}
}
