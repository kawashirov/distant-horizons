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
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * This is packet handler for our mod
 * It basically handles the packets sent from the server & client
 *
 * @author Ran
 */
public class NetworkHandler {
    // If you need the response sender then tell me
    // I'll add extra code to get the response sender
    public static void receivePacketClient(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf) {
        // TODO: Server sided stuff here
        // You can make client execute something by using client.execute(Runnable)
        // In the fabric docs it says that client.execute is ran on the render thread?
    }

    // If you need the response sender then tell me
    // I'll add extra code to get the response sender
    public static void receivePacketServer(MinecraftServer server, ServerPlayer client, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf) {
        // TODO: Server sided stuff here
    }
}
