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

package com.seibel.lod.core.wrapperInterfaces.minecraft;

import java.util.HashSet;

import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;

/**
 * Contains everything related to
 * rendering in Minecraft.
 * 
 * @author James Seibel
 * @version 11-18-2021
 */
public interface IMinecraftRenderWrapper
{
	public Vec3f getLookAtVector();
	
	public AbstractBlockPosWrapper getCameraBlockPosition();
	
	public boolean playerHasBlindnessEffect();
	
	public Vec3d getCameraExactPosition();
	
	public Mat4f getDefaultProjectionMatrix(float partialTicks);
	
	public double getGamma();
	
	public double getFov(float partialTicks);
	
	/** Measured in chunks */
	public int getRenderDistance();
	
	public int getScreenWidth();
	public int getScreenHeight();
	
	/**
	 * This method returns the ChunkPos of all chunks that Minecraft
	 * is going to render this frame.
	 */
	public HashSet<AbstractChunkPosWrapper> getRenderedChunks();
}
