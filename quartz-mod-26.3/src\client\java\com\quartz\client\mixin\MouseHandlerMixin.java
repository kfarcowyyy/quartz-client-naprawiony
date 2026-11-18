package com.quartz.client.mixin;

import com.quartz.client.QuartzConfig;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void quartz$trackClick(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (action == 1) QuartzConfig.recordClick(button);
    }
}
