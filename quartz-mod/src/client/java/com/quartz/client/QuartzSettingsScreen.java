package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

public final class QuartzSettingsScreen extends QuartzSafeScreen {
    private final Screen parent;
    public QuartzSettingsScreen(Screen parent) {
        super(QuartzText.of("Quartz settings", "Ustawienia Quartz"));
        this.parent = parent;
    }

    @Override protected void init() {
        int span = Math.min(220, width - 18), left = (width - span) / 2;
        addRenderableWidget(new QuartzButton(QuartzText.of("Language · English", "Język · Polski"), left, height / 2 - 9, span, 22, true, () -> {
            QuartzConfig.data.language = "pl".equals(QuartzConfig.data.language) ? "en" : "pl";
            QuartzConfig.save();
            minecraft.setScreen(new QuartzSettingsScreen(parent));
        }));
        addRenderableWidget(new QuartzButton(QuartzText.of("Back", "Wróć"), left, height / 2 + 20, span, 22, false, this::onClose));
    }

    @Override protected void drawQuartz(GuiGraphics g, int x, int y, float delta) {
        QuartzTheme.background(g, width, height);
        g.drawCenteredString(font, title, width / 2, height / 2 - 29, QuartzTheme.ACCENT);
        drawWidgets(g, x, y, delta);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
}
