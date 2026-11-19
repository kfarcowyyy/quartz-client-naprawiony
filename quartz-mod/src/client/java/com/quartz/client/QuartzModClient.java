package com.quartz.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

public final class QuartzModClient implements ClientModInitializer {
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        QuartzConfig.load();
        QuartzShotPreview.start();
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.quartz.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            QuartzTitleScreen.replaceVanilla(client);
            QuartzWardrobe.tick();
            QuartzHud.tick(client);
            QuartzShotPreview.tick();
            while (openMenu.consumeClick()) {
                if (client.screen == null && client.player != null) {
                    try {
                        client.setScreen(new QuartzMenuScreen(null));
                    } catch (RuntimeException error) {
                        LoggerFactory.getLogger("Quartz Client").error("Could not open the Quartz mods screen", error);
                        client.setScreen(null);
                        client.player.displayClientMessage(QuartzText.of("Could not open Quartz menu. See the game log.",
                                "Nie udało się otworzyć menu Quartz. Sprawdź dziennik gry."), false);
                    }
                }
            }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("quartz", "in_game_hud"), (graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && !(client.screen instanceof QuartzHudEditorScreen)) QuartzHud.inGame(graphics, client);
            if (!client.options.hideGui) QuartzShotPreview.popup(graphics);
        });
    }
}
