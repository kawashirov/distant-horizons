package com.seibel.distanthorizons.fabric.wrappers.modAccessor;

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IImmersivePortalsAccessor;

public class ImmersivePortalsAccessor implements IImmersivePortalsAccessor
{
	@Override
	public String getModName()
	{
		return "ImmersivePortals-Fabric";
	}
	
	public float partialTicks;
	
}
