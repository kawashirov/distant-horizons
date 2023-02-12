package com.seibel.lod.fabric.mixins.mods.sodium;

import com.seibel.lod.core.api.internal.ClientApi;
import com.seibel.lod.core.dependencyInjection.ModAccessorInjector;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.lod.fabric.wrappers.modAccessor.SodiumAccessor;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegionChunkRenderer.class)
public class MixinSodiumChunkRenderer {
    @Unique SodiumAccessor accessor = null;
    @Inject(remap = false, method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/ShaderChunkRenderer;begin(Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPass;)V", shift = At.Shift.AFTER))
    private void injectDHLoDRendering(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderList list, BlockRenderPass pass, ChunkCameraContext camera, CallbackInfo ci) {
        if (accessor == null) {
            accessor = (SodiumAccessor)ModAccessorInjector.INSTANCE.get(ISodiumAccessor.class);
        }
        if (pass.equals(BlockRenderPass.SOLID)) {
            //TODO: use matrices.modelView() and matrices.projection() instead of
            // SodiumAccessor.mcModelViewMatrix,
            // SodiumAccessor.mcProjectionMatrix,
            ClientApi.INSTANCE.renderLods(accessor.levelWrapper,
                    accessor.mcModelViewMatrix,
                    accessor.mcProjectionMatrix,
                    accessor.partialTicks);
        }
    }
}
