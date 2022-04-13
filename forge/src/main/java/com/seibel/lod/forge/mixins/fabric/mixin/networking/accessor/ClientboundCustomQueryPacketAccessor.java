package com.seibel.lod.forge.mixins.fabric.mixin.networking.accessor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundCustomQueryPacket.class)
public interface ClientboundCustomQueryPacketAccessor {
    #if MC_1_16_5
    @Accessor
    void setTransactionId(int transactionId);

    @Accessor
    void setIdentifier(ResourceLocation identifier);

    @Accessor
    void setData(FriendlyByteBuf data);
    #endif
}
