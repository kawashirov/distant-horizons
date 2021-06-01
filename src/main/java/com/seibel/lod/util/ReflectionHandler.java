package com.seibel.lod.util;

import java.lang.reflect.Field;

import com.seibel.lod.enums.FogQuality;

import net.minecraft.client.Minecraft;

/**
 * This object is used to get variables from methods
 * where they are private.
 * 
 * @author James Seibel
 * @version 02-18-2021
 */
public class ReflectionHandler
{
	private Minecraft mc = Minecraft.getInstance();
	
	public Field ofFogField = null;
	
	
	public ReflectionHandler()
	{
		setupFogField();
	}
	
	/**
	 * Similar to setupFovMethod.
	 */
	private void setupFogField()
	{
		// get every variable from the entity renderer
		Field[] vars = mc.gameSettings.getClass().getDeclaredFields();
				
		// try and find the ofFogType variable in gameSettings
		for(Field f : vars)
		{
			if(f.getName().equals("ofFogType"))
			{
				ofFogField = f;
				return;
			}
		}
		
		// we didn't find the field,
		// either optifine isn't installed, or
		// optifine changed the name of the variable
		ofFogField = null;
	}
	
	
	
	
	
	/**
	 * Get what type of fog optifine is currently set to render.
	 */
	public FogQuality getFogQuality()
	{
		if (ofFogField == null)
		{
			// either optifine isn't installed,
			// the variable name was changed, or
			// the setup method wasn't called yet.
			return FogQuality.OFF;
		}
		
		int returnNum = 0;
		
		try
		{
			returnNum = (int)ofFogField.get(mc.gameSettings);
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		
		switch (returnNum)
		{
			case 0:
				return FogQuality.FAST;
			case 1:
				return FogQuality.FAST;
			case 2:
				return FogQuality.FANCY;
			case 3:
				return FogQuality.OFF;
				
			default:
				return FogQuality.FAST;
		}
	}
	
}
