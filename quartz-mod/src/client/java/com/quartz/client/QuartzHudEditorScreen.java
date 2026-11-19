package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class QuartzHudEditorScreen extends QuartzSafeScreen {
    private final Screen parent;
    private boolean dragging;
    private int dragX, dragY;

    public QuartzHudEditorScreen(Screen parent) {
        super(QuartzText.of("HUD layout", "Pozycja HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(QuartzText.of("Done", "Gotowe"), b -> onClose())
                .bounds(width / 2 - 60, height - 40, 120, 24).build());
    }

    @Override
    protected void drawQuartz(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, 0x3B1D1230, 0x9C1D1230);
        String sample = preview();
        int x = panelX(), y = panelY();
        graphics.fill(x, y, x + font.width(sample) + 12, y + 18, QuartzConfig.data.background);
        graphics.drawString(font, sample, x + 6, y + 5, QuartzConfig.data.foreground, false);
        graphics.drawCenteredString(font,
                QuartzText.of("Drag the HUD panel to move it", "Przeciagnij panel, aby go przesunac"),
                width / 2, Math.max(4, height - 52), 0xFFFFFFFF);
        drawWidgets(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int x = panelX(), y = panelY();
        int w = panelWidth();
        if (event.button() == 0 && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY < y + 18) {
            dragging = true;
            dragX = (int) mouseX - x;
            dragY = (int) mouseY - y;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == 0) {
            QuartzConfig.data.hudX = clamp((int) event.x() - dragX, width - panelWidth());
            QuartzConfig.data.hudY = clamp((int) event.y() - dragY, height - 62);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging && event.button() == 0) {
            dragging = false;
            QuartzConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    private String preview() {
        String text = QuartzConfig.hudTemplate().replace("{fps}", "144").replace("{cps}", "8 | 3");
        return font.plainSubstrByWidth(text, Math.max(0, width - 12));
    }

    private int panelWidth() { return font.width(preview()) + 12; }
    private int panelX() { return clamp(QuartzConfig.data.hudX, width - panelWidth()); }
    private int panelY() { return clamp(QuartzConfig.data.hudY, height - 62); }

    private static int clamp(int value, int max) { return Math.max(0, Math.min(max, value)); }

    @Override
    public void removed() {
        if (dragging) {
            dragging = false;
            QuartzConfig.save();
        }
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
