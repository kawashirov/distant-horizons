package com.seibel.distanthorizons.fabric.mixins.mods.imm_ptl_core;

// TODO: Fix this eventually
/*
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IImmersivePortalsAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IModChecker;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import com.seibel.distanthorizons.coreapi.util.math.Vec3f;
import com.seibel.distanthorizons.fabric.wrappers.modAccessor.ImmersivePortalsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import java.util.function.Consumer;

@Mixin(MyGameRenderer.class)
public class MixinImmersivePortalsGameRenderer {
    @Shadow public static Minecraft client;

    //    @Unique
//    static ImmersivePortalsAccessor accessor = null;
    // TODO: Find a way to inject just before line 190 in the immersive portals code
    // This current place that we are injecting into is not correct
    @Inject(remap = false,
            method = "switchAndRenderTheWorld",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
    private static void injectDHLoDRendering(ClientLevel newWorld, Vec3 thisTickCameraPos, Vec3 lastTickCameraPos, Consumer<Runnable> invokeWrapper, int renderDistance, boolean doRenderHand, CallbackInfo ci) {
        // TODO: Is there a better way to do this?
        if (SingletonInjector.INSTANCE.get(IModChecker.class).isModLoaded("sodium"))
            return;

        // TODO: Move this out of the function to not run it every frame
        ImmersivePortalsAccessor accessor = null;

        if (accessor == null) {
            accessor = (ImmersivePortalsAccessor) ModAccessorInjector.INSTANCE.get(IImmersivePortalsAccessor.class);
        }

//        System.out.println(McObjectConverter.Convert(RenderSystem.getProjectionMatrix()).toString());
        Mat4f modelViewMatrix = McObjectConverter.Convert(RenderSystem.getModelViewMatrix());
        Mat4f projectionMatrix = McObjectConverter.Convert(client.gameRenderer.getProjectionMatrix(client.options.fov));
        Vector3f vanillaLookVector = client.gameRenderer.getMainCamera().getLookVector();


//        client.getCameraEntity()

//        System.out.println(client.gameRenderer.getMainCamera().getLookVector());
        ClientApi.INSTANCE.renderLods(
                ClientLevelWrapper.getWrapper(client.level),
                modelViewMatrix,
                projectionMatrix,
                accessor.partialTicks
        );
    }
}
*/