package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class QuartzButton extends Button {
    private final boolean accent;
    private float highlight;
    private long lastFrame = System.nanoTime();

    QuartzButton(Component label, int x, int y, int w, int h, boolean accent, Runnable action) {
        super(x, y, w, h, label, button -> action.run(), DEFAULT_NARRATION);
        this.accent = accent;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float change = Math.min(1f, (now - lastFrame) / 120_000_000f);
        highlight += ((active && (isHovered() || isFocused()) ? 1f : 0f) - highlight) * change;
        lastFrame = now;
        int bg = !active ? 0xFFF5EFF8 : accent
                ? QuartzTheme.mix(0xFF8250B0, 0xFFAC66B3, highlight)
                : QuartzTheme.mix(QuartzTheme.WHITE, 0xFFF0DEFD, highlight);
        int edge = accent && active ? 0xFF704398 : QuartzTheme.OUTLINE;
        int fg = !active ? QuartzTheme.MUTED : accent ? 0xFFFFFFFF : QuartzTheme.INK;
        QuartzTheme.card(g, getX(), getY(), width, height, bg, edge);
        var font = Minecraft.getInstance().font;
        String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, width - 12));
        g.text(font, label, getX() + (width - font.width(label)) / 2,
                getY() + (height - 8) / 2, fg, false);
    }
}
