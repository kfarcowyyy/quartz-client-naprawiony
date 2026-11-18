package com.quartz.client.mixin;

import com.quartz.client.QuartzWardrobe;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class QuartzPlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void quartzApplyWardrobe(CallbackInfoReturnable<PlayerSkin> result) {
        if ((Object) this instanceof LocalPlayer) {
            result.setReturnValue(QuartzWardrobe.apply(result.getReturnValue()));
        }
    }
}
