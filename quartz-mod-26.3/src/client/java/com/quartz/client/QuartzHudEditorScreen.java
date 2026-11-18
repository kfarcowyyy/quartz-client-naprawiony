package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class QuartzHudEditorScreen extends Screen {
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        String sample = QuartzConfig.data.hudText.replace("{fps}", "144").replace("{cps}", "8 | 3");
        int x = QuartzConfig.data.hudX, y = QuartzConfig.data.hudY;
        graphics.fill(x, y, x + font.width(sample) + 12, y + 18, QuartzConfig.data.background);
        graphics.text(font, sample, x + 6, y + 5, QuartzConfig.data.foreground, false);
        QuartzGraphics.center(graphics, font,
                QuartzText.of("Drag the HUD panel to move it", "Przeciagnij panel, aby go przesunac"),
                width / 2, 16, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int x = QuartzConfig.data.hudX, y = QuartzConfig.data.hudY;
        int w = font.width("FPS 144  |  CPS 8 | 3") + 12;
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 20) {
            dragging = true;
            dragX = (int) mouseX - x;
            dragY = (int) mouseY - y;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            QuartzConfig.data.hudX = Math.max(0, Math.min(width - 20, (int) event.x() - dragX));
            QuartzConfig.data.hudY = Math.max(0, Math.min(height - 30, (int) event.y() - dragY));
            QuartzConfig.save();
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
}
