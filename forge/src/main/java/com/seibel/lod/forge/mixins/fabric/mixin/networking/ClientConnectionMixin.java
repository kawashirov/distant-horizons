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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.seibel.lod.forge.fabric.impl.networking.ChannelInfoHolder;
import com.seibel.lod.forge.fabric.impl.networking.DisconnectPacketSource;
import com.seibel.lod.forge.fabric.impl.networking.NetworkHandlerExtensions;
import com.seibel.lod.forge.fabric.impl.networking.PacketCallbackListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
#if PRE_MC_1_19
import net.minecraft.network.chat.TranslatableComponent;
#endif
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;

@Mixin(Connection.class)
abstract class ClientConnectionMixin implements ChannelInfoHolder {
	@Shadow
	private PacketListener packetListener;

	@Shadow
	public abstract void send(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> callback);

	@Shadow
	public abstract void disconnect(Component disconnectReason);

	@Unique
	private Collection<ResourceLocation> playChannels;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void initAddedFields(PacketFlow side, CallbackInfo ci) {
		this.playChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	// Must be fully qualified due to mixin not working in production without it
	@SuppressWarnings("UnnecessaryQualifiedMemberReference")
	@Redirect(method = "Lnet/minecraft/network/Connection;exceptionCaught(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Throwable;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V"))
	private void resendOnExceptionCaught(Connection self, Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> listener) {
		PacketListener handler = this.packetListener;

		#if PRE_MC_1_19
		if (handler instanceof DisconnectPacketSource) {
			this.send(((DisconnectPacketSource) handler).createDisconnectPacket(new TranslatableComponent("disconnect.genericReason")), listener);
		} else {
			this.disconnect(new TranslatableComponent("disconnect.genericReason")); // Don't send packet if we cannot send proper packets
		}
		#else
		if (handler instanceof DisconnectPacketSource) {
			this.send(((DisconnectPacketSource) handler).createDisconnectPacket(Component.translatable("disconnect.genericReason")), listener);
		} else {
			this.disconnect(Component.translatable("disconnect.genericReason")); // Don't send packet if we cannot send proper packets
		}
		#endif
	}

	@Inject(method = "sendPacket", at = @At(value = "FIELD", target = "Lnet/minecraft/network/Connection;sentPackets:I"))
	private void checkPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo ci) {
		if (this.packetListener instanceof PacketCallbackListener) {
			((PacketCallbackListener) this.packetListener).sent(packet);
		}
	}

	#if PRE_MC_1_19
	@Inject(method = "channelInactive", at = @At("HEAD"))
	private void handleDisconnect(ChannelHandlerContext channelHandlerContext, CallbackInfo ci) throws Exception {
		if (packetListener instanceof NetworkHandlerExtensions) { // not the case for client/server query
			((NetworkHandlerExtensions) packetListener).getAddon().handleDisconnect();
		}
	}
	#else
		//FIXME: error: No obfuscation mapping for @Inject target channelInactive
	#endif

	@Override
	public Collection<ResourceLocation> getPendingChannelsNames() {
		return this.playChannels;
	}
}
