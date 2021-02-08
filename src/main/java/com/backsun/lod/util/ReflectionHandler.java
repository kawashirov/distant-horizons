package com.backsun.lod.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import com.backsun.lod.util.fog.FogQuality;

import net.minecraft.client.Minecraft;

/**
 * This object is used to get variables from methods
 * where they are private.
 * 
 * @author James Seibel
 * @version 09-21-2020
 */
public class ReflectionHandler
{
	public Method fovMethod = null;
	public Field ofFogField = null;
	
	
	public ReflectionHandler()
	{
		setupFovMethod();
		setupFogField();
	}
	
	
	
	
	/**
	 * This sets the "getFOVModifier" method from the 
	 * minecraft "EntityRenderer" class, so that we can get
	 * the FOV of the player at any time.
	 * 
	 * This is required since Minecraft is obfuscated so
	 * we can't just look for 'getFOVModifier'
	 * we have to search for it based on its parameters and
	 * return type; which luckily are unique in the EntityRenderer
	 * class.
	 */
	private void setupFovMethod()
	{
		// get every method from the entity renderer
		Method[] methods = Minecraft.getMinecraft().entityRenderer.getClass().getDeclaredMethods();
		
		Class<?> returnType;
		Parameter[] params;
		Method returnMethod = null;
		
		for(Method m : methods)
		{
			returnType = m.getReturnType();
			params = m.getParameters();
			
			// see if this method has the same return type
			// and parameters as the 'getFOVModifier' method. 
			if (returnType.equals(float.class) && 
					params.length == 2 && 
					params[0].getType().equals(float.class) &&
					params[1].getType().equals(boolean.class))
			{
				
				// only accept the first method that we find
				if (returnMethod == null)
				{
					returnMethod = m;
				}
				else
				{
					// we found a second method that matches the 
					// outline we were looking for,
					// to prevent unexpected behavior 
					// dont't set fovMethod.
					
					// Since we aren't sure that 
					// this method is the right 
					// one, we may accidently mess 
					// up the entityRender by invoking
					// it and we probably wouldn't get 
					// the FOV from it anyway.
					
					System.err.println("Error: a second method that matches the parameters and return typ of 'getFOVModifier' was found, LODs won't be rendered.");
					
					return;
				}
			}
		}
		
		// only set the method once we have gone through
		// the whole array of methods, just to
		// make sure we have the right one.
		fovMethod = returnMethod;
		// set up the method so we can invoke it later
		fovMethod.setAccessible(true);
	}
	
	/**
	 * Similar to setupFovMethod.
	 */
	private void setupFogField()
	{
		// get every variable from the entity renderer
		Field[] vars = Minecraft.getMinecraft().gameSettings.getClass().getDeclaredFields();
				
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
	public FogQuality getFogType()
	{
		if (ofFogField == null)
		{
			// either optifine isn't installed,
			// the variable name was changed,
			// or the setup method wasn't called yet.
			return FogQuality.UNKNOWN;
		}
		
		int returnNum = 0;
		
		try
		{
			returnNum = (int)ofFogField.get(Minecraft.getMinecraft().gameSettings);
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			System.out.println(e);
		}
		
		switch (returnNum)
		{
			case 0:
				return FogQuality.UNKNOWN;
			case 1:
				return FogQuality.FAST;
			case 2:
				return FogQuality.FANCY;
			case 3:
				return FogQuality.OFF;
				
			default:
				return FogQuality.UNKNOWN;
		}
	}
	
	
	/**
	 * Gets the FOV used by the EntityRender.
	 */
	public float getFov(Minecraft mc, float partialTicks, boolean useFovSetting)
	{
		try
		{
			return (float)fovMethod.invoke(mc.entityRenderer, new Object[]{partialTicks, useFovSetting});
		}
		catch(InvocationTargetException | IllegalAccessException | IllegalArgumentException e)
		{
			// hopefully this should never be called
			System.out.println(e);
		}
		
		return 0.0f;
	}
	
}
