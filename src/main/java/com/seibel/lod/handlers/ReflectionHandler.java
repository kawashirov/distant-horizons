package com.seibel.lod.handlers;

import java.lang.reflect.Field;

import com.seibel.lod.enums.FogQuality;

import net.minecraft.client.Minecraft;

/**
 * This object is used to get variables from methods
 * where they are private. Specifically the fog setting
 * in Optifine.
 * 
 * @author James Seibel
 * @version 7-03-2021
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
		Field[] optionFields = mc.options.getClass().getDeclaredFields();
				
		// try and find the ofFogType variable in gameSettings
		for(Field field : optionFields)
		{
			if(field.getName().equals("ofFogType"))
			{
				ofFogField = field;
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
			return FogQuality.FANCY;
		}
		
		int returnNum = 0;
		
		try
		{
			returnNum = (int)ofFogField.get(mc.options);
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		
		switch (returnNum)
		{
			// optifine's "default" option,
			// it should never be called in this case
			case 0:
				return FogQuality.FAST;
				
			// normal options
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
