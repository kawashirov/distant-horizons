
/**
 * This class reads info from Optifine's config.
 * 
 * This class is required since Optifine's config is
 * in the default package which means that named
 * packages (the rest of my code) can't access it normally.
 * To access any of the methods here reflection must be used.
 * 
 * @author James Seibel
 *
 */
public class OfConfig
{
	
	public static boolean getZoom()
	{
		return Config.zoomMode;
	}
	
}
