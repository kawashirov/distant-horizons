/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.seibel.lod.forge.mixins.fabric.mixin.networking;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import com.seibel.lod.forge.fabric.impl.networking.DisconnectPacketSource;
import com.seibel.lod.forge.fabric.impl.networking.NetworkHandlerExtensions;
import com.seibel.lod.forge.fabric.impl.networking.PacketCallbackListener;
import com.seibel.lod.forge.fabric.impl.networking.server.ServerLoginNetworkAddon;

@Mixin(ServerLoginPacketListenerImpl.class)
abstract class ServerLoginNetworkHandlerMixin implements NetworkHandlerExtensions, DisconnectPacketSource, PacketCallbackListener {
	@Shadow
	@Final
	private MinecraftServer server;

	@Shadow
	public abstract void handleAcceptedLogin();

	@Unique
	private ServerLoginNetworkAddon addon;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void initAddon(CallbackInfo ci) {
		this.addon = new ServerLoginNetworkAddon((ServerLoginPacketListenerImpl) (Object) this);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;handleAcceptedLogin()V"))
	private void handlePlayerJoin(ServerLoginPacketListenerImpl handler) {
		// Do not accept the player, thereby moving into play stage until all login futures being waited on are completed
		if (this.addon.queryTick()) {
			this.handleAcceptedLogin();
		}
	}

	@Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
	private void handleCustomPayloadReceivedAsync(ServerboundCustomQueryPacket packet, CallbackInfo ci) {
		// Handle queries
		if (this.addon.handle(packet)) {
			ci.cancel();
		}
	}

	@Redirect(method = "handleAcceptedLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getCompressionThreshold()I", ordinal = 0))
	private int removeLateCompressionPacketSending(MinecraftServer server) {
		return -1;
	}

	@Inject(method = "onDisconnect", at = @At("HEAD"))
	private void handleDisconnection(Component reason, CallbackInfo ci) {
		this.addon.handleDisconnect();
	}

	#if MC_1_16_5
	@Inject(method = "handleAcceptedLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V"))
	private void handlePlayTransitionNormal(CallbackInfo ci) {
		this.addon.handlePlayTransition();
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V"))
	private void handlePlayTransitionDelayed(CallbackInfo ci) {
		this.addon.handlePlayTransition();
	}
	#else
	@Inject(method = "placeNewPlayer", at = @At("HEAD"))
	private void handlePlayTransitionNormal(ServerPlayer player, CallbackInfo ci) {
		this.addon.handlePlayTransition();
	}
	#endif

	@Override
	public void sent(Packet<?> packet) {
		if (packet instanceof ClientboundCustomQueryPacket) {
			this.addon.registerOutgoingPacket((ClientboundCustomQueryPacket) packet);
		}
	}

	@Override
	public ServerLoginNetworkAddon getAddon() {
		return this.addon;
	}

	@Override
	public Packet<?> createDisconnectPacket(Component message) {
		return new ClientboundLoginDisconnectPacket(message);
	}
}
