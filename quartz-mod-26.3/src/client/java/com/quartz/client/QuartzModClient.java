package com.quartz.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class QuartzModClient implements ClientModInitializer {
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        QuartzConfig.load();
        QuartzWardrobe.refresh();
        QuartzMultiplayerWardrobe.initialize();
        QuartzShotPreview.start();
        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.quartz.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT,
                KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            QuartzTitleScreen.replaceVanilla(client);
            QuartzWardrobe.tick();
            if (client.getConnection() != null) QuartzMultiplayerWardrobe.tick();
            QuartzShotPreview.tick();
            while (openMenu.consumeClick()) {
                if (client.gui.screen() == null && client.player != null) client.gui.setScreen(new QuartzMenuScreen(null));
            }
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("quartz", "main_hud"), (graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (!client.gui.hud.isHidden()) QuartzShotPreview.popup(graphics);
            if (!QuartzConfig.data.hudEnabled || client.gui.hud.isHidden() || client.player == null) return;
            String text = QuartzConfig.data.hudText
                    .replace("{fps}", Integer.toString(client.getFps()))
                    .replace("{cps}", QuartzConfig.cps());
            int x = Math.max(0, Math.min(QuartzConfig.data.hudX, graphics.guiWidth() - 8));
            int y = Math.max(0, Math.min(QuartzConfig.data.hudY, graphics.guiHeight() - 16));
            int width = client.font.width(text) + 12;
            graphics.fill(x, y, x + width, y + 18, QuartzConfig.data.background);
            graphics.text(client.font, text, x + 6, y + 5, QuartzConfig.data.foreground, false);
        });
    }
}
