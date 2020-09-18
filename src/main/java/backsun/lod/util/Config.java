package backsun.lod.util;

import java.lang.reflect.Method;

/**
 * 
 * @author James Seibel
 * @version 09-17-2020
 */
public class Config
{
	
	public static boolean zoomMode()
	{
		//TODO move this code so that we don't
		// have to find the class and method each time
		// we want the zoom status
		try
		{
			Class<?> ofConfig = Class.forName("OfConfigReflector");
			Method zoomMethod = ofConfig.getMethod("zoomMode");
			
			return (boolean) zoomMethod.invoke(ofConfig);
		}
		catch(Exception e)
		{
			// the only way any exceptions should be thrown is if
			// Optifine isn't installed or the method's name was
			// changed.
			
			System.err.println(e);
			return false;
		}
	}
	
	
	public static boolean isDynamicFov()
	{
		//TODO move this code so that we don't
		// have to find the class and method each time
		// we want the zoom status
		try
		{
			Class<?> ofConfig = Class.forName("OfConfigReflector");
			Method dynamicMethod = ofConfig.getMethod("isDynamicFov");
			
			return (boolean) dynamicMethod.invoke(ofConfig);
		}
		catch(Exception e)
		{
			// the only way any exceptions should be thrown is if
			// Optifine isn't installed or the method's name was
			// changed.
			
			System.err.println(e);
			return false;
		}
	}
}
