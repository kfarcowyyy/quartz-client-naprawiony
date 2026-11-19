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
        if (w < 6 || h < 6) { g.fill(x, y, x + w, y + h, color); return; }
        g.fill(x + 3, y, x + w - 3, y + 1, color);
        g.fill(x + 1, y + 1, x + w - 1, y + 3, color);
        g.fill(x, y + 3, x + w, y + h - 3, color);
        g.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, color);
        g.fill(x + 3, y + h - 1, x + w - 3, y + h, color);
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
