/**
 * This class reads info from Optifine's config.
 * 
 * This class is required since Optifine's config is
 * in the default package which means that named
 * packages (the rest of my code) can't access it normally.
 * To access any of the methods here reflection must be used.
 * 
 * @author James Seibel
 * @version 09-17-2020
 */
public class OfConfigReflector
{
	/*
	example of how to call methods from this object.

	try
	{
		Class<?> ofConfig = Class.forName("OfConfig");
		Method zoomMethod = ofConfig.getMethod("zoomMode");
		
		return zoomMethod.invoke(ofConfig);
	}
	catch(Exception e)
	{
		
	}
	*/
	
	public static boolean zoomMode()
	{
		return Config.zoomMode;
	}
	
	public static boolean isDynamicFov()
	{
		return Config.isDynamicFov();
	}
	
}
