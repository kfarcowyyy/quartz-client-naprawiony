package com.quartz.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class QuartzModClient implements ClientModInitializer {
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        QuartzConfig.load();
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.quartz.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) client.setScreen(new QuartzMenuScreen());
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("quartz", "main_hud"), (graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (!QuartzConfig.data.hudEnabled || client.options.hideGui || client.player == null) return;
            String text = QuartzConfig.data.hudText
                    .replace("{fps}", Integer.toString(client.getFps()))
                    .replace("{cps}", QuartzConfig.cps());
            int x = Math.max(0, Math.min(QuartzConfig.data.hudX, graphics.guiWidth() - 8));
            int y = Math.max(0, Math.min(QuartzConfig.data.hudY, graphics.guiHeight() - 16));
            int width = client.font.width(text) + 12;
            graphics.fill(x, y, x + width, y + 18, QuartzConfig.data.background);
            graphics.drawString(client.font, text, x + 6, y + 5, QuartzConfig.data.foreground, false);
        });
    }
}
