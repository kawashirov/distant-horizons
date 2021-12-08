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

package com.seibel.lod.core.render.shader;

import java.awt.Color;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;


/**
 * This object holds the reference to a OpenGL shader program
 * and contains a few methods that can be used with OpenGL shader programs.
 * The reason for many of these simple wrapper methods is as reminders of what
 * can (and needs to be) done with a shader program.
 * 
 * @author James Seibel
 * @version 11-26-2021
 */
public class LodShaderProgram
{
	/** Stores the handle of the program. */
	public final int id;
	
	/** Creates a shader program. */
	public LodShaderProgram()
	{
		id = GL20.glCreateProgram();
	}
	
	
	
	/** Calls GL20.glUseProgram(this.id) */
	public void use()
	{
		GL20.glUseProgram(id);
	}
	
	/**
	 * Calls GL20.glAttachShader(this.id, shader.id)
	 *
	 * @param shader Shader to get attached
	 */
	public void attachShader(LodShader shader)
	{
		GL20.glAttachShader(this.id, shader.id);
	}
	

	/**
	 * Links the shader program to the current OpenGL context.
	 * @throws Exception Exception if the program failed to link
	 */
	public void link()
	{
		GL20.glLinkProgram(this.id);
	    checkLinkStatus();
	}
	
	/**
	 * Checks if the program was linked successfully.
	 * @throws Exception if the program failed to link
	 */
	public void checkLinkStatus()
	{
		int status = GL20.glGetProgrami(this.id, GL20.GL_LINK_STATUS);
		if (status != GL20.GL_TRUE)
			throw new RuntimeException("Shader Link Error. Details: "+GL20.glGetProgramInfoLog(this.id));
	}
	
	
	
	
	/**
	 * Gets the location of an attribute variable with specified name.
	 * Calls GL20.glGetAttribLocation(id, name)
	 *
	 * @param name Attribute name
	 *
	 * @return Location of the attribute
	 */
	public int getAttributeLocation(CharSequence name)
	{
		return GL20.glGetAttribLocation(id, name);
	}
	
	/**
	 * Calls GL20.glEnableVertexAttribArray(location)
	 * 
	 * @param location Location of the vertex attribute
	 */
	public void enableVertexAttribute(int location)
	{
		GL20.glEnableVertexAttribArray(location);
	}
	
	/**
	 * Calls GL20.glDisableVertexAttribArray(location)
	 * 
	 * @param location Location of the vertex attribute
	 */
	public void disableVertexAttribute(int location)
	{
		GL20.glDisableVertexAttribArray(location);
	}
	
	/**
	 * Sets the vertex attribute pointer.
	 * Calls GL20.glVertexAttribPointer(...)
	 *
	 * @param location Location of the vertex attribute
	 * @param size     Number of values per vertex
	 * @param stride   Offset between consecutive generic vertex attributes in
	 *                 bytes
	 * @param offset   Offset of the first component of the first generic vertex
	 *                 attribute in bytes
	 */
	public void pointVertexAttribute(int location, int size, int stride, int offset)
	{
		GL20.glVertexAttribPointer(location, size, GL20.GL_FLOAT, false, stride, offset);
	}
	
	/**
	 * Gets the location of a uniform variable with specified name.
	 * Calls GL20.glGetUniformLocation(id, name)
	 *
	 * @param name Uniform name
	 *
	 * @return -1 = error value, 0 = first value, 1 = second value, etc.
	 */
	public int getUniformLocation(CharSequence name)
	{
		return GL20.glGetUniformLocation(id, name);
	}
	
	
	
	public void setUniform(int location, boolean value)
	{
		GL20.glUniform1i(location, value ? 1 : 0);
	}
	
	public void setUniform(int location, int value)
	{
		GL20.glUniform1i(location, value);
	}
	
	public void setUniform(int location, float value)
	{
		GL20.glUniform1f(location, value);
	}
	
	public void setUniform(int location, Vec3f value)
	{
		GL20.glUniform3f(location, value.x, value.y, value.z);
	}
	
	public void setUniform(int location, Vec3d value)
	{
		GL20.glUniform3f(location, (float) value.x, (float) value.y, (float) value.z);
	}
	
	public void setUniform(int location, Mat4f value)
	{
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			FloatBuffer buffer = stack.mallocFloat(4 * 4);
			value.store(buffer);
			GL20.glUniformMatrix4fv(location, false, buffer);
		}
	}
	
	/** Converts the color's RGBA values into values between 0 and 1. */
	public void setUniform(int location, Color value)
	{
		GL20.glUniform4f(location, value.getRed() / 256.0f, value.getGreen() / 256.0f, value.getBlue() / 256.0f, value.getAlpha() / 256.0f);
	}
	

	
}
