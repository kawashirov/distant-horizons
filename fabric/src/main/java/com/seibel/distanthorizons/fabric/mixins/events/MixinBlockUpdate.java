/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.fabric.mixins.events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * If someone has a better way to do this then please let me know.
 *
 * @author Ran
 */
@Mixin(ClientboundBlockUpdatePacket.class)
@Deprecated
public abstract class MixinBlockUpdate
{
	@Shadow public abstract BlockPos getPos();
	
	//TODO: Check if this event will be needed in new reworked system
//    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V", at = @At("TAIL"))
//    private void onBlockUpdate(ClientGamePacketListener clientGamePacketListener, CallbackInfo ci) {
//        Main.client_proxy.blockChangeEvent(Minecraft.getInstance().player.clientLevel, this.getPos());
//    }
}
