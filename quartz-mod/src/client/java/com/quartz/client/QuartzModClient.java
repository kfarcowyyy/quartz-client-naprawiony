package com.quartz.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class QuartzModClient implements ClientModInitializer {
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        QuartzConfig.load();
        QuartzWardrobe.refresh();
        QuartzMultiplayerWardrobe.initialize();
        QuartzShotPreview.start();
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.quartz.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            QuartzWardrobe.tick();
            if (client.getConnection() != null) QuartzMultiplayerWardrobe.tick();
            QuartzShotPreview.tick();
            while (openMenu.consumeClick()) {
                if (client.screen == null && client.player != null) client.setScreen(new QuartzMenuScreen(null));
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof TitleScreen)) return;
            Screens.getButtons(screen).add(Button.builder(Component.literal("Quartz Client"),
                    button -> client.setScreen(new QuartzMenuScreen(screen))).bounds(8, 8, 98, 20).build());
            ScreenEvents.afterRender(screen).register((current, graphics, x, y, delta) -> {
                String caption = "High Performance Minecraft Client";
                graphics.drawString(client.font, caption, width - client.font.width(caption) - 8, 12, 0xFFF3EDFF, true);
            });
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("quartz", "main_hud"), (graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (!client.options.hideGui) QuartzShotPreview.popup(graphics);
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
