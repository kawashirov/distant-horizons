package com.seibel.distanthorizons.fabric.wrappers.modAccessor;

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IBCLibAccessor;
#if MC_1_16_5 || MC_1_17_1 || MC_1_20_4 // These versions either don't have BCLib, or the implementation is different
#elif MC_1_18_2
import ru.bclib.config.ClientConfig;
import ru.bclib.config.Configs;
#else
import org.betterx.bclib.config.ClientConfig;
import org.betterx.bclib.config.Configs;
#endif

public class BCLibAccessor implements IBCLibAccessor
{
	@Override
	public String getModName() { return "BCLib"; }
	
	public void setRenderCustomFog(boolean newValue)
	{
		#if !(MC_1_16_5 || MC_1_17_1 || MC_1_20_4) // These versions either don't have BCLib, or the implementation is different
		
		// Change the value of CUSTOM_FOG_RENDERING in the bclib client config
		// This disabled fog from rendering within bclib
		Configs.CLIENT_CONFIG.set(ClientConfig.CUSTOM_FOG_RENDERING, newValue);
		#endif
	}
	
}