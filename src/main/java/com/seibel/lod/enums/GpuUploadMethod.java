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
 * Buffer_Storage, Sub_Data, Buffer_Mapping
 * 
 * @author James Seibel
 * @version 10-23-2021
 */
public enum GpuUploadMethod
{
	/** Default if OpenGL 4.5 is supported. Fast rendering, no stuttering. */
	BUFFER_STORAGE,
	
	/** Default if OpenGL 4.5 is NOT supported. Fast rendering but may stutter when uploading. */
	SUB_DATA,
	
	/** May end up storing buffers in System memory. Slower rendering but won't stutter when uploading. */
	BUFFER_MAPPING,
}