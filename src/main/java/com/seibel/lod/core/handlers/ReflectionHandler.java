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

package com.seibel.lod.core.handlers;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.rendering.FogDrawMode;
import com.seibel.lod.core.objects.math.Mat4f;

/**
 * A singleton used to get variables from methods
 * where they are private or potentially absent. 
 * Specifically the fog setting in Optifine or the
 * presence/absence of other mods.
 * 
 * @author James Seibel
 * @version 11-26-2021
 */
public class ReflectionHandler implements IReflectionHandler
{
	private static final Logger LOGGER = LogManager.getLogger(ModInfo.NAME + "-" + ReflectionHandler.class.getSimpleName());
	
	private static ReflectionHandler instance;
	
	private Field ofFogField = null;
	private final Object mcOptionsObject;
	
	
	
	private ReflectionHandler(Field[] optionFields, Object newMcOptionsObject)
	{
		mcOptionsObject = newMcOptionsObject;
		
		setupFogField(optionFields);
	}
	
	/**
	 * @param optionFields the fields that should contain "ofFogType"
	 * @param newMcOptionsObject the object instance that contains "ofFogType"
	 * @return the ReflectionHandler just created
	 * @throws IllegalStateException if a ReflectionHandler already exists
	 */
	public static ReflectionHandler createSingleton(Field[] optionFields, Object newMcOptionsObject) throws IllegalStateException
	{
		if (instance != null)
		{
			throw new IllegalStateException();	
		}
		
		instance = new ReflectionHandler(optionFields, newMcOptionsObject);
		return instance;
	}
	
	
	
	
	/** finds the Optifine fog type field */
	private void setupFogField(Field[] optionFields)
	{
		// try and find the ofFogType variable in gameSettings
		for (Field field : optionFields)
		{
			if (field.getName().equals("ofFogType"))
			{
				ofFogField = field;
				return;
			}
		}
		
		// we didn't find the field,
		// either optifine isn't installed, or
		// optifine changed the name of the variable
		LOGGER.info(ReflectionHandler.class.getSimpleName() + ": unable to find the Optifine fog field. If Optifine isn't installed this can be ignored.");
	}
	
	
	/**
	 * Get what type of fog optifine is currently set to render.
	 * @return the fog quality
	 */
	@Override
	public FogDrawMode getFogDrawMode()
	{
		if (ofFogField == null)
		{
			// either optifine isn't installed,
			// the variable name was changed, or
			// the setup method wasn't called yet.
			return FogDrawMode.FOG_ENABLED;
		}
		
		int returnNum = 0;
		
		try
		{
			returnNum = (int) ofFogField.get(mcOptionsObject);
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		
		switch (returnNum)
		{
		default:
		case 0:
			// optifine's "default" option,
			// it should never be called in this case
			
			// normal options
		case 1: // fast
		case 2: // fancy
			return FogDrawMode.FOG_ENABLED;
		case 3: // off
			return FogDrawMode.FOG_DISABLED;
		}
	}
	
	
	
	/** Detect if Vivecraft is present. Attempts to find the "VRRenderer" class. */
	@Override
	public boolean vivecraftPresent()
	{
		try
		{
			Class.forName("org.vivecraft.provider.VRRenderer");
			return true;
		}
		catch (ClassNotFoundException ignored)
		{
			LOGGER.info(ReflectionHandler.class.getSimpleName() + ": Vivecraft not detected.");
		}
		return false;
	}
	
	/**
	 * Modifies the projection matrix's clip planes.
	 * The projection matrix must be in column-major format.
	 * 
	 * @param projectionMatrix The projection matrix to be modified.
	 * @param newNearClipPlane the new near clip plane value.
	 * @param newFarClipPlane the new far clip plane value.
	 * @return The modified matrix.
	 */ 
	@Override
	public Mat4f ModifyProjectionClipPlanes(Mat4f projectionMatrix, float newNearClipPlane, float newFarClipPlane)
	{
		// find the matrix values.
		float nearMatrixValue = -((newFarClipPlane + newNearClipPlane) / (newFarClipPlane - newNearClipPlane));
		float farMatrixValue = -((2 * newFarClipPlane * newNearClipPlane) / (newFarClipPlane - newNearClipPlane));
		
		try
		{
			// TODO this was originally created before we had the Mat4f object,
			// so this doesn't need to be done with reflection anymore.
			// And should be moved to RenderUtil
			
			// get the fields of the projectionMatrix
			Field[] fields = projectionMatrix.getClass().getDeclaredFields();
			// bypass the security protections on the fields that encode near and far plane values.
			fields[10].setAccessible(true);
			fields[11].setAccessible(true);
			// Change the values of the near and far plane.
			fields[10].set(projectionMatrix, nearMatrixValue);
			fields[11].set(projectionMatrix, farMatrixValue);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return projectionMatrix;
	}
}
