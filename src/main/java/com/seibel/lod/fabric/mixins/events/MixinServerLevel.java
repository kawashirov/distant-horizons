package com.seibel.lod.fabric.mixins.events;

import com.seibel.lod.fabric.Main;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class is used for world saving events
 * @author Ran
 */
@Mixin(ServerLevel.class)
public class MixinServerLevel {
    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;saveAll()V", shift = At.Shift.AFTER))
    private void saveWorldEvent_sA(ProgressListener progressListener, boolean bl, boolean bl2, CallbackInfo ci) {
        Main.client_proxy.worldSaveEvent();
    }

    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;autoSave()V", shift = At.Shift.AFTER))
    private void saveWorldEvent_aS(ProgressListener progressListener, boolean bl, boolean bl2, CallbackInfo ci) {
        Main.client_proxy.worldSaveEvent();
    }
}
