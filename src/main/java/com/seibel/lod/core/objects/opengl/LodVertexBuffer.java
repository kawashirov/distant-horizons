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

import org.lwjgl.opengl.GL15;

import com.seibel.lod.core.enums.rendering.GLProxyContext;
import com.seibel.lod.core.render.GLProxy;

/**
 * This is a container for a OpenGL
 * VBO (Vertex Buffer Object).
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public class LodVertexBuffer implements AutoCloseable
{
	public int id;
	public int vertexCount;
	
	public LodVertexBuffer()
	{
		if (GLProxy.getInstance().getGlContext() == GLProxyContext.NONE)
			throw new IllegalStateException("Thread [" +Thread.currentThread().getName() + "] tried to create a [" + LodVertexBuffer.class.getSimpleName() + "] outside a OpenGL contex.");
		
		this.id = GL15.glGenBuffers();
	}
	
	
	@Override
	public void close()
	{
		if (this.id >= 0)
		{
			GLProxy.getInstance().recordOpenGlCall(() -> GL15.glDeleteBuffers(this.id));
			this.id = -1;
		}
	}
}