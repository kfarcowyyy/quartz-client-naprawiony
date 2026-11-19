package com.quartz.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public final class QuartzTitleScreen extends Screen {
    private int left, top, panelWidth;

    private QuartzTitleScreen() { super(Component.literal("Quartz Client")); }

    public static void replaceVanilla(Minecraft client) {
        if (client.isDemo() || !(client.screen instanceof TitleScreen original) || original instanceof Classic) return;
        client.setScreen(QuartzConfig.data.classicMenu ? new Classic() : new QuartzTitleScreen());
    }

    private static final class Classic extends TitleScreen {
        @Override
        protected void init() {
            super.init();
            addRenderableWidget(Button.builder(QuartzText.of("Switch to Quartz menu", "Wróć do menu Quartz"), button -> {
                QuartzConfig.data.classicMenu = false;
                QuartzConfig.save();
                minecraft.setScreen(new QuartzTitleScreen());
            }).bounds(6, 6, 126, 20).build());
        }
    }

    @Override
    protected void init() {
        panelWidth = Math.min(248, width - 28);
        int half = (panelWidth - 6) / 2;
        left = (width - panelWidth) / 2;
        top = Math.max(92, (height - 58) / 2);
        add(Component.translatable("menu.singleplayer"), left, top, panelWidth, true,
                () -> show(new SelectWorldScreen(this)));
        add(Component.translatable("menu.multiplayer"), left, top + 26, panelWidth, false,
                () -> show(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)))
                .active = minecraft.allowsMultiplayer();
        int gap = 5, tile = Math.min(89, (width - 36) / 4), nav = (width - 4 * tile - 3 * gap) / 2;
        add(QuartzText.of("Mods", "Mody"), nav, 34, tile, false, () -> show(new QuartzMenuScreen(this)));
        add(QuartzText.of("Wardrobe", "Garderoba"), nav + tile + gap, 34, tile, false, () -> show(new QuartzWardrobeScreen(this)));
        add(QuartzText.of("Packs & shaders", "Paczki i shadery"), nav + 2 * (tile + gap), 34, tile, false, () -> show(new QuartzPacksScreen(this)));
        add(QuartzText.of("Screenshots", "Zrzuty"), nav + 3 * (tile + gap), 34, tile, false, () -> show(new QuartzScreenshotScreen(this)));
        add(Component.translatable("menu.options"), left, top + 53, half, false, () -> show(new OptionsScreen(this, minecraft.options)));
        add(Component.translatable("menu.quit"), left + half + 6, top + 53, half, false, minecraft::stop);
        add(QuartzText.of("Classic menu", "Klasyczne menu"), 8, height - 28, 112, false, () -> {
            QuartzConfig.data.classicMenu = true;
            QuartzConfig.save();
            show(new Classic());
        });
    }

    private QuartzButton add(Component label, int x, int y, int w, boolean accent, Runnable action) {
        return addRenderableWidget(new QuartzButton(label, x, y, w, 21, accent, action));
    }

    private void show(Screen screen) { minecraft.setScreen(screen); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float delta) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        QuartzTheme.background(g, width, height);
        String account = QuartzText.of("Account: ", "Konto: ").getString() + minecraft.getUser().getName();
        int badgeWidth = Math.min(width - 16, font.width(account) + 18);
        String visibleAccount = font.plainSubstrByWidth(account, badgeWidth - 16);
        QuartzTheme.card(g, width - badgeWidth - 8, 8, badgeWidth, 19, QuartzTheme.WHITE, QuartzTheme.OUTLINE);
        g.drawString(font, visibleAccount, width - badgeWidth + 1, 14, QuartzTheme.INK, false);
        String brand = "Quartz Client";
        int brandX = (width - 25 - font.width(brand)) / 2, brandY = top - 36;
        QuartzTheme.crystal(g, brandX, brandY, 18, 27);
        g.drawString(font, brand, brandX + 25, brandY + 10, QuartzTheme.ACCENT, false);
        QuartzTheme.card(g, left - 10, top - 7, panelWidth + 20, 89, 0xFFFAF6FE, QuartzTheme.OUTLINE);
        super.render(g, mouseX, mouseY, delta);
    }
}
