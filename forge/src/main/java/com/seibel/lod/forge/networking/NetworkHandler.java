/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.forge.networking;

import com.seibel.lod.common.networking.NetworkInterface;
import com.seibel.lod.common.networking.Networking;
import com.seibel.lod.forge.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.seibel.lod.forge.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * @author Ran
 */
public class NetworkHandler implements NetworkInterface {
    @Override
    public void register_Client() {
        ClientPlayNetworking.registerGlobalReceiver(Networking.resourceLocation_meow, (client, handler, buf, responseSender) -> {
            com.seibel.lod.common.networking.NetworkHandler.receivePacketClient(client, handler, buf);
        });
    }

    @Override
    public void register_Server() {
        ServerPlayNetworking.registerGlobalReceiver(Networking.resourceLocation_meow, (server, player, handler, buf, responseSender) -> {
            com.seibel.lod.common.networking.NetworkHandler.receivePacketServer(server, player, handler, buf);
        });
    }
}
