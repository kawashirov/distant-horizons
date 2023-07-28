package com.seibel.distanthorizons.forge.mixins.client;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener
{
	// TODO update fabric version as well
	
    @Inject(method = "handleLogin", at = @At("HEAD"))
    void onHandleLoginStart(CallbackInfo ci)
    {
        // not the best way to notify Core that we are no longer in the previous world, but it will have to do for now
        ClientApi.INSTANCE.onClientOnlyDisconnected();
    }
    @Inject(method = "handleLogin", at = @At("RETURN"))
    void onHandleLoginEnd(CallbackInfo ci) { ClientApi.INSTANCE.onClientOnlyConnected(); }
	
}
