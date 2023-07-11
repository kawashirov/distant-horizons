package com.seibel.distanthorizons.fabric.mixins.client;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

#if POST_MC_1_20_1
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.world.level.chunk.LevelChunk;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
#endif

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener
{
    @Shadow
    private ClientLevel level;
	
	
	
    /** THIS EXPLANATION IS WRITTEN BY FABRIC.
     * An explanation why we unload entities during onGameJoin: (On in our remapping name case, handleLogin(TODO: CHECK))
     * Proxies such as Waterfall may send another Game Join packet if entity meta rewrite is disabled, so we will cover ourselves.
     * Velocity by default will send a Game Join packet when the player changes servers, which will create a new client world.
     * Also anyone can send another GameJoinPacket at any time, so we need to watch out.
     */
    @Inject(method = "handleLogin", at = @At("HEAD"))
	void onHandleLoginStart(CallbackInfo ci)
	{
		// not the best way to notify Core that we are no longer in the previous world, but it will have to do for now
		ClientApi.INSTANCE.onClientOnlyDisconnected();
	}
	@Inject(method = "handleLogin", at = @At("RETURN"))
	void onHandleLoginEnd(CallbackInfo ci) { ClientApi.INSTANCE.onClientOnlyConnected(); }
	
	@Inject(method = "handleRespawn", at = @At("HEAD"))
	void onHandleRespawnStart(CallbackInfo ci) { ClientApi.INSTANCE.clientLevelUnloadEvent(ClientLevelWrapper.getWrapper(level)); }
	@Inject(method = "handleRespawn", at = @At("RETURN"))
	void onHandleRespawnEnd(CallbackInfo ci) { ClientApi.INSTANCE.clientLevelLoadEvent(ClientLevelWrapper.getWrapper(level)); }

	#if PRE_MC_1_19_4
	@Inject(method = "cleanup", at = @At("HEAD"))
	#else
	@Inject(method = "close", at = @At("HEAD"))
	#endif
	void onCleanupStart(CallbackInfo ci)
	{
		// TODO which unload method should be used? do we need both?
		if (level != null)
		{
			ClientApi.INSTANCE.clientLevelUnloadEvent(ClientLevelWrapper.getWrapper(level));
		}
	}

	#if POST_MC_1_20_1
	@Inject(method = "enableChunkLight", at = @At("TAIL"))
	void onEnableChunkLight(LevelChunk chunk, int x, int z, CallbackInfo ci)
	{
		IClientLevelWrapper clientLevel = ClientLevelWrapper.getWrapper((ClientLevel) chunk.getLevel());
		ClientApi.INSTANCE.clientChunkLoadEvent(new ChunkWrapper(chunk, chunk.getLevel(), clientLevel), clientLevel);
	}

	#endif

}
