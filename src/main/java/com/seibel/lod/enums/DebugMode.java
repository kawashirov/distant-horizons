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
 * off, detail, detail wireframe
 * 
 * @author James Seibel
 * @version 8-28-2021
 */
public enum DebugMode
{
	/** LODs are rendered normally */
	OFF,
	
	/** LOD colors are based on their detail */
	SHOW_DETAIL,
	
	/** LOD colors are based on their detail, and draws in wireframe. */
	SHOW_DETAIL_WIREFRAME;
	
	/** used when cycling through the different modes */
	private DebugMode next;
	static 
	{
		OFF.next = SHOW_DETAIL;
		SHOW_DETAIL.next = SHOW_DETAIL_WIREFRAME;
		SHOW_DETAIL_WIREFRAME.next = OFF;
	}
	
	/** returns the next debug mode */
	public DebugMode getNext()
	{
		return this.next;
	}
}
