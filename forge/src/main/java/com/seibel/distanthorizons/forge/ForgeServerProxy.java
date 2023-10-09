package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.util.ProxyUtil;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent;
#if PRE_MC_1_19_2
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
#else
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
#endif
import net.minecraftforge.eventbus.api.SubscribeEvent;

#if MC_1_16_5
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
#elif MC_1_17_1
import net.minecraftforge.fmlserverevents.FMLServerAboutToStartEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppingEvent;
#else
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
#endif


import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class ForgeServerProxy
{
	#if PRE_MC_1_19_2
	private static LevelAccessor GetEventLevel(WorldEvent e) { return e.getWorld(); }
	#else
	private static LevelAccessor GetEventLevel(LevelEvent e) { return e.getLevel(); }
    #endif
	
	private final ServerApi serverApi = ServerApi.INSTANCE;
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private final boolean isDedicated;
	public static Supplier<Boolean> isGenerationThreadChecker = null;
	
	
	//=============//
	// constructor //
	//=============//
	
	public ForgeServerProxy(boolean isDedicated)
	{
		this.isDedicated = isDedicated;
		isGenerationThreadChecker = BatchGenerationEnvironment::isCurrentThreadDistantGeneratorThread;
	}
	
	
	
	//========//
	// events //
	//========//
	
	// ServerTickEvent (at end)
	@SubscribeEvent
	public void serverTickEvent(TickEvent.ServerTickEvent event)
	{
		if (event.phase == TickEvent.Phase.END)
		{
			this.serverApi.serverTickEvent();
		}
	}
	
	// ServerWorldLoadEvent
	@SubscribeEvent
	public void dedicatedWorldLoadEvent(#if MC_1_16_5 || MC_1_17_1 FMLServerAboutToStartEvent #else ServerAboutToStartEvent #endif event)
	{
		this.serverApi.serverLoadEvent(this.isDedicated);
	}
	
	// ServerWorldUnloadEvent
	@SubscribeEvent
	public void serverWorldUnloadEvent(#if MC_1_16_5 || MC_1_17_1 FMLServerStoppingEvent #else ServerStoppingEvent #endif event)
	{
		this.serverApi.serverUnloadEvent();
	}
	
	// ServerLevelLoadEvent
	@SubscribeEvent
	#if PRE_MC_1_19_2
	public void serverLevelLoadEvent(WorldEvent.Load event)
	#else
	public void serverLevelLoadEvent(LevelEvent.Load event)
	#endif
	{
		if (GetEventLevel(event) instanceof ServerLevel)
		{
			this.serverApi.serverLevelLoadEvent(this.getServerLevelWrapper((ServerLevel) GetEventLevel(event)));
		}
	}
	
	// ServerLevelUnloadEvent
	@SubscribeEvent
	#if PRE_MC_1_19_2
	public void serverLevelUnloadEvent(WorldEvent.Unload event)
	#else
	public void serverLevelUnloadEvent(LevelEvent.Unload event)
    #endif
	{
		if (GetEventLevel(event) instanceof ServerLevel)
		{
			this.serverApi.serverLevelUnloadEvent(this.getServerLevelWrapper((ServerLevel) GetEventLevel(event)));
		}
	}
	
	@SubscribeEvent
	public void serverChunkLoadEvent(ChunkEvent.Load event)
	{
		ILevelWrapper levelWrapper = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		
		IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetEventLevel(event), levelWrapper);
		this.serverApi.serverChunkLoadEvent(chunk, levelWrapper);
	}
	@SubscribeEvent
	public void serverChunkSaveEvent(ChunkEvent.Unload event)
	{
		ILevelWrapper levelWrapper = ProxyUtil.getLevelWrapper(GetEventLevel(event));
		
		IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetEventLevel(event), levelWrapper);
		this.serverApi.serverChunkSaveEvent(chunk, levelWrapper);
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	private static ServerLevelWrapper getServerLevelWrapper(ServerLevel level) { return ServerLevelWrapper.getWrapper(level); }
	
	
}
