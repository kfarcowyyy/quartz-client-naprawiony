package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public final class QuartzMenuScreen extends Screen {
    public QuartzMenuScreen() { super(QuartzText.of("Quartz Client", "Quartz Client")); }

    @Override
    protected void init() {
        int left = width / 2 - 110;
        int top = height / 2 - 86;
        addRenderableWidget(Button.builder(QuartzText.of("Mod list", "Lista modow"), b ->
                Minecraft.getInstance().setScreen(new QuartzModListScreen(this)))
                .bounds(left, top, 220, 28).build());
        addRenderableWidget(Button.builder(QuartzText.of("HUD layout", "Pozycja HUD"), b ->
                Minecraft.getInstance().setScreen(new QuartzHudEditorScreen(this)))
                .bounds(left, top + 36, 220, 28).build());
        addRenderableWidget(Button.builder(QuartzText.of("HUD settings", "Ustawienia HUD"), b ->
                Minecraft.getInstance().setScreen(new QuartzSettingsScreen(this)))
                .bounds(left, top + 72, 220, 28).build());
        addRenderableWidget(Button.builder(QuartzText.of("Screenshots", "Zrzuty ekranu"), b ->
                Minecraft.getInstance().setScreen(new QuartzScreenshotScreen(this)))
                .bounds(left, top + 108, 220, 28).build());
        addRenderableWidget(Button.builder(QuartzText.of("Resource packs folder", "Folder paczek zasobow"), b ->
                QuartzFiles.openFolder("resourcepacks"))
                .bounds(left, top + 144, 220, 24).build());
        addRenderableWidget(Button.builder(QuartzText.of("Shader packs folder", "Folder shaderow"), b ->
                QuartzFiles.openFolder("shaderpacks"))
                .bounds(left, top + 174, 220, 24).build());
        addRenderableWidget(Button.builder(QuartzText.of("Done", "Gotowe"), b -> onClose())
                .bounds(left, top + 204, 220, 24).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 120, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                QuartzText.of("Right Shift opens this menu. Change the key in Controls.",
                        "Right Shift otwiera menu. Zmien klawisz w Sterowaniu."),
                width / 2, height - 18, 0xFFB8C7F2);
        super.render(graphics, mouseX, mouseY, delta);
    }
}
