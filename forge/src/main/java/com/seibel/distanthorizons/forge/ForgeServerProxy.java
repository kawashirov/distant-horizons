package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerLevel;
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
    private static LevelAccessor GetLevel(WorldEvent e) { return e.getWorld(); }
    #else
    private static LevelAccessor GetLevel(LevelEvent e) { return e.getLevel(); }
    #endif

    private final ServerApi serverApi = ServerApi.INSTANCE;
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    private final boolean isDedicated;
    public static Supplier<Boolean> isGenerationThreadChecker = null;

    public ForgeServerProxy(boolean isDedicated)
	{
		this.isDedicated = isDedicated;
		isGenerationThreadChecker = BatchGenerationEnvironment::isCurrentThreadDistantGeneratorThread;
	}
    private boolean isValidTime()
	{
		if (this.isDedicated)
		{
			return true;
		}
	
		//FIXME: This may cause init issue...
		return !(Minecraft.getInstance().screen instanceof TitleScreen);
	}
    private ServerLevelWrapper getLevelWrapper(ServerLevel level) { return ServerLevelWrapper.getWrapper(level); }


    // ServerTickEvent (at end)
    @SubscribeEvent
    public void serverTickEvent(TickEvent.ServerTickEvent event)
	{
		if (event.phase == TickEvent.Phase.END)
		{
			if (this.isValidTime())
			{
				this.serverApi.serverTickEvent();
			}
		}
	}

    // ServerWorldLoadEvent
    @SubscribeEvent
    public void dedicatedWorldLoadEvent(#if MC_1_16_5 || MC_1_17_1 FMLServerAboutToStartEvent #else ServerAboutToStartEvent #endif event)
	{
		if (this.isValidTime())
		{
			this.serverApi.serverLoadEvent(this.isDedicated);
		}
	}

    // ServerWorldUnloadEvent
    @SubscribeEvent
    public void serverWorldUnloadEvent(#if MC_1_16_5 || MC_1_17_1 FMLServerStoppingEvent #else ServerStoppingEvent #endif event)
	{
		if (this.isValidTime())
		{
			this.serverApi.serverUnloadEvent();
		}
	}

    // ServerLevelLoadEvent
    @SubscribeEvent
	#if PRE_MC_1_19_2
	public void serverLevelLoadEvent(WorldEvent.Load event)
	#else
    public void serverLevelLoadEvent(LevelEvent.Load event)
	#endif
    {
        if (isValidTime()) {
            if (GetLevel(event) instanceof ServerLevel) {
                serverApi.serverLevelLoadEvent(getLevelWrapper((ServerLevel) GetLevel(event)));
            }
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
        if (isValidTime()) {
            if (GetLevel(event) instanceof ServerLevel) {
                serverApi.serverLevelUnloadEvent(getLevelWrapper((ServerLevel) GetLevel(event)));
            }
        }
    }

    @SubscribeEvent
    public void serverChunkLoadEvent(ChunkEvent.Load event)
    {
        if (isValidTime()) {
            if (GetLevel(event) instanceof ServerLevel) {
                ServerLevelWrapper wrappedLevel = ServerLevelWrapper.getWrapper((ServerLevel) GetLevel(event));
                IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetLevel(event), wrappedLevel);
                serverApi.serverChunkLoadEvent(chunk, getLevelWrapper((ServerLevel) GetLevel(event)));
            }
        }
    }
    @SubscribeEvent
    public void serverChunkSaveEvent(ChunkEvent.Unload event)
    {
        if (isValidTime()) {
            if (GetLevel(event) instanceof ServerLevel) {
                ServerLevelWrapper wrappedLevel = ServerLevelWrapper.getWrapper((ServerLevel) GetLevel(event));
                IChunkWrapper chunk = new ChunkWrapper(event.getChunk(), GetLevel(event), wrappedLevel);
                serverApi.serverChunkSaveEvent(chunk, getLevelWrapper((ServerLevel) GetLevel(event)));
            }
        }
    }
}
