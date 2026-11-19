package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

final class QuartzButton extends Button {
    private final boolean accent;
    private float highlight;
    private long lastFrame = System.nanoTime();

    QuartzButton(Component label, int x, int y, int w, int h, boolean accent, Runnable action) {
        super(x, y, w, h, label, button -> invoke(button, action), DEFAULT_NARRATION);
        this.accent = accent;
    }

    private static void invoke(Button button, Runnable action) {
        Minecraft client = Minecraft.getInstance();
        var previous = client.gui.screen();
        try {
            action.run();
        } catch (RuntimeException error) {
            LoggerFactory.getLogger("Quartz Buttons").error("Quartz menu action failed: {}", button.getMessage().getString(), error);
            button.setMessage(QuartzText.of("See game log", "Sprawdź dziennik gry"));
            if (client.gui.screen() != previous) {
                try {
                    client.gui.setScreen(previous);
                } catch (RuntimeException restore) {
                    LoggerFactory.getLogger("Quartz Buttons").error("Could not restore the previous screen", restore);
                    client.gui.setScreen(null);
                }
            }
        }
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
