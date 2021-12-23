package com.seibel.lod.fabric.mixins.events;

import com.seibel.lod.fabric.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * If someone has a better way to do this then please let me know.
 *
 * @author Ran
 */
@Mixin(ClientboundBlockUpdatePacket.class)
public abstract class MixinBlockUpdate {
    @Shadow public abstract BlockPos getPos();

    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V", at = @At("TAIL"))
    private void onBlockUpdate(ClientGamePacketListener clientGamePacketListener, CallbackInfo ci) {
        Main.client_proxy.blockChangeEvent(Minecraft.getInstance().player.clientLevel, this.getPos());
    }
}
