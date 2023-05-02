package com.seibel.lod.fabric.mixins.mods.imm_ptl_core;

import com.mojang.blaze3d.systems.RenderSystem;
import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.world.ClientLevelWrapper;
import com.seibel.lod.core.api.internal.ClientApi;
import com.seibel.lod.core.dependencyInjection.ModAccessorInjector;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IImmersivePortalsAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.ImmersivePortalsAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import java.util.function.Consumer;

@Mixin(MyGameRenderer.class)
public class MixinImmersivePortalsGameRenderer {
//    @Unique
//    static ImmersivePortalsAccessor accessor = null;
    @Inject(remap = false, method = "renderWorldNew", at = @At(value = "INVOKE", target = "Lqouteall/imm_ptl/core/render/MyGameRenderer;switchAndRenderTheWorld(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Consumer;IZ)V", shift = At.Shift.AFTER))
    private static void injectDHLoDRendering(WorldRenderInfo worldRenderInfo, Consumer<Runnable> invokeWrapper, CallbackInfo ci) {
        // TODO: Move this out of the function to not run it every frame
        ImmersivePortalsAccessor accessor = null;

        if (accessor == null) {
            accessor = (ImmersivePortalsAccessor) ModAccessorInjector.INSTANCE.get(IImmersivePortalsAccessor.class);
        }
        ClientApi.INSTANCE.renderLods(
                ClientLevelWrapper.getWrapper(worldRenderInfo.world),
                McObjectConverter.Convert(RenderSystem.getProjectionMatrix()),
                McObjectConverter.Convert(RenderSystem.getProjectionMatrix()),
                accessor.partialTicks
        );
    }
}
