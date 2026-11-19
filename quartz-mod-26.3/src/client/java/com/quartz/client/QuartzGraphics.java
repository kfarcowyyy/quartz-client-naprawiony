package com.quartz.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class QuartzGraphics {
    private QuartzGraphics() {}

    static void center(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
        graphics.text(font, text, x - font.width(text) / 2, y, color, true);
    }

    static void center(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        graphics.text(font, text, x - font.width(text) / 2, y, color, true);
    }
}
