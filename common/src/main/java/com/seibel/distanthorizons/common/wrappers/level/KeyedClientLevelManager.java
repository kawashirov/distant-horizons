package com.seibel.distanthorizons.common.wrappers.level;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.level.IServerKeyedClientLevel;
import com.seibel.distanthorizons.core.level.IKeyedClientLevelManager;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Objects;

public class KeyedClientLevelManager implements IKeyedClientLevelManager
{
	public static final KeyedClientLevelManager INSTANCE = new KeyedClientLevelManager();
	
	/** This is set and managed by the ClientApi for servers with support for DH. */
	private IServerKeyedClientLevel overrideWrapper = null;
	private boolean useOverrideWrapper = false;
	
	
	//=============//
	// constructor //
	//=============//
	
	private KeyedClientLevelManager() { }
	
	
	
	//======================//
	// level override logic //
	//======================//
	
	@Override
	public void setServerKeyedLevel(IServerKeyedClientLevel clientLevel) { this.overrideWrapper = clientLevel; }
	@Override
	public IServerKeyedClientLevel getOverrideWrapper() { return this.overrideWrapper; }
	
	@Override
	public IServerKeyedClientLevel getServerKeyedLevel(ILevelWrapper level, String serverLevelKey)
	{
		Objects.requireNonNull(level);
		Objects.requireNonNull(serverLevelKey);
		return new ServerKeyedClientLevel((ClientLevel) level.getWrappedMcObject(), serverLevelKey);
	}
	
	
	@Override
	public void setUseOverrideWrapper(boolean useOverrideWrapper) { this.useOverrideWrapper = useOverrideWrapper; }
	@Override
	public boolean getUseOverrideWrapper() { return this.useOverrideWrapper; }
	
	
	
}
