package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

public final class QuartzSettingsScreen extends Screen {
    private final Screen parent;
    private EditBox template;
    private EditBox background;
    private EditBox foreground;

    public QuartzSettingsScreen(Screen parent) {
        super(QuartzText.of("HUD settings", "Ustawienia HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 150;
        template = new EditBox(font, left, height / 2 - 50, 300, 24,
                QuartzText.of("HUD text template", "Tekst HUD"));
        template.setMaxLength(72);
        template.setValue(QuartzConfig.data.hudText);
        addRenderableWidget(template);

        background = new EditBox(font, left, height / 2 - 12, 145, 24,
                QuartzText.of("Background AARRGGBB", "Tlo AARRGGBB"));
        foreground = new EditBox(font, left + 155, height / 2 - 12, 145, 24,
                QuartzText.of("Text AARRGGBB", "Tekst AARRGGBB"));
        background.setMaxLength(8);
        foreground.setMaxLength(8);
        background.setValue(String.format("%08X", QuartzConfig.data.background));
        foreground.setValue(String.format("%08X", QuartzConfig.data.foreground));
        addRenderableWidget(background);
        addRenderableWidget(foreground);

        addRenderableWidget(Button.builder(QuartzText.of("HUD: On / Off", "HUD: wlacz / wylacz"), button -> {
            QuartzConfig.data.hudEnabled = !QuartzConfig.data.hudEnabled;
            QuartzConfig.save();
        }).bounds(left, height / 2 + 20, 145, 24).build());
        addRenderableWidget(Button.builder(QuartzText.of("Language: English / Polish", "Jezyk: English / Polski"), button -> {
            QuartzConfig.data.language = "pl".equals(QuartzConfig.data.language) ? "en" : "pl";
            QuartzConfig.save();
            Minecraft.getInstance().setScreen(new QuartzSettingsScreen(parent));
        }).bounds(left + 155, height / 2 + 20, 145, 24).build());
        addRenderableWidget(Button.builder(QuartzText.of("Save", "Zapisz"), button -> saveAndClose())
                .bounds(left, height / 2 + 52, 145, 24).build());
        addRenderableWidget(Button.builder(QuartzText.of("Done", "Gotowe"), button -> onClose())
                .bounds(left + 155, height / 2 + 52, 145, 24).build());
    }

    private void saveAndClose() {
        String value = template.getValue().trim();
        QuartzConfig.data.hudText = value.isEmpty() ? "FPS {fps} | CPS {cps}" : value;
        try { QuartzConfig.data.background = (int) Long.parseLong(background.getValue(), 16); } catch (Exception ignored) { }
        try { QuartzConfig.data.foreground = (int) Long.parseLong(foreground.getValue(), 16); } catch (Exception ignored) { }
        QuartzConfig.save();
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 90, 0xFFFFFFFF);
        graphics.drawString(font,
                QuartzText.of("Placeholders: {fps} and {cps}; color format: AARRGGBB",
                        "Zmienne: {fps} i {cps}; kolor w formacie AARRGGBB"),
                width / 2 - 150, height / 2 - 72, 0xFFB8C7F2);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
