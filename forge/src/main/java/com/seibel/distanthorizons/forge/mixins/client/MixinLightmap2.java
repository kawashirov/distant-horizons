package com.seibel.distanthorizons.forge.mixins.client;


import com.seibel.distanthorizons.common.util.LightTextureMarker;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class MixinLightmap2
{
	@Shadow
	@Final
	private DynamicTexture lightTexture;
	
	@Inject(method = "<init>", at = @At("RETURN"))
	public void markLightTexture(CallbackInfo ci)
	{
		((LightTextureMarker) lightTexture).markLightTexture();
	}
	
}
