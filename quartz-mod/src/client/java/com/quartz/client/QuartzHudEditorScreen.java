package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public final class QuartzHudEditorScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private int dragX;
    private int dragY;

    public QuartzHudEditorScreen(Screen parent) {
        super(QuartzText.of("HUD layout", "Pozycja HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(QuartzText.of("Done", "Gotowe"), button -> onClose())
                .bounds(width / 2 - 60, height - 40, 120, 24).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        String sample = QuartzConfig.data.hudText.replace("{fps}", "144").replace("{cps}", "8 | 3");
        int x = QuartzConfig.data.hudX;
        int y = QuartzConfig.data.hudY;
        graphics.fill(x, y, x + font.width(sample) + 12, y + 18, QuartzConfig.data.background);
        graphics.drawString(font, sample, x + 6, y + 5, QuartzConfig.data.foreground, false);
        graphics.drawCenteredString(font,
                QuartzText.of("Drag the HUD panel to move it", "Przeciagnij panel, aby zmienic jego polozenie"),
                width / 2, 16, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = QuartzConfig.data.hudX;
        int y = QuartzConfig.data.hudY;
        int w = font.width("FPS 144  |  CPS 8 | 3") + 12;
        if (button == 0 && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 20) {
            dragging = true;
            dragX = (int) mouseX - x;
            dragY = (int) mouseY - y;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            QuartzConfig.data.hudX = Math.max(0, Math.min(width - 20, (int) mouseX - dragX));
            QuartzConfig.data.hudY = Math.max(0, Math.min(height - 30, (int) mouseY - dragY));
            QuartzConfig.save();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
