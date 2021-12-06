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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.lwjgl.opengl.GL20;

import com.seibel.lod.core.api.ClientApi;

/**
 * This object holds a OpenGL reference to a shader
 * and allows for reading in and compiling a shader file.
 * 
 * @author James Seibel
 * @version 11-8-2021
 */
public class LodShader
{	
	/** OpenGL shader ID */
	public final int id;
	
	
	
	/** Creates a shader with specified type. */
	public LodShader(int type)
	{
		id = GL20.glCreateShader(type);
	}
	
	
	
	/**
	 * Loads a shader from file.
	 *
	 * @param type Either GL_VERTEX_SHADER or GL_FRAGMENT_SHADER.
	 * @param path File path of the shader
	 * @param absoluteFilePath If false the file path is relative to the resource jar folder.
	 * @throws Exception if the shader fails to compile 
	 */
	public static LodShader loadShader(int type, String path, boolean absoluteFilePath) throws Exception
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		try
		{
			// open the file
			InputStream in = absoluteFilePath ? new FileInputStream(path) : LodShader.class.getClassLoader().getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			
			// read in the file
			String line;
			while ((line = reader.readLine()) != null)
				stringBuilder.append(line).append("\n");
		}
		catch (IOException e)
		{
			ClientApi.LOGGER.error("Unable to load shader from file [" + path + "]. Error: " + e.getMessage());
		}
		CharSequence shaderFileSource = stringBuilder.toString();
		
		return createShader(type, shaderFileSource);
	}
	
	/**
	 * Creates a shader with the specified type and source.
	 *
	 * @param type   Either GL_VERTEX_SHADER or GL_FRAGMENT_SHADER.
	 * @param source Source of the shader
	 * @throws Exception if the shader fails to compile
	 */
	public static LodShader createShader(int type, CharSequence source) throws Exception
	{
		LodShader shader = new LodShader(type);
		GL20.glShaderSource(shader.id, source);
		shader.compile();
		
		return shader;
	}
	
	/** 
	 * Compiles the shader and checks its status afterwards.
	 * @throws Exception if the shader fails to compile
	 */
	public void compile() throws Exception
	{
		GL20.glCompileShader(id);
		
		// check if the shader compiled
		int status = GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS);
		if (status != GL20.GL_TRUE)
			throw new Exception(GL20.glGetShaderInfoLog(id));
	}
	
}
