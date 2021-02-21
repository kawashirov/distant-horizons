package com.backsun.lod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.DamageSource;

@Mixin(ItemEntity.class)
public class MixinItemEntity
{
	@Inject(at = @At("HEAD"), method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z", cancellable = true)
	private void attackEntityFrom(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback)
	{
	    callback.setReturnValue(false);
	}

}
