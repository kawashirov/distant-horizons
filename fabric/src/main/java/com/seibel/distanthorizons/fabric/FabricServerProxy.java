package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * This handles all events sent to the server,
 * and is the starting point for most of the mod.
 *
 * @author Ran
 * @author Tomlee
 * @version 5-11-2022
 */
public class FabricServerProxy
{
	private static final ServerApi SERVER_API = ServerApi.INSTANCE;
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final boolean isDedicated;
	public static Supplier<Boolean> isGenerationThreadChecker = null;
	
	
	
	public FabricServerProxy(boolean isDedicated)
	{
		this.isDedicated = isDedicated;
	}
	
	
	
	private boolean isValidTime()
	{
		if (isDedicated)
		{
			return true;
		}
		
		//FIXME: This may cause init issue...
		return !(Minecraft.getInstance().screen instanceof TitleScreen);
	}
	
	private ClientLevelWrapper getClientLevelWrapper(ClientLevel level) { return ClientLevelWrapper.getWrapper(level); }
	private ServerLevelWrapper getServerLevelWrapper(ServerLevel level) { return ServerLevelWrapper.getWrapper(level); }
	private ServerPlayerWrapper getServerPlayerWrapper(ServerPlayer player) { return ServerPlayerWrapper.getWrapper(player); }

	/** Registers Fabric Events */
	public void registerEvents()
	{
		LOGGER.info("Registering Fabric Server Events");
		isGenerationThreadChecker = BatchGenerationEnvironment::isCurrentThreadDistantGeneratorThread;
		
		/* Register the mod needed event callbacks */
		
		// ServerTickEvent
		ServerTickEvents.END_SERVER_TICK.register((server) -> SERVER_API.serverTickEvent());
		
		// ServerWorldLoadEvent
		//TODO: Check if both of these use the correct timed events. (i.e. is it 'ed' or 'ing' one?)
		ServerLifecycleEvents.SERVER_STARTING.register((server) -> 
		{
			if (isValidTime())
			{
				ServerApi.INSTANCE.serverLoadEvent(isDedicated);
			}
		});
		// ServerWorldUnloadEvent
		ServerLifecycleEvents.SERVER_STOPPED.register((server) -> 
		{
			if (isValidTime())
			{
				ServerApi.INSTANCE.serverUnloadEvent();
			}
		});
		
		// ServerLevelLoadEvent
		ServerWorldEvents.LOAD.register((server, level) -> 
		{
			if (isValidTime())
			{
				ServerApi.INSTANCE.serverLevelLoadEvent(getServerLevelWrapper(level));
			}
		});
		// ServerLevelUnloadEvent
		ServerWorldEvents.UNLOAD.register((server, level) -> 
		{
			if (isValidTime())
			{
				ServerApi.INSTANCE.serverLevelUnloadEvent(getServerLevelWrapper(level));
			}
		});
		
		// ServerChunkLoadEvent
		ServerChunkEvents.CHUNK_LOAD.register((server, chunk) -> 
		{
			ILevelWrapper level = getServerLevelWrapper((ServerLevel) chunk.getLevel());
			if (isValidTime())
			{
				ServerApi.INSTANCE.serverChunkLoadEvent(
						new ChunkWrapper(chunk, chunk.getLevel(), level),
						level);
			}
		});
		// ServerChunkSaveEvent - Done in MixinChunkMap

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
		{
			if (isValidTime())
			{
				ServerApi.INSTANCE.serverPlayerJoinEvent(getServerPlayerWrapper(handler.player));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
		{
			if (isValidTime())
			{
				ServerApi.INSTANCE.serverPlayerDisconnectEvent(getServerPlayerWrapper(handler.player));
			}
		});
	}
}
