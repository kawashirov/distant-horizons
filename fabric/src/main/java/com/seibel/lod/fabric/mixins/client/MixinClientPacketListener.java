package com.seibel.lod.fabric.mixins.client;

import com.seibel.lod.common.wrappers.world.WorldWrapper;
import com.seibel.lod.core.api.internal.a7.ClientApi;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Shadow
    private ClientLevel level;

    /** THIS EXPLANATION IS WRITTEN BY FABRIC.
     * An explanation why we unload entities during onGameJoin: (On in our remapping name case, handleLogin(TODO: CHECK))
     * Proxies such as Waterfall may send another Game Join packet if entity meta rewrite is disabled, so we will cover ourselves.
     * Velocity by default will send a Game Join packet when the player changes servers, which will create a new client world.
     * Also anyone can send another GameJoinPacket at any time, so we need to watch out.
     */
    @Inject(method = "handleLogin", at = @At("HEAD"))
    void onHandleLoginStart(CallbackInfo ci) {
        if (level != null) ClientApi.INSTANCE.clientLevelUnloadEvent(WorldWrapper.getWorldWrapper(level));
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    void onHandleLoginEnd(CallbackInfo ci) {
        ClientApi.INSTANCE.clientLevelLoadEvent(WorldWrapper.getWorldWrapper(level));
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    void onHandleRespawnStart(CallbackInfo ci) {
        ClientApi.INSTANCE.clientLevelUnloadEvent(WorldWrapper.getWorldWrapper(level));
    }
    @Inject(method = "handleRespawn", at = @At("RETURN"))
    void onHandleRespawnEnd(CallbackInfo ci) {
        ClientApi.INSTANCE.clientLevelLoadEvent(WorldWrapper.getWorldWrapper(level));
    }

    @Inject(method = "cleanup", at = @At("HEAD"))
    void onCleanupStart(CallbackInfo ci) {
        if (level != null) ClientApi.INSTANCE.clientLevelUnloadEvent(WorldWrapper.getWorldWrapper(level));
    }
}