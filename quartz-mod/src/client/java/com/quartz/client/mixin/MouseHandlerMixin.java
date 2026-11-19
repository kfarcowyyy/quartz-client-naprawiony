package com.quartz.client.mixin;

import com.quartz.client.QuartzConfig;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void quartz$trackClick(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (action == 1 && client.player != null && client.screen == null) QuartzConfig.recordClick(input.button());
    }
}
