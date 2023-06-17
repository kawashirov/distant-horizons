/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.distanthorizons.common.networking;

import com.seibel.distanthorizons.coreapi.ModInfo;
//#if MC_1_16_5
//import me.shedaniel.architectury.networking.NetworkManager;
//#else
//import dev.architectury.networking.NetworkManager;
//#endif
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * This class holds most of the networking code for the mod.
 * @author Ran
 */
public class Networking {
    public static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation("lod", "data");

    public static FriendlyByteBuf createNew() {
        // TODO: Probably replace the Unpooled.buffer()
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(ModInfo.PROTOCOL_VERSION);
        return buf;
    }

    /**
     * Sends a packet to a player.
     *
     * @param player the player to send the packet to
     * @param buf the payload of the packet.
     */
    public static void send(ServerPlayer player, FriendlyByteBuf buf) {
//        NetworkManager.sendToPlayer(player, RESOURCE_LOCATION, buf);
    }

    /**
     * Sends a packet to the connected server.
     *
     * @param buf the payload of the packet
     * @throws IllegalStateException if the client is not connected to a server
     */
    public static void send(FriendlyByteBuf buf) throws IllegalStateException {
//        NetworkManager.sendToServer(RESOURCE_LOCATION, buf);
    }

}
