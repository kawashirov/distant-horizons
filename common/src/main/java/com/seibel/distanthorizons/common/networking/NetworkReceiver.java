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

/*
#if MC_1_16_5
import me.shedaniel.architectury.networking.NetworkManager;
#else
import dev.architectury.networking.NetworkManager;
#endif
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
*/

/**
 * @author Ran
 */
// Comment: What does the 'server' side mean? Dedicated server? Or does it include the internal server?
// (I removed the hookup that calls the register method, since I'm not sure what it is doing yet)
public class NetworkReceiver {
    /*
    public void register_Client() {
        NetworkManager.registerReceiver(NetworkManager.serverToClient(), Networking.RESOURCE_LOCATION, new ClientReceiver());
    }

    public void register_Server() {
        NetworkManager.registerReceiver(NetworkManager.clientToServer(), Networking.RESOURCE_LOCATION, new ServerReceiver());
    }

    static class ServerReceiver implements NetworkManager.NetworkReceiver {
        @Override
        public void receive(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
            com.seibel.distanthorizons.common.networking.NetworkHandler.receivePacketServer(buf, context.getPlayer());
        }
    }

    static class ClientReceiver implements NetworkManager.NetworkReceiver {
        @Override
        public void receive(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
            com.seibel.distanthorizons.common.networking.NetworkHandler.receivePacketClient(Minecraft.getInstance(), buf, context.getPlayer());
        }
    }
     */
}
