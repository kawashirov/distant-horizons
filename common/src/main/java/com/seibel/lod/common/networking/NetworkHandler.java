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
 
package com.seibel.lod.common.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

/**
 * This is packet handler for our mod
 * It basically handles the packets sent from the server & client
 *
 * @author Ran
 */

// TODO: Server sided stuff here
public class NetworkHandler {
    public static void receivePacketClient(Minecraft client, FriendlyByteBuf buf, Player player) {
        // This just exists here for testing purposes, it'll be removed in the future
        System.out.println("Received int " + buf.readInt());
    }

    public static void receivePacketServer(FriendlyByteBuf buf, Player player) {

    }
}
