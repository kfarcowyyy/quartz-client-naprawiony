package com.quartz.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

final class QuartzTheme {
    static final int INK = 0xFF382648;
    static final int MUTED = 0xFF705D80;
    static final int ACCENT = 0xFF8145B5;
    static final int OUTLINE = 0xFFDFCCEB;
    static final int WHITE = 0xFFFFFCFF;

    private QuartzTheme() {}

    static void background(GuiGraphicsExtractor g, int width, int height) {
        g.fillGradient(0, 0, width, height, 0xFFFDF6FE, 0xFFEFE2FC);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00F5D9F4, 0x99F5D9F4);
    }

    static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        int radius = Math.min(7, Math.min(w, h) / 2);
        g.fill(x, y + radius, x + w, y + h - radius, color);
        for (int row = 0; row < radius; row++) {
            double dy = radius - row - 0.5;
            int inset = (int) Math.round(radius - Math.sqrt(radius * radius - dy * dy));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - 1 - row, x + w - inset, y + h - row, color);
        }
    }

    static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int background, int line) {
        rounded(g, x, y + 2, w, h, 0x1F663F83);
        rounded(g, x, y, w, h, line);
        rounded(g, x + 1, y + 1, w - 2, h - 2, background);
    }

    static int mix(int from, int to, float amount) {
        int value = 0xFF000000;
        for (int i = 0; i <= 16; i += 8) {
            int a = from >> i & 255, b = to >> i & 255;
            value |= Math.round(a + (b - a) * amount) << i;
        }
        return value;
    }
}
