package com.quartz.client.mixin;

import com.quartz.client.QuartzConfig;
import com.mojang.blaze3d.platform.InputConstants;
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
        if (action == 1 && client.player != null && client.gui.screen() == null) {
            if (input.button() == InputConstants.MOUSE_BUTTON_LEFT) QuartzConfig.recordClick(0);
            if (input.button() == InputConstants.MOUSE_BUTTON_RIGHT) QuartzConfig.recordClick(1);
        }
    }
}
