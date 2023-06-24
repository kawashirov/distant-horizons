package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.networking.Networking;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
#if PRE_MC_1_19
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
#else
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
#endif
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class ForgeServerProxy
{
    #if PRE_MC_1_19
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
    public void dedicatedWorldLoadEvent(ServerAboutToStartEvent event)
	{
		if (this.isValidTime())
		{
			this.serverApi.serverLoadEvent(this.isDedicated);
		}
	}

    // ServerWorldUnloadEvent
    @SubscribeEvent
    public void serverWorldUnloadEvent(ServerStoppingEvent event)
	{
		if (this.isValidTime())
		{
			this.serverApi.serverUnloadEvent();
		}
	}

    // ServerLevelLoadEvent
    @SubscribeEvent
	#if PRE_MC_1_19_1
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
	#if PRE_MC_1_19_1
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

    // This just exists here for testing purposes, it'll be removed in the future
    public void tester(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf payload = Networking.createNew();
            payload.writeInt(1);
            System.out.println("Sending int 1");
            Networking.send(player, payload);
        }
    }
}
