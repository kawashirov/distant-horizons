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
 * Near_First <br>
 * Far_First <br>
 * <br>
 * Determines how fast the buffers need to be regenerated
 * @author Leonardo Amato
 * @version 9-25-2021
 */
public enum BufferRebuildTimes
{
	FREQUENT(1000, 500, 2500),
	
	NORMAL(2000, 1000, 5000),
	
	RARE(5000, 2000, 10000);
	
	public final int playerMoveTimeout;
	public final int renderedChunkTimeout;
	public final int chunkChangeTimeout;
	
	BufferRebuildTimes(int playerMoveTimeout, int renderedChunkTimeout, int chunkChangeTimeout)
	{
		this.playerMoveTimeout = playerMoveTimeout;
		this.renderedChunkTimeout = renderedChunkTimeout;
		this.chunkChangeTimeout = chunkChangeTimeout;
	}
}
